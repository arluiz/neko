package lse.neko.abcast;

// java imports:
import java.util.Iterator;
import java.util.TreeSet;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.util.CompressedIntSet;
import lse.neko.util.Util;


/**
 * Implementation of the uniform privilege based algorithm.
 *
 * <p>Status = working version:<br>
 * as with the non-uniform version initialization messages<br>
 * require use of NekoThread.</p>
 *
 * <p>Algorithm details :<br>
 * Page 145 of "Agreement-Related Problems: From Semi-passive<br>
 * Replication To Totally Ordered Broadcast" Xavier Defago</p>
 *
 * @author Arindam Chakraborty
 */
public class PrivilegeBasedUSimple
    extends PrivilegeBasedNU
{

    /* VARIABLES -----------------------------------*/

    /**
     * Array of lists for the acknowledgments of the messages by each process.
     */
    protected int[] acks;

    protected CompressedIntSet localAcks = new CompressedIntSet();

    /**
     * The highest sequence number seen in messages so far.
     */
    protected int highestSeqNum = -1;

    /**
     * Message ID local to the algorithm.
     */
    protected static final int PBUTOKENMSG = 559;

    static {
        MessageTypes.instance().register(PBUTOKENMSG, "PBUTOKENMSG");
    }

    /* CONSTRUCTOR ----------------------------------------*/

    public PrivilegeBasedUSimple(NekoProcess process) {
        super(process);
        setName("privilegeUthread");

        acks = new int[process.getN()];

        // recvQ is now distinct from deliverQ, unline in PrivilegeBasedNU
        recvQ = new TreeSet();

    } // end constructor

    /* METHODS -----------------------------------------------*/

    /* Inherited and Overridden Methods */

    protected int getMessageType() {
        return PBUTOKENMSG;
    }

    protected void receiveMessage(Content content) {

        // update highestSeqNum
        int seqNum = content.getSeqNum();
        if (seqNum > highestSeqNum) {
            highestSeqNum = seqNum;
        }

        // update acks (all elements except acks[p])
        if (seqNum == highestSeqNum) {
            // but only if the information in the message is recent
            int[] acks2 = ((UContent) content).getAcks();
            for (int i = 0; i < acks.length; i++) {
                if (acks2[i] > acks[i]) {
                    acks[i] = acks2[i];
                }
            }
        } else {
            // otherwise, just make a copy, to work around
            // a "feature" of Java serialization
            // (the same object does not get serialized again,
            // not even if it changes)
            acks = (int[]) acks.clone();
        }

        // update acks[p]
        if (content.getMessage() != null) {
            localAcks.add(content.getSeqNum() - 1);
            acks[process.getID()] = localAcks.getFilled();
        }

        // detect stability
        Iterator it = recvQ.iterator();
        while (it.hasNext()) {
            Content c = (Content) it.next();
            if (belongsAll(c.getSeqNum() - 1)) {
                deliverQ.add(c);
                it.remove();
                // logger.fine("adding to deliverQ and removing from recvQ "
                //       + "for message " + c);
            } // end if
        } // end while
    }

    protected Content createContent(NekoMessage m,
                                    int seqNum,
                                    int tokenHolder)
    {
        if (m != null) {
            localAcks.add(seqNum - 1);
            acks[process.getID()] = localAcks.getFilled();
        }
        return new UContent(m, seqNum, tokenHolder, (int[]) acks.clone());
    }

    /* Required Functions and Classes */

    /**
     * Checks if the message has been acked by all processes.
     */
    private boolean belongsAll(int seqNum) {

        for (int i = 0; i < process.getN(); i++) {
            if (acks[i] <= seqNum) {
                return false;
            } // end if
        } // end for
        return true;

    } // end belongsAll

    /**
     * Content type for PBUTOKENMSG messages.
     * Element contains message, sequence number, the token holder's id and
     * the array of acknowledgement linked lists.
     * @author Arindam Chakraborty
     */
    public static class UContent extends Content {

        private int[] acks;

        public UContent(NekoMessage m,
                        int seqNum,
                        int tokenHolder,
                        int[] acks)
        {
            super(m, seqNum, tokenHolder);
            this.acks = acks;
        } // end constructor

        public int[] getAcks() {
            return acks;
        } // end getAcks

        public String toString() {
            return super.toString() + " " + Util.toString(acks);
        } // end toString

    } // end class UContent

} // end class PrivilegeBasedUSimple
