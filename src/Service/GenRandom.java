/*
	Applet receive data and set seed from data
	Send a random data initialized by seed and send to client
*/

package Service;

import javacard.framework.*;
import javacard.security.*;

public class GenRandom extends Applet
{
	
	private RandomData ranData;
	private byte[] seed, ranKey;

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new GenRandom().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			return;
		}

		byte[] buf = apdu.getBuffer();
		short dataLen = (short)(buf[ISO7816.OFFSET_LC] &0x00FF);
		short readCount = apdu.setIncomingAndReceive();
		
		byte[] buf_temp = JCSystem.makeTransientByteArray(dataLen, JCSystem.CLEAR_ON_RESET);
		
		while(dataLen > 0) {
			dataLen -= readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}
		for(short i = 0; i< buf_temp.length; i++) {
			short j = (short)(i+5);
			buf_temp[i] = (byte)buf[j];
		}
		
		switch (buf[ISO7816.OFFSET_INS])
		{
		case (byte)0x00:
			seed = new byte[(short)buf_temp.length];
			Util.arrayCopy(buf_temp, (short)0, seed, (short)0, (short)buf_temp.length);
			ranData = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
			ranData.setSeed(seed,(short)0, (short)seed.length);
			short ranLen = 10; // set length of random data
			
			ranKey = new byte[ranLen];
			ranData.generateData(ranKey, (short)0, ranLen);
			apdu.setOutgoing();
			apdu.setOutgoingLength(ranLen);
			apdu.sendBytesLong(ranKey, (short)0, ranLen);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

}
