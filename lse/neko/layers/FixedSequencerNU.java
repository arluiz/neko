package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.CompressedIntSet;
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Implementation of the non-uniform fixed sequencer algorithm.
 *
 * <p>Status = working version :<br>
 * 1) the sequencer is also a part of the destination<br>
 * processes and not a separate sequencer.<br>
 * 2) the destination group of processes is not constant and at the beginning
 * includes all.</p>
 *
 * <p>Algorithm details :<br>
 * Page 151 of "Agreement-Related Problems: From Semi-passive<br>
 * Replication To Totally Ordered Broadcast" Xavier Defago</p>
 *
 * @author Arindam Chakraborty
 */
public class FixedSequencerNU
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface
{

    /* VARIABLES -----------------------------------*/

    /**
     * Flag, that allows to disable send.
     * When this flag set to false, all messages sent to send are discarded
     */
    protected boolean enabled = true;

    /**
     * True if the process is the sequencer.
     */
    protected boolean isSequencer;

    /**
     * Sequence number common for all sources.
     */
    protected int seqNum;

    /**
     *  Seqnumber of messages by this sender.
     */
    protected int localSeqNum = -1;

    /**
     * Seqnumber of messages by this sender.
     */
    protected int lastOrdered;

    /**
     * Maps total aOrder seqence number to a unordered message.
     */
    protected Map aOrder;

    /**
     * HashTable of message that has not order yet.
     */
    protected Map aUnordered;

    /**
     * Variable indicating last delivered message.
     */
    protected int lastDelivered;

    /**
     * Keeps ids of message that were delivered per sender.
     */
    protected CompressedIntSet[] deliveredPerMember;

    /**
     * Set of received messages, sorted by sequence number.
     */
    protected SortedMap received;

    /**
     * Mapping of sequence numbers to send.
     */
    protected Map sequenceNumbers;

    /**
     * Array containing the IDs of processes currently listed in the processes.
     */
    protected int[] processes;

    /**
     * Current session ID.
     */
    protected int sessionId;

    /**
     * Messages that will be handled after a new view will be installed.
     */
    protected List bufferedMessages = new LinkedList();;

    /**
     * Maximal number of messages that could be stored simultaneously.
     */
    protected int maxBufferSize;

    /**
     * Pointer to stable layer in order archive better performance.
     */
    protected ReceiverInterface stableLayer;

    /**
     * The ID of the sequencer.
     */
    protected int sequencer;
    protected int[] toSequencer;

    /*
     * Message IDs local to the algorithm.
     */

    // FIXME: when these constants are no longer needed,
    // remember to remove them from the MemberName CheckStyle rule
    protected final int GET_SEQNUM = getMessageTypeGetSeqNum();
    protected final int SEND_SEQNUM = getMessageTypeSendSeqNum();
    protected final int ORDER_SEQNUM = getMessageTypeOrderSeqNum();
    protected final int SINGLE_SEQNUM = getMessageTypeSingleSeqNum();

    protected int getMessageTypeGetSeqNum() { return NU_GET_SEQNUM; }
    protected int getMessageTypeSendSeqNum() { return NU_SEND_SEQNUM; }
    protected int getMessageTypeOrderSeqNum() { return NU_ORDER_SEQNUM; }
    protected int getMessageTypeSingleSeqNum() { return NU_SINGLE_SEQNUM; }

    public static final int NU_GET_SEQNUM = 546;
    public static final int NU_SEND_SEQNUM = 547;
    public static final int NU_ORDER_SEQNUM = 2238;
    public static final int NU_SINGLE_SEQNUM = 2240;

    static {
        MessageTypes.instance().register(NU_GET_SEQNUM, "GET_SEQNUM");
        MessageTypes.instance().register(NU_SEND_SEQNUM, "SEND_SEQNUM");
        MessageTypes.instance().register(NU_ORDER_SEQNUM, "ORDER_SEQNUM");
        MessageTypes.instance().register(NU_SINGLE_SEQNUM, "SINGLE_SEQNUM");
    }

    // dummy Object for synchronization
    private Object lock = new Object();

    /* CONSTRUCTOR ----------------------------------------*/

    protected NekoProcess process;

    public FixedSequencerNU(NekoProcess process) {

        this.process = process;

        // install a processes of all members
        processes = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            processes[i] = i;
        }

        reset(processes, 0);

        Configurations config = NekoSystem.instance().getConfig();
        maxBufferSize =
            config.getInteger("FixedSequencerNU.buffer.max.size");

    } // end constructor

    protected SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    protected ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    /* METHODS -----------------------------------------------*/

    /* Inherited and overridden methods */

    public void send(NekoMessage m) {
        if (!enabled) {
            logger.info("FixedSequencerNU is disabled"
                        + " message discarded " + m);
            return;
        }
        if (m.getSource() < 0) {
            m.setSource(process.getID());
        }

        UnorderedContent c;
        synchronized (lock) {
            localSeqNum++;
            c = new UnorderedContent(m, process.getID(),
                                     localSeqNum,
                                     sessionId);
        }

        NekoMessage m1 = new NekoMessage(processes, getId(), c, GET_SEQNUM);
        logger.log(Level.FINER, "send {0}", m1);
        sender.send(m1);

    } // end send


    public void deliver(NekoMessage m) {

        logger.log(Level.FINE, "deliver {0}", m);
        if (m.getType() == GET_SEQNUM) {
            handleGetSeqNum(m);
        } else if (m.getType() == ORDER_SEQNUM) {
            handleOrderSeqNum(m);
        } else if (m.getType() == SEND_SEQNUM) {
            handleSendSeqNum(m);
        } else {
            throw new UnexpectedMessageException(m);
        } // end if

    } // end deliver


    public void setStableLayer(ReceiverInterface stableLayer) {
        this.stableLayer = stableLayer;
    }

    protected NekoMessage newOrderedMessage(Integer aSeqNum,
                                            UnorderedContent c)
    {
        synchronized (lock) {
            if (aOrder.remove(aSeqNum) == null) {
                throw new RuntimeException
                    ("Message:" + aSeqNum + "Has not been found in order set");
            }
            if (aUnordered.remove(c) == null) {
                throw new RuntimeException
                    ("Message:" + c + "Has not been found in unorder set");
            }
        }
        // The message will be received back and also stored by
        // STABILITY layer.
        OrderedContent c1 = new OrderedContent(aSeqNum.intValue(), c);

        NekoMessage m = new
            NekoMessage(new int[] { process.getID() },
                        getId(), c1, SINGLE_SEQNUM);
        //logger.finest("A new aOrdered message " + c1);

        return m;
    }

    protected void arrangeMessages() {
        List ll = new LinkedList();
        synchronized (lock) {
            while (true) {
                Integer n = new Integer(lastOrdered);
                UnorderedContent c = (UnorderedContent) aOrder.get(n);
                if (c == null) {
                    break;
                }
                UnorderedContent c1 = (UnorderedContent) aUnordered.get(c);
                if (c1 == null) {
                    logger.info("WE have seq. num of a message,  "
                                + "but do not (still) have "
                                + "the message itself.");
                    break;
                }
                // Ok, we have a new ordered messages.
                ll.add(newOrderedMessage(n, c1));
                lastOrdered++;
            }
        } // synchronized
        if (ll.size() > 0) {
            NekoMessage m = new
                NekoMessage(new int[] { process.getID() },
                            getId(), ll, SEND_SEQNUM);
            stableLayer.deliver(m);
        }
    }

    protected void doDelivery() {
        synchronized (lock) {
            Iterator it = received.values().iterator();
            while (it.hasNext()) {
                NekoMessage m = (NekoMessage) it.next();
                OrderedContent c = (OrderedContent) m.getContent();
                if (c.getSeqNum() != lastDelivered + 1
                    || !isDeliverable(c.getSeqNum()))
                {
                    break;
                }
                receiver.deliver(c.getMessage());
                UnorderedContent uc =
                    (UnorderedContent) c.getUnorderedContent();
                CompressedIntSet st = deliveredPerMember[uc.getSourceId()];
                assert (!st.contains(uc.getSeqNum()));
                st.add(uc.getSeqNum());
                lastDelivered++;
                it.remove();
            }
        }
    }

    public void reset(int[] newProcesses, int newSessionId) {
        synchronized (lock) {
            sessionId = newSessionId;
            processes = (int[]) newProcesses.clone();
            setSequencer();
            seqNum = -1;
            lastDelivered = -1;
            deliveredPerMember = new CompressedIntSet[process.getN()];
            int i;
            for (i = 0; i < process.getN(); i++) {
                deliveredPerMember[i] = new CompressedIntSet();
            }
            received = new TreeMap();
            lastOrdered = 0;
            sequenceNumbers = new LinkedHashMap();
            aUnordered = new LinkedHashMap();
            aOrder = new LinkedHashMap();
            enable();
            processBufferedMessages();
        }
    }

    protected int getSequencer() {
        int minId;
        // Sequencer.is a member of the processes with minimal id.
        if (processes.length == 0) {
            minId = -1;
        } else {
            minId = processes[0];
            for (int i = 1; i < processes.length; i++) {
                if (processes[i] < minId) {
                    minId = processes[i];
                }
            }
        }
        return minId;
    }

    protected void setSequencer() {
        sequencer = getSequencer();
        logger.fine("Sequencer is: " + sequencer);
        toSequencer = new int[] { sequencer };

        isSequencer = (process.getID() == sequencer);
    }

    protected void handleGetSeqNum(NekoMessage m) {
        UnorderedContent c = (UnorderedContent) m.getContent();
        synchronized (lock) {
            if (c.getSessionId() < sessionId) {
                logger.fine("A message with old sessionId is discarded:" + m);
                return;
            }

            if (c.getSessionId() > sessionId || !enabled) {
                logger.fine("Order message is buffered");
                bufferedMessages.add(m);
                return;
            }
            //logger.finest("GET_SEQNUM message received " + c);
            aUnordered.put(c, c); // Content gives the value for itself
            if (aUnordered.size() > maxBufferSize) {
                throw new RuntimeException
                    ("Amount of messages to store is too big");
            }
        } // synchronized

        if (isSequencer) {
            synchronized (lock) {
                UnorderedContent c1 =
                    new UnorderedContent(null, c.getSourceId(),
                                         c.getSeqNum(), sessionId);
                seqNum++;
                sequenceNumbers.put(new Integer(seqNum), c1);
                sendSeqNumbers();
            } // synchronized
        } else {
            arrangeMessages();
        }
    }

    protected void sendSeqNumbers() {
        Map toSend;
        synchronized (lock) {
            toSend = sequenceNumbers;
            sequenceNumbers = new LinkedHashMap();
        } // synchronized
        NekoMessage m = new NekoMessage(processes, getId(),
                                        toSend, ORDER_SEQNUM);
        sender.send(m);
    }

    protected void handleOrderSeqNum(NekoMessage m) {
        Map mp = (Map) m.getContent();
        Iterator it = mp.values().iterator();
        UnorderedContent c = (UnorderedContent) it.next();
        synchronized (lock) {
            if (c.getSessionId() < sessionId) {
                logger.fine("Order message with old sessionId is discarded");
                return;
            }
            if (c.getSessionId() > sessionId || !enabled) {
                logger.fine("Order message is buffered");
                bufferedMessages.add(m);
                return;
            }
            aOrder.putAll(mp);
            if (aOrder.size() > maxBufferSize) {
                throw new RuntimeException
                    ("Amount of messages to store is too big");
            }
            arrangeMessages();
        }
    }

    // returns true only if the message has correct sessionId
    protected boolean handleSendSeqNum(NekoMessage m) {
        List ll = (List) m.getContent();
        Iterator it = ll.iterator();
        NekoMessage m1 = (NekoMessage) it.next();
        OrderedContent c = (OrderedContent) m1.getContent();

        if (c.getSessionId() < sessionId) {
            logger.fine("A message with old sessionId is discarded:" + m);
            return false;
        }
        if (c.getSessionId() > sessionId || !enabled) {
            logger.fine("Order message is buffered");
            bufferedMessages.add(m);
            return false;
        }

        while (true) {
            //logger.finest("SEND_SEQNUM message received " + c);
            synchronized (lock) {

                received.put(new Integer(c.getSeqNum()), m1);
            } // synchronized

            if (!it.hasNext()) {
                break;
            }
            m1 = (NekoMessage) it.next();
            c = (OrderedContent) m1.getContent();
        }

        doDelivery();
        return true;
    }

    /**
     * When we agree opon an new view all messages that were suspended before
     * are processed.
     */
    protected void processBufferedMessages() {
        // see !!!! consensusCTEx..

        Iterator it;
        synchronized (lock) {
            it = bufferedMessages.iterator();
            bufferedMessages = new LinkedList();
        }

        while (it.hasNext()) {
            deliver((NekoMessage) (it.next()));
        }
    }

    public void disable() {
        enabled = false;
    }

    public void enable() {
        enabled = true;
    }

    protected boolean isDeliverable(int aSeqNum) {
        return true;
    }


    /* Required Utility functions and classes */

    /**
     * Container class for algorithmic data to be exchanged over the network.
     * Content contains message and sequence number.
     * @author Arindam Chakraborty
     */
    public static class UnorderedContent
        implements Serializable, Comparable
    {

        private NekoMessage message;
        private int sourceId;
        private int seqNum;
        private int sessionId;

        public UnorderedContent(NekoMessage m, int sourceId,
                       int seqNum, int sessionId)
        {
            this.message = m;
            this.sourceId = sourceId;
            this.seqNum = seqNum;
            this.sessionId = sessionId;
        } // end constructor

        public NekoMessage getMessage() {
            return message;
        } // end getMessage

        public int getSourceId() {
            return sourceId;
        } // end getSeqNum

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public int getSessionId() {
            return sessionId;
        } // getSessionId

        public int compareTo(Object e) {
            UnorderedContent other = (UnorderedContent) e;
            if (sourceId == other.getSourceId()) {
                if (seqNum == other.getSeqNum()) {
                    return 0;
                } else {
                    return (seqNum < other.getSeqNum()) ? -1 : +1;
                }
            } else {
                return (sourceId < other.getSourceId()) ? -1 : +1;
            }
        } // end compareTo

        public int hashCode() {
            return seqNum + sourceId * ((2 << 16) - 1);
        }

        public boolean equals(Object c) {
             return (compareTo(c) == 0);
        }

        public String toString() {

            String s = "sourceId " + sourceId + " seqNum " + seqNum
                + " sessionId " + sessionId;
            if (message != null) {
                return s +  " message " + message;
            }
            return s;
        } // end toString()

    } // end class UnorderedContent

    /**
     * Container class for algorithmic data to be exchanged over the network.
     * Content contains message and sequence number.
     * @author Arindam Chakraborty
     */
    public static class OrderedContent
        implements Serializable, Comparable
    {
        private UnorderedContent unordered;
        private int seqNum;

        public OrderedContent(int seqNum, UnorderedContent unordered) {
            this.seqNum = seqNum;
            this.unordered = unordered;

        } // end constructor

        public NekoMessage getMessage() {
            return unordered.getMessage();
        } // end getMessage

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public UnorderedContent getUnorderedContent() {
            return unordered;
        } // end getSeqNum

        public int getSessionId() {
            return unordered.getSessionId();
        } // getSessionId

        public int compareTo(Object e) {
            OrderedContent other = (OrderedContent) e;
            if (seqNum == other.getSeqNum()) {
                return 0;
            }
            return (seqNum < other.getSeqNum()) ? -1 : +1;
        } // end compareTo

        public int hashCode() {
            return seqNum;
        }

        public boolean equals(Object c) {
             return (compareTo(c) == 0);
        }

        public String toString() {
            String s = " Total order seqNum: " + seqNum + " " + unordered;
            return s;
        } // end toString()

    } // end class OrderedContent

    private static final Logger logger =
        NekoLogger.getLogger(FixedSequencerNU.class.getName());

} // end class FixedSequencerNU
