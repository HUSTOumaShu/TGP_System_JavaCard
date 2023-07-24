package IAC_System_Sender;
import javacard.framework.Shareable;

public interface SharedInterface extends Shareable {
	public short getData(byte[] data);
}
