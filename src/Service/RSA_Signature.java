/*
	Applet received data from client and copy to buf_temp
	- sign: sign data from salt, buf_temp, copy to sig_buffer and send both buf_temp + sig_buffer to client
	- verify: verify data with buf_temp and sig_buffer after sign, send result to client
*/

package Service;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class RSA_Signature extends Applet
{
	private static final byte INS_SIGN = (byte)0x00;
	private static final byte INS_VERIFY = (byte)0x01;
	
	private RSAPrivateKey rsaPrivate;
	private RSAPublicKey rsaPublic;
	private Signature rsaSig;
	
	private byte[] buf_temp, salt, sig_buffer;
	private short sigLen;
	
	private RSA_Signature() {
		salt = new byte[] {0x20, 0x20, 0x47, 0x57};
		
		sigLen = (short)(KeyBuilder.LENGTH_RSA_1024/8);
		sig_buffer = new byte[sigLen];
		
		rsaSig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
		rsaPrivate = (RSAPrivateKey)(KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, (short)(8*sigLen), false));
		rsaPublic = (RSAPublicKey)(KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, (short)(8*sigLen), false));
		
		KeyPair keyPair = new KeyPair(KeyPair.ALG_RSA, (short)(8*sigLen));
		keyPair.genKeyPair();
		rsaPrivate = (RSAPrivateKey)keyPair.getPrivate();
		rsaPublic = (RSAPublicKey)keyPair.getPublic();
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new RSA_Signature().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
		
		buf_temp = JCSystem.makeTransientByteArray(dataLen, JCSystem.CLEAR_ON_RESET);
		
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
		case INS_SIGN:
			rsaSig.init(rsaPrivate, Signature.MODE_SIGN);
			rsaSig.update(salt, (short)0, (short)salt.length);
			rsaSig.sign(buf_temp, (short)0, (short)buf_temp.length, sig_buffer, (short)0);
			
			apdu.setOutgoing();
			short len = (short)(buf_temp.length + sig_buffer.length);
			apdu.setOutgoingLength(len);
			apdu.sendBytesLong(buf_temp, (short)0, (short)buf_temp.length);
			apdu.sendBytesLong(sig_buffer, (short)0, (short)sig_buffer.length);
			break;
		case INS_VERIFY:
			rsaSig.init(rsaPublic, Signature.MODE_VERIFY);
			rsaSig.update(salt, (short)0, (short)salt.length);
			
			// in this example, get sig_buffer from function INS_SIGN because sig_buffer
			// in the system, client send plain text with sig_buffer to verify Signature
			boolean result = rsaSig.verify(buf_temp, (short)0, (short)buf_temp.length, sig_buffer, (short)0, sigLen);
			buf[(short)0] = result? (byte)1 : (byte)0;
			apdu.setOutgoingAndSend((short)0, (short)1);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

}
