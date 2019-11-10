package lse.neko.layers;

// java imports:
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


/**
 * Implementation of the acknowledgments collector.
 *
 * <p>Status = working version :<br>
 * Acks have to be sent to sequentially. It means an ack to message n
 * also acknowledges messages 0..n-1,n.<br>
 *
 * <p>Algorithm details :<br>
 * This class doesn't know set of receivers(ack senders), but
 * it waits for at least @requiredAcksNumber receivers to acknowledge
 * an message. In this case addAckFrom function returns true.</p>
 *
 * @author Ilya Shnaiderman
 */
public class AckCollector {
    // maps a receiver to the seq. number of the last acknowledge message.
    private Map acks;

    // seq. number of a message that has been acknowledged by
    // required number of receivers.
    private int limit = -1;

    // number of process that need to advance their value before
    //limit could change
    private int numberOfOutsiders = 0;

    // required number of receivers to consider a message stable.
    // This means a last addAckFrom will return true, and getLimit
    // will return seq. number bigger that it returned before.
    private int requiredAcksNumber;

    private static final Logger logger =
        NekoLogger.getLogger(AckCollector.class.getName());
    // dummy Object for synchronization
    private Object lock = new Object();

    // constructor
    public AckCollector(int requiredAcksNumber) {
        acks = new LinkedHashMap();
        this.requiredAcksNumber = requiredAcksNumber;
    }

    /**
     * Adds an ack from receiver receiver to message with seqNum.
     */
    public boolean addAckFrom(int receiver, int seqNum) {
        return addAckFrom(new Integer(receiver), new Integer(seqNum));
    }

    /**
     * Adds an ack from receiver receiver to message with seqNum.
     */
    public boolean addAckFrom(Object receiver, Integer seq) {

        if ((((Integer) receiver).intValue()) < 0) {
            throw new IllegalArgumentException();
        }
        Integer prevValue = (Integer) acks.get(receiver);

        // logger.finest("Received ack from: "
        //               + receiver + " to message " + seq);

        // is it something new?
        if ((prevValue != null) && (prevValue.compareTo(seq) > 0)) {
            return false; // it is an old ack
        }

        acks.put(receiver, seq);

        // do we have enough acks?
        if (acks.size() < requiredAcksNumber) {
            return false;
        }

        // is it one of the outsiders?
        int p, s;
        if (prevValue == null) {
            p = -1;
        } else {
            p = prevValue.intValue();
        }
        s = seq.intValue();
        // at least one of the slowest processors advanced
        if (p <= limit && s > limit) {
            numberOfOutsiders--;
            if (numberOfOutsiders <= 0) { // ok let's check the limit
                return recalculateLimit();
            }
        }

        return false;
    }

    // calculates @limit according to information in the @acks map.
    private boolean recalculateLimit() {

        Object []array = acks.values().toArray();
        Arrays.sort(array);

        int len = array.length;

        Integer newLimit =
            (Integer) array[len - requiredAcksNumber];

        if (newLimit.intValue() <= limit) {
            return false;
        }

        limit = newLimit.intValue();
        numberOfOutsiders = 0;

        for (int i = len - requiredAcksNumber; i < len; i++) {
            if (newLimit.compareTo(array[i]) == 0) {
                numberOfOutsiders++;
            } else {
                break;
            }
        }
        return true;
    }

    /**
     * Returns the sequence number of a message that has received the
     * required number of acks. Returns the biggest sequence number
     * among such messages.
     */
    public int getLimit() {
        return limit;
    }
} // end of class AckCollector

