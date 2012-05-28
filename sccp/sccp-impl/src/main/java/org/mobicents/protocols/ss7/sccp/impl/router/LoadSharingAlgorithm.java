/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
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

package org.mobicents.protocols.ss7.sccp.impl.router;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public enum LoadSharingAlgorithm {
	Undefined("Undefined"), Bit3("Bit3"), Bit4("Bit4");

	private static final String UNDEFINED = "Undefined";
	private static final String BIT3 = "Bit3";
	private static final String BIT4 = "Bit4";

	private final String algo;

	private LoadSharingAlgorithm(String type) {
		this.algo = type;
	}

	public static LoadSharingAlgorithm getInstance(String type) {
		if (UNDEFINED.equalsIgnoreCase(type)) {
			return Undefined;
		} else if (BIT3.equalsIgnoreCase(type)) {
			return Bit3;
		} else if (BIT4.equalsIgnoreCase(type)) {
			return Bit4;
		}

		return null;
	}

	public String getAlgo() {
		return this.algo;
	}
}