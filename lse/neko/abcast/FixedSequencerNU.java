package lse.neko.abcast;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.logging.NekoLogger;


/**
 * Implementation of the non-uniform fixed sequencer algorithm.
 *
 * <p>Status = working version :<br>
 * 1) the sequencer is also a part of the destination<br>
 * processes and not a separate sequencer.<br>
 * 2) the destination group is constant and includes all.</p>
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
     * True if the process is the sequencer.
     */
    protected boolean isSequencer;

    /**
     * Sequence number common for all sources.
     */
    protected int seqNum;

    /**
     * Variable indicating last delivered message.
     */
    protected int lastDelivered;

    /**
     * Set of received messages, sorted by sequence number.
     */
    protected SortedSet received;

    /**
     * Array containing the process IDs of all destinations.
     */
    protected int[] processes;

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

    protected int getMessageTypeGetSeqNum() { return NU_GET_SEQNUM; }
    protected int getMessageTypeSendSeqNum() { return NU_SEND_SEQNUM; }

    public static final int NU_GET_SEQNUM = 546;
    public static final int NU_SEND_SEQNUM = 547;

    static {
        MessageTypes.instance().register(NU_GET_SEQNUM, "GET_SEQNUM");
        MessageTypes.instance().register(NU_SEND_SEQNUM, "SEND_SEQNUM");
    }

    /* CONSTRUCTOR ----------------------------------------*/

    protected NekoProcess process;

    public FixedSequencerNU(NekoProcess process) {

        this.process = process;

        processes = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            processes[i] = i;
        }

        setSequencer();

        if (isSequencer) {
            seqNum = -1;
        } else {
            seqNum = Integer.MAX_VALUE;
        }
        lastDelivered = -1;
        received = new TreeSet();

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

        if (m.getSource() < 0) {
            m.setSource(process.getID());
        }

        if (isSequencer) {
            seqNum++;
            Content c = new Content(m, seqNum);
            NekoMessage m1 =
                new NekoMessage(processes, getId(), c, SEND_SEQNUM);
            logger.log(Level.FINE, "send: sending {0}", m1);
            sender.send(m1);
        } else {
            NekoMessage m2 = new NekoMessage(toSequencer,
                                             getId(), m, GET_SEQNUM);
            logger.log(Level.FINE, "send: sending {0}", m2);
            sender.send(m2);
        } // end if

    } // end send

    public synchronized void deliver(NekoMessage m) {

        logger.log(Level.FINE, "deliver {0}", m);
        if (m.getType() == GET_SEQNUM) {
            handleGetSeqNum(m);
        } else if (m.getType() == SEND_SEQNUM) {
            handleSendSeqNum(m);
        } else {
            throw new UnexpectedMessageException(m);
        } // end if

    } // end deliver

    protected void doDelivery() {
        Iterator it = received.iterator();
        while (it.hasNext()) {
            Content c = (Content) it.next();
            if (c.getSeqNum() != lastDelivered + 1
                || !isDeliverable(c.getSeqNum()))
            {
                break;
            }
            receiver.deliver(c.getMessage());
            lastDelivered++;
            it.remove();
        }
    }

    protected boolean isDeliverable(int someSeqNum) {
        return true;
    }

    protected int getSequencer() {
        int minId;
        // Sequencer.is a member of the processes with minimal id.
        if (processes.length == 0) {
            minId = -1;
            // FIXME: I doubt that processes.length can be 0
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

        if (!isSequencer) {
            throw new UnexpectedMessageException(m);
        }
        NekoMessage newM = (NekoMessage) m.getContent();
        send(newM);

    }

    protected void handleSendSeqNum(NekoMessage m) {

        synchronized (this) {
            received.add(m.getContent());
            doDelivery();
        }

    }

    /* Required Utility functions and classes */

    /**
     * Container class for algorithmic data to be exchanged over the network.
     * Content contains message and sequence number
     * @author Arindam Chakraborty
     */
    public static class Content
        implements Serializable, Comparable
    {

        private NekoMessage message;
        private int seqNum;

        public Content(NekoMessage m, int seqNum) {
            this.message = m;
            this.seqNum = seqNum;
        } // end constructor

        public NekoMessage getMessage() {
            return message;
        } // end getMessage

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public int compareTo(Object e) {
            Content other = (Content) e;
            if (seqNum == other.getSeqNum()) {
                return 0;
            } else {
                return (seqNum < other.getSeqNum()) ? -1 : +1;
            }
        } // end compareTo

        public String toString() {
            return "seqNum " + seqNum + " message " + message;
        } // end toString()

    } // end class Content

    private static final Logger logger =
        NekoLogger.getLogger(FixedSequencerNU.class.getName());

} // end class FixedSequencerNU
