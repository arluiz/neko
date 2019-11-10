package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
// import lse.neko.layers.FixedSequencerNU;
// ambiguous with: lse.neko.abcast.FixedSequencerNU
import lse.neko.util.logging.NekoLogger;

public class VSWrapper
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface,
               MembershipChangeListener,
               CompletedStateTransferListener,
               NewViewListener
{

    /* VARIABLES -----------------------------------*/
    /**
     * Reference to implementation of Atomic Broadcast.
     */
    protected SenderInterface abcast;

    /**
     * This variable set to 'true' value
     * if a view has been installed and there is not a
     * change_membership message has been received after that.
     */
    protected boolean allowedToSend = true;

    /**
     * Messages that were not delivered yet.
     */
    protected Set notYetDelivered = new LinkedHashSet();

    /**
     * Sequence number of the last sent message.
     */
    protected int seqNum = -1;

    /**
     * Sequence number of first message that has/may been/be sent in
     * this view.
     */
    protected int firstMessageInThisView = 0;

    /**
     * View of the current group.
     */
    protected GroupView view;

    /**
     * True if there is no need to perform state transfer.
     */
    protected boolean stateTransferCompleted = true;

    /**
     * Id of the first View after a gap.
     */
    protected int firstAfterGap = -1;

    // dummy target: myself
    private int[] toMe;

    /**
     * Message IDs local to the algorithm.
     */
    public static final int VS_WRAPPED = 2222;

    static {
        MessageTypes.instance().register(VS_WRAPPED, "VS_WRAPPED");
    }

    private static final Logger logger =
        NekoLogger.getLogger(VSWrapper.class.getName());

    // dummy Object for synchronization
    private Object lock = new Object();

    /* CONSTRUCTOR ----------------------------------------*/

    private NekoProcess process;

    public VSWrapper(NekoProcess process, SenderInterface abcast) {
        this.process = process;
        this.abcast = abcast;
        toMe = new int[] {process.getID()};
        int[] all = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            all[i] = i;
        }
        view = new GroupView(0, all);

    } // end constructor

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    private AckMembershipChangeListener ackMembershipChangeListener;

    public void setAckMembershipChangeListener(AckMembershipChangeListener
                                               ackMembershipChangeListener)
    {
        this.ackMembershipChangeListener = ackMembershipChangeListener;
    }

    /* METHODS -----------------------------------------------*/

    // send notYetDelivered messages again.
    private void sendAgain() {
        Iterator it;
        synchronized (lock) {
            int viewId = view.getViewId();
            it = notYetDelivered.iterator();

            // it seems that we do not to copy notYetDelivered !!!
            // but meanwhile I remain it.
            List l = new LinkedList();
            while (it.hasNext()) {
                Content c = (Content) it.next();
                l.add(c);
            }
            it = l.iterator();

            while (it.hasNext()) {
                Content c = (Content) it.next();
                if (firstMessageInThisView <= c.getSeqNum()) {
                    // all messages after this (include this one)
                    // has been send it this view,
                    // there is no reason to send them again
                    return;
                }
                NekoMessage m1 = new NekoMessage(toMe, getId(), c, VS_WRAPPED);
                //logger.finest("remove: Message SeqNum: " + c.getSeqNum() +
                //             " has been resend to VSSequencerLayer");
                if (!allowedToSend) { // a new view is under way
                    break;
                }
                if (viewId != view.getViewId()) {
                    break; // new view has been istalled
                }
                if (leakyBucketSwitch != null) {
                    leakyBucketSwitch.setInsertIntoLeakyBucket(true);
                }
                abcast.send(m1);
                if (leakyBucketSwitch != null) {
                    leakyBucketSwitch.setInsertIntoLeakyBucket(false);
                }
            }
        } // synchronized
    }

    private LeakyBucketSwitch leakyBucketSwitch;

    public void setLeakyBucketSwitch(LeakyBucketSwitch leakyBucketSwitch) {
        this.leakyBucketSwitch = leakyBucketSwitch;
    }

    protected void handleWrappedMessage(Content c) {
        if (c.getSource() == process.getID()) {
            synchronized (lock) {
                if (!notYetDelivered.remove(c)) {
                    throw new RuntimeException
                        ("My message has not been found in"
                         + " notYetDelivered " + c);
                } else {
                    //logger.finest("A message has been removed " +
                    //                 "from notYetDelivered " + c);
                }
            }
        }

        //logger.finest("Message has been delived to application " +
        //               c.getMessage());
        //         logger.warning("Curent ViewID: " + view.getViewId() +
        //        " DELIVERED: " + c.getMessage());
        receiver.deliver(c.getMessage());
    }

    public void handleNewView (GroupView newView) {
        logger.info("Amount of undelivered messages is: "
                    + notYetDelivered.size());

        if (view.getMembersNumber() <= 1) {
            throw new RuntimeException
                ("This number of members is not allowed: "
                 + view.getMembersNumber());
        }

        if (this.view.getViewId() + 1 != newView.getViewId()) {
            logger.info("We have to retrive messages for views between "
                        + this.view.getViewId() +  "(exclusive)");
            stateTransferCompleted = false;
            firstAfterGap = newView.getViewId();
        } else {
            if (allowedToSend) {
                logger.warning("New View Received without prior "
                               + "Change Membership notification. "
                               + newView.getViewId());
                throw new RuntimeException
                    ("New View received without prior "
                     + "Change Membership notification. " + process.getID());

            }

        } // else

        firstMessageInThisView = seqNum + 1;
        this.view = newView;
        allowedToSend = true;

        // lets send again messages that has not been delivered yet
        if (stateTransferCompleted) {
            synchronized (lock) {
                sendAgain();
            }
        }


        // in real GCS application is to receive this message too.
        // super.deliver(m);
    }

    public void handleMembershipChange(int viewId) {
        if (viewId < view.getViewId()) {
            throw new IllegalArgumentException
                ("Wrong viewId number " + viewId);
        }

        synchronized (lock) {
            allowedToSend = false;
            // !!! may be later we will add undelivered messages to unstable
            // for optimisation (but,  pay attention we are sending Map,
            // and storing in Set
            Map mp = new LinkedHashMap();
            // put the viewId in the map
            mp.put(new Integer(-1), new Integer(viewId));
            logger.fine("Ack on membership change sent");
            ackMembershipChangeListener.ackMembershipChange(mp);
        }
    } // handleMembershipChange

    /* Inherited and overridden methods */

    public void send(NekoMessage m) {

        if (m.getSource() < 0) {
            m.setSource(process.getID());
        }

        Content c;
        synchronized (lock) {
            seqNum++;
            c = new Content(m, seqNum, process.getID());
        }
        NekoMessage m1 = new NekoMessage(toMe, getId(), c, VS_WRAPPED);

        synchronized (lock) {
            // We have to store the message till delivery
            notYetDelivered.add(c);
            //logger.finest("Message SeqNum: " + seqNum +
            //                 " added to notYetDeliverd");
        }

        if (allowedToSend) { // A view has been istalled
            //logger.finest("Message SeqNum: " + seqNum +
            //           " send to VSSequencerLayer");
            // FIXME: The message m1 is of type VS_WRAPPED,
            // and is handled by VSWrapper.deliver
            // and VSStateTransfer.deliver
            abcast.send(m1);
        }
    }

    public void deliver(NekoMessage m) {
        //logger.finest("Message received " + m);
        if (m.getType() == VS_WRAPPED) {
            handleWrappedMessage((Content) m.getContent());
        } else {
            throw new UnexpectedMessageException(m);
        } // end of switch
    } // end of deliver

    public void completedStateTransfer(int requestId) {
        if (requestId != firstAfterGap) {
            logger.warning("Received completedStateTransfer for an old gap");
            return;
        }
        stateTransferCompleted = true;
        if (allowedToSend) {
            sendAgain();
        }
    }

    public static class Content
        implements Serializable, Comparable
    {

        private NekoMessage message;
        private int seqNum;
        private int sourceId;

        public Content(NekoMessage m, int seqNum, int sourceId) {
            this.message = m;
            this.seqNum = seqNum;
            this.sourceId = sourceId;
        } // end constructor

        public NekoMessage getMessage() {
            return message;
        } // end getMessage

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public int getSource() {
            return sourceId;
        }

        public int compareTo(Object e) {
            Content other = (Content) e;
            if ((seqNum == other.getSeqNum())
                && (sourceId == other.getSource()))
            {
                return 0;
            } else {
                return 1;
            }
        } // end compareTo

        public int hashCode() {
            return seqNum + sourceId * ((2 << 16) - 1);
        }

        public boolean equals(Object c) {
            return (compareTo(c) == 0);
        }

        public String toString() {
            if (message != null) {
                return "seqNum " + seqNum + " sourceId " + sourceId
                    + " message " + message;
            } else {
                return "seqNum " + seqNum + " sourceId " + sourceId;
            }
        } // end toString()
    } // end of class Content
}


