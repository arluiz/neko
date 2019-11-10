package lse.neko.abcast;

// java imports:
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.ActiveReceiver;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.logging.NekoLogger;


/**
 * Implementation of the non-uniform privilege based algorithm.
 *
 * <p>Status = working version:<br>
 * 1) had to extend NekoThread as the initial token had to be<br>
 * sent during initialization which is not possible in the<br>
 * constructor as when the constructor is executed, the protocol<br>
 * stack is not in place. So have put the message sending in the<br>
 * run() method.</p>
 *
 * <p>Algorithm details :<br>
 * Page 143 of "Agreement-Related Problems: From Semi-passive<br>
 * Replication To Totally Ordered Broadcast" Xavier Defago</p>
 *
 * @author Arindam Chakraborty
 */
public class PrivilegeBasedNU
    extends ActiveReceiver
    implements SenderInterface
{

    /* VARIABLES -----------------------------------*/

    /**
     * List of messages to be sent.
     */
    protected final List sendQ;

    /**
     * List of messages received.
     */
    protected Collection recvQ;

    /**
     * List of messages to be delivered.
     */
    protected final Collection deliverQ;

    /**
     * Array containing the IDs of all processes.
     */
    protected final int[] all;

    /**
     * Array containing the ID of this process.
     */
    protected final int[] self;

    /**
     * Array containing the ID of the next member in the token ring.
     */
    protected final int[] next;

    /**
     * Variable indicating last delivered message.
     */
    protected int lastDelivered;

    /**
     * ID of the process which will receive the token next.
     */
    protected final int tokNext;

    /**
     * Message ID local to the algorithm.
     */
    protected static final int PBNUTOKENMSG = 558;

    static {
        MessageTypes.instance().register(PBNUTOKENMSG, "PBNUTOKENMSG");
    }

    protected int getMessageType() {
        return PBNUTOKENMSG;
    }

    /* CONSTRUCTOR ----------------------------------------*/

    public PrivilegeBasedNU(NekoProcess process) {

        super(process, "privilegeNUthread");
        tokNext = (process.getID() + 1) % process.getN();

        all = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            all[i] = i;
        }
        self = new int[] { process.getID() };
        next = new int[] { tokNext };

        sendQ = new LinkedList();
        deliverQ = new TreeSet();
        // there is no separate receive and deliver queue
        recvQ = deliverQ;

        lastDelivered = 0;

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

    /* Inherited and Overridden Methods */

    /**
     * Run method, used to generate the initial token.
     */
    public void run() {
        if (process.getID() == 0) {
            NekoMessage m1 = new NekoMessage(self,
                                             getId(),
                                             createContent(null, 0, 0),
                                             getMessageType());
            sender.send(m1);
        } // end if
    } // end run


    public synchronized void send(NekoMessage m) {
        logger.log(Level.FINE, "send {0}", m);
        m.setSource(process.getID());
        Arrays.sort(m.getDestinations());
        sendQ.add(m);
    } // end send


    public void deliver(NekoMessage m) {

        if (m.getType() == getMessageType()) {
            logger.log(Level.FINE, "deliver {0}", m);

            Content content = (Content) m.getContent();

            synchronized (this) {

                if (content.getMessage() != null) {
                    recvQ.add(m.getContent());
                } // end if

                receiveMessage(content);
                sendToken(content);
                deliverMessages();

            } // end synchronized

        } else {

            throw new UnexpectedMessageException(m);

        } // end if

    } // end deliver

    /* Required Functions and Classes */

    protected void receiveMessage(Content content) {
    }

    private void sendToken(Content content) {
        // send pending messages, if any
        // circulate token, if appropriate
        if (process.getID() == content.getTokenHolder()) {
            if (!sendQ.isEmpty()) {
                NekoMessage m1 = (NekoMessage) sendQ.remove(0);
                Content e = createContent(m1,
                                          content.getSeqNum() + 1,
                                          tokNext);
                //receiveMessage(e);
                //deliverMessages();
                NekoMessage m2 = new NekoMessage(all, getId(),
                                                 e, getMessageType());
                logger.log(Level.FINE, "sending to all {0}", m2);
                sender.send(m2);
            } else {
                Content e = createContent(null,
                                          content.getSeqNum(),
                                          tokNext);
                //receiveMessage(e);
                //deliverMessages();
                NekoMessage m3 =
                    new NekoMessage(next,
                                    getId(),
                                    e,
                                    getMessageType());
                logger.log(Level.FINE, "passing token {0}", m3);
                sender.send(m3);
            } // end if
        } // end if
    }

    private void deliverMessages() {
        // deliver messages that can be delivered
        Iterator it = deliverQ.iterator();
        while (it.hasNext()) {
            Content e1 = (Content) it.next();
            if (e1.getSeqNum() != lastDelivered + 1) {
                break;
            }
            it.remove();
            int[] dest = e1.getMessage().getDestinations();
            if (0 <= Arrays.binarySearch(dest, process.getID())) {
                logger.log(Level.FINE, "delivering {0}", e1.getMessage());
                receiver.deliver(e1.getMessage());
            }
            lastDelivered++;
        } // end while
        logger.fine("finished delivering");
    }

    /**
     * Factory method for Content objects.
     */
    protected Content createContent(NekoMessage m,
                                    int seqNum,
                                    int tokenHolder)
    {
        return new Content(m, seqNum, tokenHolder);
    }

    /**
     * Content type for the algorithm's messages.
     * Content contains message, sequence number and the token holder's id
     * @author Arindam Chakraborty
     */
    public static class Content implements Serializable, Comparable {

        private NekoMessage message;
        private int seqNum;
        private int tokenHolder;

        public Content(NekoMessage m,
                       int seqNum,
                       int tokenHolder)
        {
            this.message = m;
            this.seqNum = seqNum;
            this.tokenHolder = tokenHolder;
        } // end constructor

        public NekoMessage getMessage() {
            return message;
        } // end getMessage

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public int getTokenHolder() {
            return tokenHolder;
        } // end getTokenHolder

        public int compareTo(Object e) {
            Content other = (Content) e;
            if (seqNum == other.getSeqNum()) {
                return 0;
            } else {
                return (seqNum < other.getSeqNum()) ? -1 : +1;
            }
        } // end compareTo

        public String toString() {
            return "message " + message
                + " seqNum " + seqNum
                + " tokenHolder " + tokenHolder;
        } // end toString

    } // end class Content

    private static final Logger logger =
        NekoLogger.getLogger(PrivilegeBasedNU.class.getName());

} // end class PrivilegeBasedNU
