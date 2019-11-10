package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.CompressedIntSet;
import lse.neko.util.Timer; // ambiguous with: java.util.Timer
import lse.neko.util.TimerTask; // ambiguous with: java.util.TimerTask
import lse.neko.util.logging.NekoLogger;

/**
 * Implements detection of message stability.
 *
 * <p>Status = working version :<br>
 * Message is stable when it has received enough acknowledgments from
 * members of the group. By default all N process are members of the group.
 * and number of requeired ack in N.<br>
 * @author Ilya Shnaiderman
 *
 * Note:<br>
 * <p>Acks sent continuously, (e.g. if A process have acknowledged a
 * message number k, that means this process had received ALL messages
 * from 0 till k.<br>
 */
public abstract class StabilityLayer
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface
{
    /**
     * Contains numbers of already delivered messages.
     */
    protected CompressedIntSet delivered;

    /**
     * Sequence number of the stable message with maximal sequence number.
     */
    protected int prevLimit = 0;

    /**
     * Here I am collecting acks.
     */
    protected AckCollector ackCollector;

    /**
     * Processes to send the acknowledgment to them.
     */
    protected int[] ackWaitors;

    /**
     * Provides the interface for this type of messages,
     * Stability information is colleted only for messages of specified type.
     */
    protected SequencedMessageInterface sequencedMessage;

    /**
     * Used to distinguish beetween different
     * sessions (Views in case of Group Communication Systems).
     */
    protected int sessionId = 0;

    /**
     * Time between sending acknowlegments.
     */
    protected double maximalAckDelay = 0.0;

    /**
     * True if acks are allowed to be sent piggybacked on other messages.
     */
    protected boolean piggybackEnabled = false;

    /**
     * Seq. number of last acknowledgedment packet per process
     */
    private int[] lastAckThatWasSentToTheProcess;

    /**
     *  Number of processes that are still waiting for the ack
     *  from us.
     */
    private int waitForOurAck;

    /**
     * This flag (per process number) is turned on if the process is one
     * of the ack waitors.
     */
    private boolean[] processesBitMask;

    // reference to the scheduler
    private Timer timer;

    // task  for the scheduler
    private TimerTask task = null;

    /**
     * Message IDs local to the algorithm.
     */
    protected final int SEQUENTIAL_ACKNOWLEDGMENT =
        getMessageTypeSequentialAcknolegment();
    protected abstract int getMessageTypeSequentialAcknolegment();

    private static final Logger logger =
        NekoLogger.getLogger(StabilityLayer.class.getName());

    // dummy Object for synchronization
    private Object lock = new Object();

    protected NekoProcess process;

    /* CONSTRUCTOR ----------------------------------------*/
    public StabilityLayer(NekoProcess process,
                          SequencedMessageInterface sequencedMessage)
    {
        this.process = process;

        this.sequencedMessage = sequencedMessage;

        lastAckThatWasSentToTheProcess = new int[process.getN()];
        processesBitMask = new boolean[process.getN()];

        timer = NekoSystem.instance().getTimer();

        // by default acks collected by everybody, and
        // and an ack required from everyone, and session id is to 0
        int[] all = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            all[i] = i;
        }
        reset(all, process.getN(), 0);
    } // constructor

    protected SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    protected ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    /**
     * Constructor that enables ack piggybacking.
     */
    public StabilityLayer(NekoProcess process,
                          SequencedMessageInterface sequencedMessage,
                          double maximalAckDelay)
    {
        this(process, sequencedMessage);

        piggybackEnabled = true;
        this.maximalAckDelay = maximalAckDelay;

    } // end constructor

    /* METHODS -----------------------------------------------*/
    public void reset(int[] newAckWaitors,
                      int requiredAcksNumber,
                      int newSessionId)
    {
        synchronized (lock) {
            ackWaitors = (int[]) newAckWaitors.clone();
            sessionId = newSessionId;

            ackCollector = new AckCollector(requiredAcksNumber);
            // We suppose messages numbered from zero for
            // every new set of ackWaitors
            delivered = new CompressedIntSet();
            for (int i = 0; i < process.getN(); i++) {
                lastAckThatWasSentToTheProcess[i] = -1;
                processesBitMask[i] = false;
            }

            for (int i = 0; i < ackWaitors.length; i++) {
                processesBitMask[ackWaitors[i]] = true;
            }

            if (task != null) {
                task.cancel();
                task = null;
            }

        } // synchoronized
    } // logger

    /**
     * If a message has been received at least twice this message is
     * called.
     */
    protected void duplicateReceived(int seqNum) {
        logger.warning("Duplicate received. Message discarded seqNum "
                       + seqNum);
        throw new RuntimeException
            ("Message seqNum " + seqNum + " received at least twice");
    }

    /**
     * If a message with unexpected sessionId has been received.
     */
    protected void nonExpectedMessageHandler(NekoMessage m,
                                             int messageSessionId)
    {
        logger.warning("Message discarded " + m);
        throw new RuntimeException
            ("Message " + m + " is not relevant ");
    }

    /**
     * This method is called when number of required acks has been
     * collected for at least one message.
     */
    protected abstract void updateLimit(int newLimit, int previousLimit);

    // this method is called when a message received and there is no
    // 'gaps' beetween this message and 0.  @filled is seq. number of
    // a message before the gap (or last message that has been
    // received, if there is no gaps.
    private void filledUpdated() {
        //logger.finest("filledUpdated called" + delivered.getFilled());
        if (!piggybackEnabled) {
            Content c;
            synchronized (lock) {
                c = new Content(null, delivered.getFilled() ,
                                sessionId);
            }
            NekoMessage m = new NekoMessage(ackWaitors, getId(), c,
                                            SEQUENTIAL_ACKNOWLEDGMENT);
            sender.send(m);
        } else {
            synchronized (lock) {
                waitForOurAck = ackWaitors.length;
                if (task != null) {
                    return;
                }

                task = new TimerTask() {
                        public void run() { callback(); }
                    };
                //logger.finest("Callback scheduled");
                timer.schedule(task, maximalAckDelay);
            } // synchronized
        } // else
    } // filledUpdated

    /**
     * Called when a message that required acknowledgedments
     * has been received.
     */
    protected void sequencedMessageReceived(NekoMessage m) {
        Iterator it = sequencedMessage.iterator(m);
        boolean empty = true;
        while (it.hasNext()) {
            NekoMessage m1 = (NekoMessage) it.next();
            int seqNum = sequencedMessage.getSeqNum(m1);
            if (!addMessageToDelivered(seqNum)) {
                it.remove();
            } else {
                empty = false;
            }
        }

        if (!empty) {
            receiver.deliver(m);
        }
    }

    /**
     * Adds this message to set of delivered, and returns true if
     * this is a new message.
     */
    protected boolean addMessageToDelivered(int seqNum) {
        // Flag newMessage is used to avoid call to deliver function
        // inside synchronized block
        boolean newMessage = false;
        int prevFilled = delivered.getFilled();
        int newFilled = prevFilled;
        synchronized (lock) {
            if (delivered.contains(seqNum)) {
                duplicateReceived(seqNum);
            } else {
                delivered.add(seqNum);
                //logger.finest("Message number " + seqNum +
                //              "added to delivered");
                newMessage = true;
                newFilled = delivered.getFilled();
            }
        }

        if (prevFilled < newFilled) {
            filledUpdated();
        }

        return newMessage;

    } // addMessagesToDelivered

    /**
     * Called when an relevant ack has been received.
     */
    protected void ackReceived(int source, Content c) {
        if (ackCollector.addAckFrom(source, c.getSeqNum())) {
            //logger.finest("calling updateLimit");
            synchronized (lock) {
                updateLimit(ackCollector.getLimit(), prevLimit);
                prevLimit = ackCollector.getLimit();
            }
        }

    }

    // Returns true if the message has been sent, and false otherwise.
    // If this function returns false, the caller have to send message
    // by himself.
    private boolean putPiggybackOnMessage(NekoMessage m) {

        Content c;
        int[] targets = m.getDestinations();

        if (targets == null || (targets.length == 0)) {
            logger.info("Strange target");
            return false;
        }

        if (targets.length == 1) {
            // it not worth so send ack to only one participant
            return false;
        }

        if ((m.getType() == SimulatedFailureDetector.START_FAILURE)
            || (m.getType() == SimulatedFailureDetector.STOP_FAILURE))
        {
            return false;
        }


        boolean sendAckPiggybacked = false;
        synchronized (lock) {
            if (waitForOurAck <= 0) {
                return false;
            }
            int filled = delivered.getFilled();
            for (int i = 0; i < targets.length; i++) {
                if (!processesBitMask[targets[i]]) {
                    continue;
                }

                if (lastAckThatWasSentToTheProcess[targets[i]] < filled) {
                    sendAckPiggybacked = true;
                    lastAckThatWasSentToTheProcess[targets[i]] = filled;
                    waitForOurAck--;
                }
            } // end for

            if (!sendAckPiggybacked) {
                return false;
            }

            if (m.getSource() < 0) {
                m.setSource(process.getID());
            }

            c = new Content(m, delivered.getFilled(), sessionId);
            if ((waitForOurAck == 0) && (task != null)) {
                task.cancel();
                task = null;
            }
        } // synchronized

        NekoMessage m1 = new NekoMessage(targets, getId(), c,
                                         SEQUENTIAL_ACKNOWLEDGMENT);
        sender.send(m1);
        return true;

    } // putPiggybackOnMessage

    // this callback called once a message received.
    private void callback() {

        int[] dest;
        Content c;
        //logger.finest("Callback called");
        List ll = new LinkedList();

        synchronized (lock) {
            waitForOurAck = 0;
            task = null;

            int filled = delivered.getFilled();

            for (int i = 0; i < ackWaitors.length; i++) {
                int p = ackWaitors[i];
                if (lastAckThatWasSentToTheProcess[p] < filled) {
                    lastAckThatWasSentToTheProcess[p] = filled;
                    ll.add(new Integer(p));
                }
            }


            if (ll.size() == 0) {
                return; // could happend on race conditions;
            }

            dest = new int[ll.size()];
            Iterator it = ll.iterator();
            for (int i = 0; i < dest.length; i++) {
                dest[i] = ((Integer) (it.next())).intValue();
            }

            c = new Content(null, filled, sessionId);

        } // synchronized

        NekoMessage m = new NekoMessage(dest, getId(),
                                        c, SEQUENTIAL_ACKNOWLEDGMENT);
        sender.send(m);

    } // end of callback

    /* Inherited and overridden methods */
    public void deliver(NekoMessage m) {
        //logger.finest("Message received " + m);

        if ((sequencedMessage != null)
            && (m.getType() == sequencedMessage.getType()))
        {
            //logger.finest("Message for acknowlegment");

            if (sequencedMessage.getSessionId(m) == sessionId) {
                sequencedMessageReceived(m);
            } else {
                // it seems that we have received non relevant message
                nonExpectedMessageHandler(m,
                                          sequencedMessage.getSessionId(m));
            }
        } else if (m.getType() == SEQUENTIAL_ACKNOWLEDGMENT) {

            Content c = (Content) m.getContent();
            NekoMessage m1 = c.getMessage();
            if (sessionId != c.getSessionId()) {
                if (c.getMessage() != null) {
                    // I don't want a message used for piggyback to be handled
                    // by nonExpectedMessageHandler
                    Content c1 = new Content(null, c.getSeqNum(),
                                             c.getSessionId());
                    NekoMessage m2 = new
                        NekoMessage(m.getDestinations(), getId(), c1,
                                    SEQUENTIAL_ACKNOWLEDGMENT);
                    m2.setSource(m.getSource());
                    nonExpectedMessageHandler(m2, c.getSessionId());
                } else {
                    nonExpectedMessageHandler(m, c.getSessionId());
                }
            } else {
                ackReceived(m.getSource(), c);
            }

            if (m1 != null) { // the ack has been piggybacked on a message,
                // so let deliver the message.
                //logger.finest("Piggyback message has been delivered " + m1);
                receiver.deliver(m1);
            }
        } else {
            //logger.finest("Message will be delivered " + m);
            throw new UnexpectedMessageException(m);
        } // end switch

    } // end deliver

    public void send(NekoMessage m) {

        if ((piggybackEnabled) && putPiggybackOnMessage(m)) {
            return;
        }

        sender.send(m);
    } // end send

    public static class Content
        implements Serializable
    {

        private int seqNum;
        private int sessionId;

        private NekoMessage message;

        public Content(NekoMessage m, int seqNum, int sessionId) {
            this.message = m;
            this.seqNum = seqNum;
            this.sessionId = sessionId;
        } // end constructor

        public NekoMessage getMessage() {
            return message;
        } // end getMessage

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public int getSessionId() {
            return sessionId;
        } // end getSessionId

        public String toString() {
            if (message != null) {
                return "seqNum " + seqNum + " sessionId "
                    + sessionId + " message " + message;
            } else {
                return "seqNum " + seqNum + " sessionId "
                    + sessionId;
            }
        } // end toString()

    } // end class Content

} // end class StabilityLayer
