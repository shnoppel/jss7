/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.protocols.ss7.sccp.impl.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mobicents.protocols.ss7.sccp.impl.parameter.AbstractParameter;
import org.mobicents.protocols.ss7.sccp.impl.parameter.HopCounterImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ImportanceImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ReturnCauseImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressCodec;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SegmentationImpl;
import org.mobicents.protocols.ss7.sccp.message.UnitDataService;
import org.mobicents.protocols.ss7.sccp.parameter.HopCounter;
import org.mobicents.protocols.ss7.sccp.parameter.Importance;
import org.mobicents.protocols.ss7.sccp.parameter.ReturnCause;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.sccp.parameter.Segmentation;

/**
 * See Q.713 4.18
 * 
 * @author Oleg Kulikov
 * @author baranowb
 */
public class UnitDataServiceImpl extends SccpMessageImpl implements UnitDataService {


      
    // //////////////////
    // Fixed parts //
    // //////////////////
    /**
     * See Q.713 3.18
     */
    //    private byte hopCounter = HOP_COUNT_NOT_SET;

    private byte[] data;
    private ReturnCause returnCause;
    private SccpAddressCodec addressCodec = new SccpAddressCodec();
    

    protected UnitDataServiceImpl() {
        super(MESSAGE_TYPE);
    }
    
    protected UnitDataServiceImpl(ReturnCause returnCause, SccpAddress calledParty, SccpAddress callingParty) {
        super(MESSAGE_TYPE);
      
        this.returnCause =  returnCause;
        this.calledParty = calledParty;
        this.callingParty =  callingParty;
    }


    

	public ReturnCause getReturnCause() {
		return this.returnCause;
	}

	public void setReturnCause(ReturnCause rc) {
		this.returnCause  = (ReturnCauseImpl) rc;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void encode(OutputStream out) throws IOException {
        out.write(this.getType());

        out.write(((AbstractParameter)this.returnCause).encode());
        

        byte[] cdp = addressCodec.encode(calledParty);
        byte[] cnp = addressCodec.encode(callingParty);

        int len = 3;
        out.write(len);

        len = (cdp.length + 3);
        out.write(len);

        len += (cnp.length);
        out.write(len);

        out.write((byte) cdp.length);
        out.write(cdp);

        out.write((byte) cnp.length);
        out.write(cnp);

        out.write((byte) data.length);
        out.write(data);

    }

    
    public void decode(InputStream in) throws IOException {

    	this.returnCause = new ReturnCauseImpl();
    	((AbstractParameter)this.returnCause).decode(new byte[]{(byte) in.read()});
    	int cpaPointer = in.read() & 0xff;
        in.mark(in.available());

        in.skip(cpaPointer - 1);
        int len = in.read() & 0xff;

        byte[] buffer = new byte[len];
        in.read(buffer);

        calledParty = addressCodec.decode(buffer);

        in.reset();
        cpaPointer = in.read() & 0xff;
        in.mark(in.available());

        in.skip(cpaPointer - 1);
        len = in.read() & 0xff;

        buffer = new byte[len];
        in.read(buffer);

        callingParty = addressCodec.decode(buffer);

        in.reset();
        cpaPointer = in.read() & 0xff;

        in.skip(cpaPointer - 1);
        len = in.read() & 0xff;

        data = new byte[len];
        in.read(data);

    }

  

    
    public String toString() {
        return "UDTS[calledPartyAddress=" + calledParty + ", callingPartyAddress=" + callingParty + ", returnCause="+ returnCause +" ]";
    }
}




