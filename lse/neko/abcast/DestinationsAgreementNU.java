package lse.neko.abcast;

// java imports:
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.GUID;


/**
 * Implementation of the non-uniform destinations agreement algorithm.
 *
 * <p>Status: working version:<br>
 * Basically this is the same as Skeen's algorithm and so the code<br>
 * is only an appropriately reworked version of the original code.<br>
 * However I will make a few changes to this soon.</p>
 *
 * <p>Algorithm details :<br>
 * Page 155 of "Agreement-Related Problems: From Semi-passive<br>
 * Replication To Totally Ordered Broadcast" Xavier Defago</p>
 *
 * @author Arindam Chakraborty
 */
public class DestinationsAgreementNU
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface
{

    /* VARIABLES -----------------------------------*/

    /**
     * Map from IDs to timestamps.
     */
    private Map idToTimestamps;

    /**
     * Map from IDs to Elements.
     */
    private Map idToElement;

    /**
     * The set of received messages.
     */
    private SortedSet received;

    /**
     * Lamport clock for the process.
     * @see lse.neko.abcast.LamportClock
     */

    private LamportClock clock;

    protected static final int START_SKEEN = 30;
    protected static final int RESPONSE_TO_SENDER = 31;
    protected static final int SEND_SEQ_NUMBER = 32;

    /**
     * Message Id's local to the algorithm.
     */

    static {
        MessageTypes.instance().register(START_SKEEN, "START_SKEEN");
        MessageTypes.instance().register(RESPONSE_TO_SENDER,
                                         "RESPONSE_TO_SENDER");
        MessageTypes.instance().register(SEND_SEQ_NUMBER, "SEND_SEQ_NUMBER");
    }

    /* CONSTRUCTOR ----------------------------------------*/

    private NekoProcess process;

    public DestinationsAgreementNU(NekoProcess process) {
        this.process = process;
        idToTimestamps = new HashMap();
        idToElement = new HashMap();
        received = new TreeSet();
        clock = new LamportClock();
    } // end constructor

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    /* METHODS -----------------------------------------------*/

    /* Inherited and Overridden methods */

    /**
     * Broadcast the NekoMessage so that delivery is in total order.
     * @param m NekoMessage, message to send by total order atomic broadcast
     */
    public synchronized void send(NekoMessage m) {

        NekoMessage m1 =
            new NekoMessage(m.getSource(), m.getDestinations(),
                            getId(),
                            new ContentSKEEN(m.getProtocolId(),
                                             m.getContent(),
                                             m.getType(),
                                             new GUID(process)),
                            START_SKEEN);
        sender.send(m1);
    } // end send

    /**
     * Deliver messages according to Non-Uniform Destinations Agreement
     * algorithm.
     * @param m NekoMessage, message to deliver
     */

    public synchronized void deliver(NekoMessage m) {

        switch (m.getType()) {

        case RESPONSE_TO_SENDER:
            ContentID contentID = (ContentID) m.getContent();
            GUID id1 = contentID.getId();
            Timestamps timestamps = (Timestamps) idToTimestamps.get(id1);
            if (timestamps == null) {
                timestamps = new Timestamps(contentID.getDest().length);
                idToTimestamps.put(id1, timestamps);
            } // end if
            timestamps.update(contentID.getTimestamp());
            if (timestamps.ready()) {
                int max = timestamps.getMax();
                NekoMessage m2 =
                    new NekoMessage(contentID.getDest(),
                                    getId(),
                                    new ContentDeliver(id1, max),
                                    SEND_SEQ_NUMBER);
                sender.send(m2);
            } // end if
            break;

        case START_SKEEN:
            ContentSKEEN content = (ContentSKEEN) m.getContent();
            GUID id2 = content.getId();
            Element element =
                new Element(new NekoMessage(m.getSource(),
                                            m.getDestinations(),
                                            content.getProtocolId(),
                                            content.getContent(),
                                            content.getType()),
                            clock.getValue(), id2);
            idToElement.put(content.getId(), element);
            received.add(element);

            // Response from the receiver to the sender of the original Message
            // for calculation of the sequence number
            int[] destination = {m.getSource()};
            NekoMessage m1 = new NekoMessage(destination,
                                             getId(),
                                             new ContentID(content.getId(),
                                                           m.getDestinations(),
                                                           clock.getValue()),
                                             RESPONSE_TO_SENDER);
            sender.send(m1);
            clock.update();
            break;

        case SEND_SEQ_NUMBER:
            ContentDeliver contentID1 = (ContentDeliver) m.getContent();
            int timestamp = contentID1.getTimestamp();
            clock.update(timestamp);
            Element element2 = (Element) idToElement.get(contentID1.getId());
            received.remove(element2);
            element2.stamp(timestamp);
            received.add(element2);
            Iterator it = received.iterator();
            while (it.hasNext()) {
                Element e = (Element) it.next();
                if (!e.getStamped()) {
                    break;
                }
                it.remove();
                receiver.deliver(e.getMessage());
            } // end while
            break;

        default:
            throw new UnexpectedMessageException(m);
        } // end switch

    } // end deliver

    /* Required Functions and Classes */

    /**
     * Container class for algorithmic data to be exchanged over the network
     * Element contains message, timestamp, guid and boolean indicator of
     * whether the message is stamped or not.
     *
     * @author Arindam Chakraborty
     */

    public static class Element implements Serializable, Comparable {

        private NekoMessage message;
        private int timestamp;
        private GUID id;
        private boolean stamped;

        public Element(NekoMessage message, int timestamp, GUID id) {
            this.message = message;
            this.timestamp = timestamp;
            this.id = id;
            stamped = false;
        } // end constructor

        public NekoMessage getMessage() {
            return message;
        } // end getMessage

        public int getTimestamp() {
            return timestamp;
        } // end getTimestamp

        public GUID getId() {
            return id;
        } // end getId

        public boolean getStamped() {
            return stamped;
        } // end getStamped

        public void stamp(int newTimestamp) {
            stamped = true;
            this.timestamp = newTimestamp;
        } // end stamped

        public int compareTo(Object right) {
            Element r = (Element) right;
            if (timestamp != r.timestamp) {
                return (timestamp < r.timestamp) ? -1 : +1;
            } else if (stamped != r.stamped) {
                return (r.stamped) ? -1 : +1;
            } else {
                return id.compareTo(r.id);
            }
        } // end compareTo

    } // end class Element

    /**
     * Timestamp class for time data to be exchanged over the network.
     *
     * @author Arindam Chakraborty
     */
    public static class Timestamps implements Serializable {

        private int expected;
        private int max;

        public Timestamps(int expected) {
            this.expected = expected;
            max = Integer.MIN_VALUE;
        } // end constructor

        public void update(int timestamp) {
            if (max < timestamp) {
                max = timestamp;
            }
            expected--;
        } // end update

        public boolean ready() {
            return expected == 0;
        } // end ready

        public int getMax() {
            return max;
        } // end getMax

    } // end class Timestamps

} // end class DestinationsAgreementNU
