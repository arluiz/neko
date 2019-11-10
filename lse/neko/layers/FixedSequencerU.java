package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.CompressedIntSet;
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Implementation of the uniform fixed sequencer algorithm.
 *
 * <p>Status = working version :<br>
 * 1) Element class had to be changed to use it here (from the<br>
 * one used for Non-Uniform version.<br>
 * 2) deliver method has changed<br>
 * 3) 2 new message types are included<br>
 * 4) It seemed to me that the "received" set used in the algorithm<br>
 * specification was unnecessary and so I have commented out the<br>
 * appropriate lines below (28, 74, 117, 138, 139).</p>
 *
 * <p>Algorithm details :<br>
 * Page 153 of "Agreement-Related Problems: From Semi-passive<br>
 * Replication To Totally Ordered Broadcast" Xavier Defago</p>
 *
 * @author Arindam Chakraborty
 */
public class FixedSequencerU
    extends FixedSequencerNU
{
    /* VARIABLES -----------------------------------*/

    protected int stableSeqNum = -1;

    /**
     * Contains of set of message sequence numbers that has been received.
     */
    protected CompressedIntSet receivedSeqNums;

    /**
     * Contains Acks that have to be sent.
     */
    protected AcksContent acks = new AcksContent(0);

    /**
     * Map from sequence numbers (Integer) to AckCollector.
     */
    protected Map stabilityInfo;

    /**
     * Configurable parameters to limit number of acks to me sent in
     * one message. If acksDelay is true, new acks is to be sent only after all
     * the messages that were already acknowledged became stable.
     */
    protected boolean acksDelay = false;

    /**
     * Limits how many acks could be send in one message.
     */
    protected int acksLimit = 10000;

    /**
     * Configurable parameters to limit number of unordered message ID that may
     * be listed in one order message.
     * If orderSendDelay is true, new acks is to be sent only after all
     * the messages that were already ordered became stable
     */
    protected boolean orderSendDelay = false;

    /**
     * Limits how many message IDs could be send in one ORDER message.
     */
    protected int orderMaxSend = 10000;

    /**
     * Acknowledged message with maximal sequence number.
     */
    protected int waitingForSeqNum = -1;

    /**
     * True if we want uniform version of the algorithm.
     */
    protected boolean uniformVersion = true;

    /*
     * Message IDs local to the algorithm.
     */
    protected int getMessageTypeGetSeqNum() { return U_GET_SEQNUM; }
    protected int getMessageTypeSendSeqNum() { return U_SEND_SEQNUM; }
    protected int getMessageTypeOrderSeqNum() { return U_ORDER_SEQNUM; }

    public static final int U_GET_SEQNUM = 548;
    public static final int U_SEND_SEQNUM = 549;
    public static final int U_ORDER_SEQNUM = 2239;
    public static final int ACK_SEQNUM = 550;
    public static final int STABLE_SEQNUM = 551;

    static {
        MessageTypes.instance().register(U_GET_SEQNUM, "GET_SEQNUM");
        MessageTypes.instance().register(U_SEND_SEQNUM, "SEND_SEQNUM");
        MessageTypes.instance().register(U_ORDER_SEQNUM, "ORDER_SEQNUM");
        MessageTypes.instance().register(ACK_SEQNUM, "ACK_SEQNUM");
        MessageTypes.instance().register(STABLE_SEQNUM, "STABLE_SEQNUM");
    }

    // dummy Object for synchronization
    private Object lock = new Object();

    /* CONSTRUCTOR ----------------------------------------*/

    public FixedSequencerU(NekoProcess process) {
        super(process);
        acks = new AcksContent(0);
        Configurations config = NekoSystem.instance().getConfig();

        acksDelay = config.getBoolean("FixedSequencerU.acksDelay");
        acksLimit = (int) config.getDouble("FixedSequencerU.acksLimit");
        orderSendDelay = config.getBoolean("FixedSequencerU.orderSendDelay");
        orderMaxSend = (int) config.getDouble("FixedSequencerU.orderMaxSend");
    } // end constructor

    /* METHODS -----------------------------------------------*/

    /* Inherited and overridden methods */

    public void deliver(NekoMessage m) {

        logger.log(Level.FINE, "executing deliver {0}", m);
        if (m.getType() == ACK_SEQNUM) {
            handleAcks(m);
        } else if (m.getType() == STABLE_SEQNUM) {
            handleStableMessage((StableContent) m.getContent());
        } else {
            super.deliver(m);
        } // end switch

    } // end deliver

    protected boolean isDeliverable(int seqNum) {
        return (seqNum <= stableSeqNum) && (enabled);
    }

    protected int getNumberOfRequiredAcks() {
        return processes.length;
    }

    public void reset(int[] processes, int sessionId) {
        if (lock == null) {
            lock = new Object();
        }
        synchronized (lock) {
            logger.fine("current sessionId in process (" + process.getID()
                        + ") is " + sessionId);

            receivedSeqNums = new CompressedIntSet();
            stableSeqNum = -1;
            stabilityInfo = new LinkedHashMap();
            acks = new AcksContent(sessionId);
            acks.setSessionId(sessionId);
            super.reset(processes, sessionId);
        }
    }

    protected void sendSeqNumbers() {
        Map toSend;
        synchronized (lock) {
            if (sequenceNumbers.size() == 0) {
                return;
            }
            if (!orderSendDelay) {
                super.sendSeqNumbers();
                return;
            }

            Iterator it = sequenceNumbers.entrySet().iterator();
            Map.Entry entry = (Map.Entry) it.next();
            Integer seqNum = (Integer) entry.getKey();

            if (uniformVersion) {
                if (stableSeqNum < seqNum.intValue() - 1) {
                    // It seems that message ordered last time are not
                    // stable yet
                    return;
                }
            }

            toSend = new LinkedHashMap();
            do {
                toSend.put(entry.getKey(), entry.getValue());
                it.remove();
                if (!it.hasNext()) {
                    break;
                }
                entry = (Map.Entry) it.next();
            } while (toSend.size() < orderMaxSend);

        } // synchronized

        NekoMessage m = new NekoMessage(processes, getId(),
                                        toSend, ORDER_SEQNUM);
        sender.send(m);
    }

    protected boolean handleSendSeqNum(NekoMessage m) {
        if (!super.handleSendSeqNum(m)) {
            return false;
        }

        if (!uniformVersion) {
            return true;
        }

        NekoMessage m1 = null;
        List ll = (List) m.getContent();
        Iterator it = ll.iterator();
        synchronized (lock) {
            AcksContent acksToSend = new AcksContent(sessionId);
            while (it.hasNext()) {
                NekoMessage m2 = (NekoMessage) it.next();
                OrderedContent c = (OrderedContent) m2.getContent();
                Integer seqNum = new Integer(c.getSeqNum());
                if (!acksDelay) {
                    acks.add(seqNum);
                } else {
                    if (seqNum.intValue() >= waitingForSeqNum) {
                        acks.add(seqNum);
                    } else {
                        acksToSend.add(seqNum);
                    }
                }
            }

            if (acks.size() > maxBufferSize) {
                throw new RuntimeException
                    ("Amount of acks to be sent is too big");
            }
            if (!acksDelay) {
                m1 = buildAcknowledgment();
            } else if (waitingForSeqNum == -1) {
                m1 = buildAcknowledgment();
            } else { // we are blocked;
                if (acksToSend.size() > 0) {
                    // may be we are in deadlock now, so we should
                    // send the ack immediately
                    m1 = new NekoMessage(toSequencer, getId(),
                                         acksToSend, ACK_SEQNUM);
                }
            }
        } // synchronized

        if (m1 != null) {
            sender.send(m1);
        }
        return true;
    }

    private NekoMessage buildAcknowledgment() {
        synchronized (lock) {
            assert (acks.size() > 0);

            AcksContent acksToSend;
            if (acks.size() > acksLimit) {
                acksToSend = new AcksContent(sessionId);
                Iterator it = acks.iterator();
                while (acksToSend.size() < acksLimit && (it.hasNext())) {
                    Integer seqNum = (Integer) it.next();
                    it.remove();
                    if ((seqNum.intValue() > stableSeqNum)) {
                        acksToSend.add(seqNum);
                    }
                }
                if (acksToSend.isEmpty()) { //
                    logger.info("All messages in acks has been less that "
                                + "stable limit");
                    waitingForSeqNum = -1;
                    return null;
                }
            } else {
                acksToSend = acks;
                acks = new AcksContent(sessionId);
            }

            NekoMessage m = new NekoMessage(toSequencer,
                                            getId(),
                                            acksToSend,
                                            ACK_SEQNUM);
            //logger.finest("Sending Acks: " + acksToSend);
            waitingForSeqNum = ((Integer) acksToSend.last()).intValue();
            return m;
        } // synchronized
    }

    private void stableLimitIsUpdated() {
        sendSeqNumbers();
        doDelivery();
        if (waitingForSeqNum <= stableSeqNum) {
            if (acks.size() > 0) {
                NekoMessage m = buildAcknowledgment();
                if (m != null) {
                    sender.send(m);
                }
            } else {
                waitingForSeqNum = -1;
            }
        }
    }

    protected void handleStableMessage(StableContent c) {
        NekoMessage m = null;
        synchronized (lock) {
            if (c.getSessionId() != sessionId) {
                logger.info("Stable Message discarded " + m + " by "
                            + process.getID() + " current sessionId= "
                            + sessionId + " and message sessionId= "
                            + c.getSessionId());
                return;
            }

            if (c.getSeqNum() <= stableSeqNum) {
                return;
            }

            stableSeqNum = c.getSeqNum();
            stableLimitIsUpdated();
        } // synchronized

        if (m != null) {
            super.send(m);
        }
    }

    protected void handleAcks(NekoMessage m) {

        AcksContent  s = (AcksContent) m.getContent();
        StableContent cont = null;

        synchronized (lock) {
            if (!isSequencer) {
                throw new UnexpectedMessageException(m);
            }

            if (s.getSessionId() != sessionId) {
                logger.info("Ack Message discarded " + m + " by "
                            + process.getID());

                return;
            }
            Iterator it = s.iterator();
            while (it.hasNext()) {
                Integer n = (Integer) it.next();
                int seqNum = n.intValue();

                if (seqNum > stableSeqNum) {
                    AckCollector collector =
                        (AckCollector) stabilityInfo.get(n);
                    if (collector == null) {
                        collector =
                            new AckCollector(getNumberOfRequiredAcks());
                        stabilityInfo.put(n, collector);
                    }

                    collector.addAckFrom(m.getSource());
                }
            }

            int prevStable = stableSeqNum;

            while (true) {
                Integer n = new Integer(stableSeqNum + 1);
                AckCollector collector = (AckCollector) stabilityInfo.get(n);
                if (collector == null) {
                    break;
                }
                if (!collector.hasAllAcks()) {
                    break;
                }
                if (stabilityInfo.remove(n) == null) {
                    throw new RuntimeException
                        ("Internal bug");
                }
                stableSeqNum++;
            }

            if (prevStable < stableSeqNum) {
                // ok we have new stable limit
                //logger.finest("New Stable: " + stableSeqNum);
                cont = new StableContent(stableSeqNum, sessionId);
            }
        } // synchronized;

        if (cont != null) { // we have new stable limit to announce
            NekoMessage newM = new NekoMessage(processes, getId(),
                                               cont, STABLE_SEQNUM);
            sender.send(newM);
            stableLimitIsUpdated();
        }
    }

    /* Required Functions and Classes */

    protected class AckCollector {

        private boolean[] receivedAck;
        private int numMissingAck;

        public AckCollector() {
            receivedAck = new boolean[process.getN()];
            numMissingAck = process.getN();
        }

        public AckCollector(int requiredAcksNumber) {
            receivedAck = new boolean[process.getN()];
            this.numMissingAck = requiredAcksNumber;
        }

        public boolean hasAllAcks() {
            return numMissingAck == 0;
        }

        public void addAckFrom(int id) {
            if (id < 0 || id >= process.getN()) {
                throw new IllegalArgumentException("from " + id);
            }
            if (receivedAck[id]) {
                throw new RuntimeException("Unexpected ack from p" + id);
            }
            receivedAck[id] = true;
            numMissingAck--;
        }

    }


   /**
    * Container class for algorithmic data to be exchanged over the network.
    * Content contains message and sequence number.
    * @author Arindam Chakraborty
    */
    public static class StableContent
        implements Serializable
    {
        private int seqNum;
        private int sessionId;

        public StableContent(int seqNum, int sessionId) {
            this.seqNum = seqNum;
            this.sessionId = sessionId;
        } // end constructor

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public int getSessionId() {
            return sessionId;
        } // getSessionId

        public String toString() {
            String s = "seqNum " + seqNum + " sessionId " + sessionId;
            return s;
        } // end toString()
    } // end of class StableContent

    public static class AcksContent
        extends TreeSet
        implements Serializable
    {
        private int sessionId = -1;

        public AcksContent(int sessionId) {
            super();
            setSessionId(sessionId);
        }

        public void setSessionId(int sessionId) {
            clear();
            this.sessionId = sessionId;
        }

        public int getSessionId() {
            return sessionId;
        } // getSessionId

        public String toString() {
            return "Acks " + super.toString() + " sessionId "
                + sessionId;
        } // end toString()

    } // end class AcksContent

    private static final Logger logger =
        NekoLogger.getLogger(FixedSequencerU.class.getName());

} // end class FixedSequencerU
