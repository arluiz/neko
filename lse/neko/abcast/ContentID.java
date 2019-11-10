package lse.neko.abcast;

// java imports:
import java.io.Serializable;

// lse.neko imports:
import lse.neko.util.GUID;


/**
 * Content that is passed from the Receiver to  the Sender to calculate
    the sequence number, and from the Sender to the Receiver to send back
    the sequence number.
 */
public class ContentID implements Serializable {

    private GUID id;
    private int[] dest;
    private int timestamp;

    public ContentID(GUID id, int[] dest, int timestamp) {
        this.id = id;
        this.dest = dest;
        this.timestamp = timestamp;
    }

    public GUID getId() {
        return id;
    }

    public int[] getDest() {
        return dest;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public static String printIntArray(int[] array) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            b.append(((i == 0) ? "{" : ", ") + array[i]);
        }
        b.append("}");
        return b.toString();
    }

    public String toString() {
        return "ContentID[" + id + " sent to " + printIntArray(dest)
            + ", " + timestamp + "]";
    }

}


