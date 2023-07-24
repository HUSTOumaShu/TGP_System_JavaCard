/*
	TGP is the Applet verify user and generate random Key for user and another Server
*/

package TGP_Server;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;
import TGP_Server.SharedKey;

public class TGP extends Applet implements SharedKey
{
	// Variable for verify Signature
	private static final byte INS_SIGN = (byte)0x00;
	private static final byte INS_VERIFY = (byte)0x01;
	
	private RSAPrivateKey rsaPrivate;
	private RSAPublicKey rsaPublic;
	private Signature rsaSig;
	
	private byte[] buf_temp, salt, sig_buffer, error_buffer;
	private short sigLen;
	
	// Variable for generate random key
	private RandomData ranData;
	private byte[] seed, ranKey;
	
	private TGP() {
		salt = new byte[] {0x20, 0x20, 0x47, 0x57};
		error_buffer = new byte[] {0x00};
		
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
		new TGP().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
			rsaSign(apdu);
			break;
		case INS_VERIFY:
			rsaVerify(apdu);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
/*
	Function for Signature
*/
	// Sign with data from buf_temp and salt
	private void rsaSign(APDU apdu) {
		rsaSig.init(rsaPrivate, Signature.MODE_SIGN);
		rsaSig.update(salt, (short)0, (short)salt.length);
		rsaSig.sign(buf_temp, (short)0, (short)buf_temp.length, sig_buffer, (short)0);
		
		apdu.setOutgoing();
		apdu.setOutgoingLength((short)(buf_temp.length + sig_buffer.length));
		apdu.sendBytesLong(buf_temp, (short)0, (short)buf_temp.length);
		apdu.sendBytesLong(sig_buffer, (short)0, (short)sig_buffer.length);
	}
	
	// Verify signature
	private void rsaVerify(APDU apdu) {
		rsaSig.init(rsaPublic, Signature.MODE_VERIFY);
		rsaSig.update(salt, (short)0, (short)salt.length);
		boolean result = rsaSig.verify(buf_temp, (short)0, (short)buf_temp.length, sig_buffer, (short)0, sigLen);
		
		// verify successful
		if(result == true) {
			// send sharedKey to client
			genRandomKey(apdu);
		}
		
		// verify fail
		else {
			// send error_buffer to client
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)error_buffer.length);
			apdu.sendBytesLong(error_buffer, (short)0, (short)error_buffer.length);
		}
	}
	
/*
	Function for generating random key
*/
	private void genRandomKey(APDU apdu) {
		seed = new byte[(short)buf_temp.length];
		Util.arrayCopy(buf_temp, (short)0, seed, (short)0, (short)buf_temp.length);
		ranData = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
		ranData.setSeed(seed, (short)0, (short)seed.length);
		short ranLen = (short)(KeyBuilder.LENGTH_RSA_1024/8); // 256 bytes
		
		ranKey = new byte[ranLen];
		ranData.generateData(ranKey, (short)0, ranLen);
		apdu.setOutgoing();
		apdu.setOutgoingLength(ranLen);
		apdu.sendBytesLong(ranKey, (short)0, ranLen);
	}
	
/*
	Function for sending sharedKey
*/
	public Shareable getShareableInterfaceObject(AID serverAID, byte parameter) {
		if(parameter != 0x00) {
			return null;
		}
		return this;
	}
	
	public short getKey(byte[] key) {
		short len = (short)ranKey.length;
		Util.arrayCopy(ranKey, (short)0, key, (short)0, len);
		return len;
	}

}
