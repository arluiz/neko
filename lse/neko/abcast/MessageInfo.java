package lse.neko.abcast;


/**
 * Information about a message, used by Lamport's algorithm.  Objects
 * of this class contain the message m, the timestamp of the initial
 * send of m and the ID of the process that did the initial send of m.
 *
 * @author  Sing Souksavanh
 */
public class MessageInfo
    implements Comparable
{

    /**
     * The timestamp of the message.
     */
    private final int ts;

    /**
     * ID of the sender process.
     */
    private final int pID;

    /**
     * Creates a new instance.
     *
     * @param ts timestamp of m
     * @param p  ID of sender process
     */
    public MessageInfo(int ts, int p) {
        this.ts = ts;
        this.pID = p;
    }

    /**
     * Compares this MessageInfo object with another.
     *
     * @param r MessageInfo object to compare with
     */
    public int compareTo(MessageInfo r) {

        if (ts < r.getTS()) {
            return -1;
        } else if (ts > r.getTS()) {
            return 1;
        } else if (pID < r.getPID()) {
            return -1;
        } else if (pID > r.getPID()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int compareTo(Object r) {
        return compareTo((MessageInfo) r);
    }

    /**
     * Returns the timestamp.
     */
    public int getTS() {
        return ts;
    }

    /**
     * Returns the ID of the sender process.
     */
    public int getPID() {
        return pID;
    }

    public String toString() {
        return "[pid:" + pID + ";LC:" + ts + "]";
    }
}


