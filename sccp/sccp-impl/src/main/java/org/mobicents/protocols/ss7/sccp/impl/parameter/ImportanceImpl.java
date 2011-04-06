/**
 * 
 */
package org.mobicents.protocols.ss7.sccp.impl.parameter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mobicents.protocols.ss7.sccp.parameter.Importance;


/**
 * @author baranowb
 * 
 */
public class ImportanceImpl extends AbstractParameter implements Importance {

	
	// default is lowest priority :)
	private byte importance = 0;

	/**
	 * 
	 */
	public ImportanceImpl() {
		// TODO Auto-generated constructor stub
	}

	public ImportanceImpl(byte importance) {
		super();
		this.importance = (byte) (importance & 0x07);
	}

	public int getValue() {
		return importance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.protocols.ss7.sccp.OptionalParameter#decode(byte[])
	 */
	
	public void decode(byte[] buffer) throws IOException {
		this.importance = (byte) (buffer[0] & 0x07);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.protocols.ss7.sccp.OptionalParameter#encode()
	 */
	
	public byte[] encode() throws IOException {
		// TODO Auto-generated method stub
		return new byte[] { (byte) (importance & 0x07) };
	}


	/* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.sccp.impl.parameter.AbstractParameter#decode(java.io.InputStream)
	 */
	
	public void decode(InputStream in) throws IOException {
		if(in.read()!= 1)
		{
			throw new IOException();
		}
		
		this.importance = (byte) (in.read()& 0x07);
	}

	/* (non-Javadoc)
	 * @see org.mobicents.protocols.ss7.sccp.impl.parameter.AbstractParameter#encode(java.io.OutputStream)
	 */
	
	public void encode(OutputStream os) throws IOException {
		os.write(1);
		os.write(this.importance & 0x07);
	}
	
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + importance;
		return result;
	}

	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImportanceImpl other = (ImportanceImpl) obj;
		if (importance != other.importance)
			return false;
		return true;
	}


	
}
