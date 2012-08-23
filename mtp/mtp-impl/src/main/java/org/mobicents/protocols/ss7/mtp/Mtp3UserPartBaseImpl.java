/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.mtp;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class Mtp3UserPartBaseImpl implements Mtp3UserPart {

	private static final Logger logger = Logger.getLogger(Mtp3UserPartBaseImpl.class);

	private int maxSls = 32;
	private int slsFilter = 0x1F;

	// The count of threads that will be used for message delivering to
	// Mtp3UserPartListener's
	// For single thread model this value should be equal 1
	// TODO: make it configurable
	protected int deliveryTransferMessageThreadCount = 1;

	protected boolean isStarted = false;

	private CopyOnWriteArrayList<Mtp3UserPartListener> userListeners = new CopyOnWriteArrayList<Mtp3UserPartListener>();
	// a thread pool for delivering Mtp3TransferMessage messages
	private ExecutorService[] msgDeliveryExecutors;
	// a thread for delivering PAUSE, RESUME and STATUS messages
	private ExecutorService msgDeliveryExecutorSystem;
	private int slsTable[] = new int[maxSls];

	private PointCodeFormat pointCodeFormat = PointCodeFormat.ITU;

	private int slsLength = 5;
	private Mtp3TransferPrimitiveFactory mtp3TransferPrimitiveFactory = null;

	public Mtp3UserPartBaseImpl() {
	}

	public int getDeliveryMessageThreadCount() {
		return this.deliveryTransferMessageThreadCount;
	}

	public void setDeliveryMessageThreadCount(int deliveryMessageThreadCount) {
		if (deliveryMessageThreadCount > 0)
			this.deliveryTransferMessageThreadCount = deliveryMessageThreadCount;
	}

	@Override
	public void addMtp3UserPartListener(Mtp3UserPartListener listener) {
		this.userListeners.add(listener);
	}

	@Override
	public void removeMtp3UserPartListener(Mtp3UserPartListener listener) {
		this.userListeners.remove(listener);
	}

	/*
	 * For classic MTP3 this value is maximum SIF length minus routing label
	 * length. This method should be overloaded if different message length is
	 * supported.
	 */
	@Override
	public int getMaxUserDataLength(int dpc) {
		switch (this.pointCodeFormat) {
		case ITU:
			// For PC_FORMAT_14, the MTP3 Routing Label takes 4 bytes - OPC/DPC
			// = 16 bits each and SLS = 4 bits
			return 272 - 4;
		case ANSI:
			// For PC_FORMAT_24, the MTP3 Routing Label takes 6 bytes - OPC/DPC
			// = 24 bits each and SLS = 8 bits
			return 272 - 7;
		default:
			// TODO : We don't support rest just yet
			return -1;

		}
	}

	public PointCodeFormat getPointCodeFormat() {
		return this.pointCodeFormat;
	}

	public void setPointCodeFormat(PointCodeFormat pointCodeFormat) {
		this.pointCodeFormat = pointCodeFormat;
	}

	public int getSlsLength() {
		return this.slsLength;
	}

	public void setSlsLength(int slsLength) {
		this.slsLength = slsLength;
	}

	public Mtp3TransferPrimitiveFactory getMtp3TransferPrimitiveFactory() {
		return this.mtp3TransferPrimitiveFactory;
	}

	public void start() throws Exception {

		if (this.isStarted)
			return;

		if (!(this.pointCodeFormat == PointCodeFormat.ITU || this.pointCodeFormat == PointCodeFormat.ANSI)) {
			throw new Exception("Invalid PointCodeFormat set. We support only ITU or ANSI now");
		}

		switch (this.slsLength) {
		case 4:
			this.maxSls = 16;
			this.slsFilter = 0x0f;
			break;
		case 5:
			this.maxSls = 32;
			this.slsFilter = 0x1f;
			break;
		case 8:
			this.maxSls = 256;
			this.slsFilter = 0xff;
			break;
		default:
			throw new Exception("Invalid SLS length set. Set 4 for max SLS to be 16 or 5 for max SLS to be 32 or 8 for max SLS to be 256");
		}

		this.mtp3TransferPrimitiveFactory = new Mtp3TransferPrimitiveFactory(this.pointCodeFormat, this.slsLength);

		this.createSLSTable(this.deliveryTransferMessageThreadCount);

		this.msgDeliveryExecutors = new ExecutorService[this.deliveryTransferMessageThreadCount];
		for (int i = 0; i < this.deliveryTransferMessageThreadCount; i++) {
			this.msgDeliveryExecutors[i] = Executors.newFixedThreadPool(1);
		}
		this.msgDeliveryExecutorSystem = Executors.newFixedThreadPool(1);

		this.isStarted = true;
	}

	public void stop() throws Exception {

		if (!this.isStarted)
			return;

		this.isStarted = false;

		for (ExecutorService es : this.msgDeliveryExecutors) {
			es.shutdown();
		}
		this.msgDeliveryExecutorSystem.shutdown();
	}

	/**
	 * Deliver an incoming message to the local user
	 * 
	 * @param msg
	 * @param effectiveSls
	 *            For the thread selection (for message delivering)
	 */
	protected void sendTransferMessageToLocalUser(Mtp3TransferPrimitive msg, int seqControl) {
		if (this.isStarted) {
			MsgTransferDeliveryHandler hdl = new MsgTransferDeliveryHandler(msg);

			seqControl = seqControl & slsFilter;
			this.msgDeliveryExecutors[this.slsTable[seqControl]].execute(hdl);
		}
	}

	protected void sendPauseMessageToLocalUser(Mtp3PausePrimitive msg) {
		if (this.isStarted) {
			MsgSystemDeliveryHandler hdl = new MsgSystemDeliveryHandler(msg, null, null);
			this.msgDeliveryExecutorSystem.execute(hdl);
		}
	}

	protected void sendResumeMessageToLocalUser(Mtp3ResumePrimitive msg) {
		if (this.isStarted) {
			MsgSystemDeliveryHandler hdl = new MsgSystemDeliveryHandler(null, msg, null);
			this.msgDeliveryExecutorSystem.execute(hdl);
		}
	}

	protected void sendStatusMessageToLocalUser(Mtp3StatusPrimitive msg) {
		if (this.isStarted) {
			MsgSystemDeliveryHandler hdl = new MsgSystemDeliveryHandler(null, null, msg);
			this.msgDeliveryExecutorSystem.execute(hdl);
		}
	}

	private void createSLSTable(int minimumBoundThread) {
		int stream = 0;
		for (int i = 0; i < maxSls; i++) {
			if (stream >= minimumBoundThread) {
				stream = 0;
			}
			slsTable[i] = stream++;
		}
	}

	private class MsgTransferDeliveryHandler implements Runnable {

		private Mtp3TransferPrimitive msg;

		public MsgTransferDeliveryHandler(Mtp3TransferPrimitive msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			if (isStarted) {
				try {
					for (Mtp3UserPartListener lsn : userListeners) {
						lsn.onMtp3TransferMessage(this.msg);
					}
				} catch (Exception e) {
					logger.error("Exception while delivering a system messages to the MTP3-user: " + e.getMessage(), e);
				}
			}
		}
	}

	private class MsgSystemDeliveryHandler implements Runnable {

		private Mtp3PausePrimitive pauseMsg;
		private Mtp3ResumePrimitive resumeMsg;
		private Mtp3StatusPrimitive statusMsg;

		public MsgSystemDeliveryHandler(Mtp3PausePrimitive pauseMsg, Mtp3ResumePrimitive resumeMsg, Mtp3StatusPrimitive statusMsg) {
			this.pauseMsg = pauseMsg;
			this.resumeMsg = resumeMsg;
			this.statusMsg = statusMsg;
		}

		@Override
		public void run() {
			if (isStarted) {
				try {
					for (Mtp3UserPartListener lsn : userListeners) {
						if (this.pauseMsg != null)
							lsn.onMtp3PauseMessage(this.pauseMsg);
						if (this.resumeMsg != null)
							lsn.onMtp3ResumeMessage(this.resumeMsg);
						if (this.statusMsg != null)
							lsn.onMtp3StatusMessage(this.statusMsg);
					}
				} catch (Exception e) {
					logger.error("Exception while delivering a payload messages to the MTP3-user: " + e.getMessage(), e);
				}
			}
		}
	}
}
