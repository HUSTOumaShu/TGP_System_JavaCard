package IAC_System_Sender;

import javacard.framework.*;

public class Sender extends Applet implements SharedInterface
{
	private byte[] testData;
	public Sender() {
		testData = new byte[] {0x20, 0x20, 0x47, 0x57};
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Sender().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	public Shareable getShareableInterfaceObject(AID receiverAID, byte parameter) {
		// verify receiver
		if(parameter != (byte)0x00){
			return null;
		}
		return this;
	}
	
	public short getData(byte[] data) {
		short len = (short)testData.length;
		Util.arrayCopy(testData, (short)0, data, (short)0, len);
		return len;
	} 

}
