package lse.neko.util;

// java imports:
import java.io.ByteArrayOutputStream;


/**
 * This class makes the protected buf and count fields of
 * ByteArrayOutputStream available. Thus applications might avoid
 * the allocation and copy that happens in toByteArray.
 */
public class NoCopyByteArrayOutputStream
    extends ByteArrayOutputStream
{
    public NoCopyByteArrayOutputStream() {
        super();
    }

    public NoCopyByteArrayOutputStream(int size) {
        super(size);
    }

    public byte[] getBuf() {
        return buf;
    }

    public int getCount() {
        return count;
    }

    /**
     * Skipping n bytes. Does not change those n bytes.
     * if the byte array is resized, the extra bytes will be zeros.
     * The parameter is an int, not a long as in InputStream,
     * for array indexes are ints anyway.
     */
    public void skip(int n) {
        int newCount = count + n;
        if (newCount > buf.length) {
            byte[] newbuf = new byte[Math.max(buf.length << 1, newCount)];
            System.arraycopy(buf, 0, newbuf, 0, count);
            buf = newbuf;
        }
        count = newCount;
    }

}


