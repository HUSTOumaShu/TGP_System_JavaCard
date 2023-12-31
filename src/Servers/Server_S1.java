/*
	The Applet receive request get the sharedKey generated by TGP
*/

package Servers;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;
import TGP_Server.SharedKey;

public class Server_S1 extends Applet
{
	final static byte[] TGP_AID = new byte[] {0x00, 0x20, 0x20, 0x47, 0x57, 0x01};
	
	private final static byte INS_GETKEY = (byte)0x00;	
	private byte[] buf_temp, enc_buffer, dec_buffer, sharedKey;
	private short keyLen;
	
	private Server_S1() {
		keyLen = (short)(KeyBuilder.LENGTH_RSA_1024/8);
		sharedKey = new byte[keyLen];
		enc_buffer = new byte[keyLen];
		dec_buffer = new byte[keyLen];
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Server_S1().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			return;
		}

		byte[] buf = apdu.getBuffer();
		apdu.setIncomingAndReceive();
		
		switch (buf[ISO7816.OFFSET_INS])
		{
		case INS_GETKEY:
			getKey(buf);
			apdu.setOutgoing();
			apdu.setOutgoingLength(keyLen);
			apdu.sendBytesLong(sharedKey, (short)0, keyLen);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	private void getKey(byte[] buf) {
		AID masterAID = JCSystem.lookupAID(TGP_AID, (short)0, (byte)TGP_AID.length);
		SharedKey Key = (SharedKey)JCSystem.getAppletShareableInterfaceObject(masterAID, (byte)0x00);
		keyLen = Key.getKey(buf);
		Util.arrayCopy(buf, (short)0, sharedKey, (short)0, keyLen);
	} 

}
