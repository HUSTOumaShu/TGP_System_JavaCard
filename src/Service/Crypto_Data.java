/*
	Applet receive data and copy to buf_temp
	- Encrypt/Decrypt data and send to client
*/

package Service;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class Crypto_Data extends Applet
{

	private static final byte INS_ENCRIPT = (byte)0x00;
	private static final byte INS_DECRIPT = (byte)0x01;
	private static final byte INS_SEND = (byte)0x02;
	private byte[] enc_buffer, dec_buffer, keyData;
	
	private AESKey aesKey;
	private Cipher cipher;
	private short keyLen;
	
	private Crypto_Data() {
		keyLen = (short)(KeyBuilder.LENGTH_AES_128/8);
		enc_buffer = new byte[keyLen];
		dec_buffer = new byte[keyLen];
		keyData = new byte[keyLen];
		
		for(byte i = 0; i< keyLen; i++) {
			keyData[i] = (byte)i;
		}
		
		// initialized cipher and key
		cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
		aesKey = (AESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_AES, (short)(8*keyLen), false);
		aesKey.setKey(keyData, (short)0);
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Crypto_Data().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			return;
		}

		byte[] buf = apdu.getBuffer();
		short dataLen = (short)(buf[ISO7816.OFFSET_LC]&0x00FF);
		short readCount = apdu.setIncomingAndReceive();
		byte[] buf_temp = JCSystem.makeTransientByteArray(keyLen, JCSystem.CLEAR_ON_RESET);

		while(dataLen > 0) {
			dataLen -= readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}
		
		for(short i = 0; i< buf_temp.length; i++) {
			short j = (short)(i+5);
			buf_temp[i] = (byte)(buf[j]);
		}
		
		switch (buf[ISO7816.OFFSET_INS])
		{
		case INS_ENCRIPT:
			cipher.init(aesKey, Cipher.MODE_ENCRYPT);
			cipher.doFinal(buf_temp, (short)0, keyLen, enc_buffer, (short)0);
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)enc_buffer.length);
			apdu.sendBytesLong(enc_buffer, (short)0, (short)enc_buffer.length);
			break;
		case INS_DECRIPT:
			cipher.init(aesKey, Cipher.MODE_DECRYPT);
			cipher.doFinal(buf_temp, (short)0, keyLen, dec_buffer, (short)0);
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)dec_buffer.length);
			apdu.sendBytesLong(dec_buffer, (short)0, (short)dec_buffer.length);
			break;
		case INS_SEND:
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)buf_temp.length);
			apdu.sendBytesLong(buf_temp, (short)0, (short)buf_temp.length);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	private void encypt(APDU apdu, byte[] buf_temp) {
		
	}
	
	private void decrypt(APDU apdu, byte[] buf_temp) {
		
	}

}
