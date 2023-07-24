/*
	Applet receive data and copy to buf_temp
	Send buf_temp to client
*/

package Service;

import javacard.framework.*;

public class Send_Data extends Applet
{
	
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Send_Data().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			return;
		}
		
		// Get data
		// buf - data received from client include:
		// - header (4 bytes): CLA - INS - P1 - P2
		// - data: Lc(number of bytes) - data (bytes)
		byte[] buf = apdu.getBuffer(); 
	
		short dataLen = (short)(buf[ISO7816.OFFSET_LC]&0x00FF); // get the number of bytes of data
		short readCount = apdu.setIncomingAndReceive();
		
		// copy data of buf to buf_temp and stored buf_temp on Applet
		byte[] buf_temp = JCSystem.makeTransientByteArray((short)dataLen, JCSystem.CLEAR_ON_RESET);

		while ( dataLen > 0){
			dataLen -= readCount; 
			readCount = apdu.receiveBytes (ISO7816.OFFSET_CDATA ); 
		} 
		
		for(short i = 0; i < buf_temp.length; i++) {
			short j = (short)(i+5);
			buf_temp[i] = (byte)(buf[j]);
		}
		
		switch (buf[ISO7816.OFFSET_INS])
		{
		case (byte)0x00:
			short len = (short)buf_temp.length;
			apdu.setOutgoing();
			apdu.setOutgoingLength(len);
			apdu.sendBytesLong(buf_temp, (short)0, len);
			
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

}
