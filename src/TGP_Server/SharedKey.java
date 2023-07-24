package TGP_Server;
import javacard.framework.Shareable;
public interface SharedKey extends Shareable {
	public short getKey(byte[] key);
}
