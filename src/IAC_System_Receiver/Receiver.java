package IAC_System_Receiver;

import javacard.framework.*;
import IAC_System_Sender.SharedInterface;

public class Receiver extends Applet
{
	final static byte[] senderAID = new byte[] {0x20, 0x20, 0x20, 0x47, 0x57, 0x01};

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Receiver().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
		case (byte)0x00:
			AID masterAID = JCSystem.lookupAID(senderAID, (short)0, (byte)senderAID.length);
			SharedInterface si = (SharedInterface) JCSystem.getAppletShareableInterfaceObject(masterAID, (byte)0x00);
			short len = si.getData(buf);
			apdu.setOutgoingAndSend((short)0, len);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

}
