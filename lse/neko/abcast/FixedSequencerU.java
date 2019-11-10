package lse.neko.abcast;

// java imports:
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.CompressedIntSet;
import lse.neko.util.logging.NekoLogger;


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

    protected final CompressedIntSet stableSeqNums = new CompressedIntSet();

    /**
     * Map from sequence numbers (Integer) to AckCollector.
     */
    protected final Map stabilityInfo = new HashMap();

    /*
     * Message IDs local to the algorithm.
     */

    protected int getMessageTypeGetSeqNum() { return U_GET_SEQNUM; }
    protected int getMessageTypeSendSeqNum() { return U_SEND_SEQNUM; }

    public static final int U_GET_SEQNUM = 548;
    public static final int U_SEND_SEQNUM = 549;
    public static final int ACK_SEQNUM = 550;
    public static final int STABLE_SEQNUM = 551;

    static {
        MessageTypes.instance().register(U_GET_SEQNUM, "GET_SEQNUM");
        MessageTypes.instance().register(U_SEND_SEQNUM, "SEND_SEQNUM");
        MessageTypes.instance().register(ACK_SEQNUM, "ACK_SEQNUM");
        MessageTypes.instance().register(STABLE_SEQNUM, "STABLE_SEQNUM");
    }

    /* CONSTRUCTOR ----------------------------------------*/

    public FixedSequencerU(NekoProcess process) {
        super(process);
    } // end constructor

    /* METHODS -----------------------------------------------*/

    /* Inherited and overridden methods */

    public synchronized void deliver(NekoMessage m) {

        logger.log(Level.FINE, "executing deliver {0}", m);
        if (m.getType() == SEND_SEQNUM) {
            handleSendSeqNum(m);
        } else if (m.getType() == ACK_SEQNUM) {
            handleAcks(m);
        } else if (m.getType() == STABLE_SEQNUM) {
            int c = ((Integer) m.getContent()).intValue();
            handleStableMessage(c);
        } else {
            super.deliver(m);
        } // end switch

    } // end deliver

    protected boolean isDeliverable(int seqNum) {
        return stableSeqNums.contains(seqNum);
    }

    protected void handleSendSeqNum(NekoMessage m) {

        // send ack
        Content c = (Content) m.getContent();
        NekoMessage m1 = new NekoMessage(toSequencer,
                                         getId(),
                                         new Integer(c.getSeqNum()),
                                         ACK_SEQNUM);
        sender.send(m1);
        super.handleSendSeqNum(m);

    }

    protected void handleStableMessage(int c) {

        synchronized (this) {
            if (stableSeqNums.contains(c)) {
                throw new RuntimeException("Unexpected message: STABLE_SEQNUM"
                                           + c + " received twice");
            }
            stableSeqNums.add(c);
            doDelivery();
        }

    }

    protected void handleAcks(NekoMessage m) {

        if (!isSequencer) {
            throw new UnexpectedMessageException(m);
        }
        Integer c = (Integer) m.getContent();
        NekoMessage newM = null;
        synchronized (this) {
            AckCollector collector = (AckCollector) stabilityInfo.get(c);
            if (collector == null) {
                collector = new AckCollector();
                stabilityInfo.put(c, collector);
            }
            collector.addAckFrom(m.getSource());
            if (collector.hasAllAcks()) {
                stabilityInfo.remove(c);
                newM =
                    new NekoMessage(processes, getId(), c, STABLE_SEQNUM);
            }
        }
        if (newM != null) {
            sender.send(newM);
        }
        // FIXME: complete / but why?

    }

    /* Required Functions and Classes */

    private class AckCollector {

        private boolean[] receivedAck;
        private int numMissingAck;

        public AckCollector() {
            receivedAck = new boolean[process.getN()];
            numMissingAck = process.getN();
        }

        public boolean hasAllAcks() {
            return numMissingAck == 0;
        }

        public void addAckFrom(int id) {
            if (id < 0 || id >= process.getN()) {
                throw new IllegalArgumentException();
            }
            if (receivedAck[id]) {
                throw new RuntimeException("Unexpected ack from p" + id);
            }
            receivedAck[id] = true;
            numMissingAck--;
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(FixedSequencerU.class.getName());

} // end class FixedSequencerU
