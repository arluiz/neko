
package lse.neko.layers;

// java imports:
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.util.logging.NekoLogger;


/**
 * Implements detection of message stability in a group that supports
 * Virtual Synchrony.
 *
 * <p>Status = working version :<br>
 * Message is stable when it has received acknowledgments from every
 * member of the group. This class uses StabilityLayer class to
 * detect when a message becames stable function updateLimit is called.
 * <p>When a message of the specified type arrived it stored till it becomes
 * stable<br>.
 * When an upper layer sends ACK_MEMBERSHIP_CHANGE message to layer below,
 * all unstable messages added to the ACK_MEMBERSHIP_CHANGE message.
 * @author Ilya Shnaiderman
 */
public class VSStabilityLayer
    extends StabilityLayer
    implements AckMembershipChangeListener, NewViewListener
{
    /**
     * Contains all message that are still have not be acknowledged by
     * every alive member of the group.
     */
    protected Map unstable;

    /**
     * Messages that will be handled when a new view will be received.
     */
    protected List bufferedMessages;

    /**
     * Message IDs local to the algorithm.
     */
    protected int getMessageTypeSequentialAcknolegment() {
        return VS_SEQUENTIAL_ACKNOWLEDGMENT;
    }

    public static final int VS_SEQUENTIAL_ACKNOWLEDGMENT = 2235;

    static {
        MessageTypes.instance().register(VS_SEQUENTIAL_ACKNOWLEDGMENT,
                                         "VS_SEQUENTIAL_ACKNOWLEDGMENT");
    }
    private static final Logger logger =
        NekoLogger.getLogger(VSStabilityLayer.class.getName());

    // dummy Object for synchronization
    private Object lock = new Object();

    /* CONSTRUCTOR ----------------------------------------*/
    public VSStabilityLayer(NekoProcess process,
                            SequencedMessageInterface sequencedMessage)
    {
        super(process, sequencedMessage);
        bufferedMessages = new LinkedList();
    } // end constructor

    /**
     * Constructor that enables piggybacking.
     */
    public VSStabilityLayer(NekoProcess process,
                            SequencedMessageInterface sequencedMessage,
                            double maximalAckDelay)
    {
        super(process, sequencedMessage,
              maximalAckDelay);
        bufferedMessages = new LinkedList();
    }

    private AckMembershipChangeListener ackMembershipChangeListener;

    public void setAckMembershipChangeListener(AckMembershipChangeListener
                                               ackMembershipChangeListener)
    {
        this.ackMembershipChangeListener = ackMembershipChangeListener;
    }

    /* METHODS -----------------------------------------------*/
    /**
     * Called Whenever a new view has been received.
     */
    public void handleNewView(GroupView view) {
        reset(view.getProcesses(),
              view.getMembersNumber(),
              view.getViewId());
        // do we have something buffered and need processing?
        processBufferedMessages();
    }

    /**
     * When a new view arrives all messages that were suspended before
     * are processed.
     */
    protected void processBufferedMessages() {

        Iterator it;
        synchronized (lock) {
            it = bufferedMessages.iterator();
            bufferedMessages = new LinkedList();
        }

        while (it.hasNext()) {
            deliver((NekoMessage) (it.next()));
        }
    }

    /* Inherited and overridden methods */
    /**
     * Updates the StabilityLayer about changes in the group.
     */
    public void reset(int[] ackWaitors,
                      int requiredAcksNumber,
                      int sessionId)
    {
        super.reset(ackWaitors, requiredAcksNumber, sessionId);
        // we could empty unstable messages
        // if there is a new View.
        unstable = new LinkedHashMap();
    }

    /**
     * Removes messages up to newLimit from the unstable set.
     */
    protected void  updateLimit(int newLimit, int prevLimitParam) {
        //logger.finest("Stable:" + (newLimit-1));
        synchronized (lock) {
            int prevLimit = prevLimitParam;
            while (prevLimit < newLimit) {
                if (unstable.remove(new Integer(prevLimit)) == null) {
                    throw new RuntimeException
                        ("Can not find message in unstable set " + prevLimit);
                } else {
                    logger.log(Level.FINER, "Message number {0,number,#}"
                               + " removed from unstable messages",
                               new Integer(prevLimit));
                }
                prevLimit++;
            }
        } // synchronized
    }

    /**
     * Deals with messages that were sent in views that are differ
     * from current one.
     */
    protected void nonExpectedMessageHandler(NekoMessage m,
                                             int messageSessionId)
    {
        // We have to store messages with higher ViewId and
        // process them
        // after the new view is received
        if (messageSessionId == sessionId) {
            throw new IllegalArgumentException
                ("Message " + m + " is relevant! ");
        }

        if (messageSessionId < sessionId) {
            logger.info("Message discarded " + m);
        } else {
            synchronized (lock) {
                bufferedMessages.add(m);
            }
        }
    }

    /**
     * Puts this message m in the unstable set.
     */
    protected void sequencedMessageReceived(NekoMessage m) {

        Iterator it = sequencedMessage.iterator(m);

        while (it.hasNext()) {
            NekoMessage m1 = (NekoMessage) it.next();
            int seqNum = sequencedMessage.getSeqNum(m1);
            synchronized (lock) {
                logger.log(Level.FINER, "Message added to unstable {0}", m);
                unstable.put(new Integer(seqNum), m1);
            } // synchronized
        } // while
        super.sequencedMessageReceived(m);
    }

    public void ackMembershipChange(Map u) {

        synchronized (lock) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "ackMembershipChange {0}",
                           u.toString());
            }
            u.putAll(unstable);
            logger.info("Amount of unstable messages is: "
                        + unstable.size());
            ackMembershipChangeListener.ackMembershipChange(u);
        }
    }

} // end VSStabilityLayer

