package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;


public class VSStateTransfer
     extends StabilityLayer
    implements NewViewListener
{

    /* VARIABLES -----------------------------------*/
    /*
     * Sequence number of the last delireved message
     */
    protected int seqNum = -1;

    /**
     * View of the current group.
     */
    protected GroupView view;

    /**
     * We have messages from this View Id in suspendedMessages list;
     */

    /**
     * True if there is no need to perform state transfer.
     */
    protected boolean stateTransferCompleted = true;

    /**
     *  Keeps id of the member that have all messages, and we have asked him
     *  to send message that we have missed to us.
     */
    protected int partner = -1;

    /**
     * Keeps the id of the last send stateTranferRequest. I use
     * for this purpose viewId of the FIRST view after the LAST gap.
     */
    protected int currentRequestId = -1;

    /**
     * Keeps messages that has been delivered to upper layer.
     */
    protected Map messages = new LinkedHashMap();

    /**
     * Keeps message that have to be delivered when state transfer finishes.
     */
    protected List suspendedMessages = new LinkedList();

    /**
     * Messages that will be handled after a new view will be installed.
     */
    protected List bufferedMessages = new LinkedList();;

    // dummy target: myself
    private int[] toMe;

    protected int getMessageTypeSequentialAcknolegment() {
        return VS_ST_ACKNOWLEDGMENT;
    }

    /* If sendBunch set to true the state transfer is to be made by one
     * message.
     */
    protected boolean sendBunch;

    /**
     * Message IDs local to the algorithm.
     */
    private static final int VS_ST_ACKNOWLEDGMENT = 2230;
    private static final int VS_ST_REQUEST = 2231;
    private static final int VS_ST_MESSAGE = 2232;
    private static final int VS_ST_LAST_MESSAGE = 2233;

    private static final int VS_ST_BUNCH_MESSAGE = 2241;

    static {
        MessageTypes.instance().register(VS_ST_ACKNOWLEDGMENT,
                                         "VS_ST_ACKNOWLEDGMENT");
        MessageTypes.instance().register(VS_ST_REQUEST,
                                         "VS_ST_REQUEST");
        MessageTypes.instance().register(VS_ST_MESSAGE,
                                         "VS_ST_MESSAGE");
        MessageTypes.instance().register(VS_ST_LAST_MESSAGE,
                                         "VS_ST_LAST_MESSAGE");
        MessageTypes.instance().register(VS_ST_BUNCH_MESSAGE,
                                         "VS_ST_BUNCH_MESSAGE");
    }



    private static final Logger logger =
        NekoLogger.getLogger(VSStateTransfer.class.getName());

    // dummy Object for synchronization
    private Object lock = new Object();


    /* CONSTRUCTOR ----------------------------------------*/
    public VSStateTransfer(NekoProcess process, double maximalDelay) {
        super(process, null, maximalDelay);

        toMe = new int[] {process.getID()};
        int[] all = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            all[i] = i;
        }
        view = new GroupView(0, all);

        Configurations config = NekoSystem.instance().getConfig();
        // if this flag is true the state transfer is to be made by one
        // message
        sendBunch =
            config.getBoolean("VSStateTransfer.sendBunch");

    } // end constructor

    /* METHODS -----------------------------------------------*/

    protected void handleWrappedMessage(NekoMessage m) {
        synchronized (lock) {
            if (stateTransferCompleted) {
                seqNum++;
                Content c = new Content(m, seqNum, view.getViewId());
                if (!super.addMessageToDelivered(seqNum)) {
                    throw new RuntimeException
                        ("Internal Bug");
                }
                //logger.finest("Message #" + seqNum + " has been received");
                messages.put(new Integer(seqNum), c);
                receiver.deliver(m);
            } else {
                Content c = new Content(m, seqNum, view.getViewId());
                suspendedMessages.add(c);
            }
        }
    }

    protected void handleStateTranferRequestMessage(NekoMessage m) {
        RequestContent request = (RequestContent) m.getContent();
        logger.fine("Received request " + m);
        if (request.getViewId() > view.getViewId()) {
            if (request.getViewId() - 1 == view.getViewId()) {
                // I suppose I will receive a new view soon
                bufferedMessages.add(m);
                return;
            } else {
                throw new RuntimeException
                    ("I don't know about this viewId "
                     + request.getViewId() + " my view is: "
                     + view.getViewId());
            }
        }
        // I can not receive such request from myself
        assert (m.getSource() != process.getID());

        retransmitMessages(new int[] {m.getSource()},
                           request.getSeqNum(),
                           request.getViewId());
    }

    /**
     * Retransmits the messages to the dest.
     */
    protected void retransmitMessages(int[] dest,
                                      int fromSeqNum,
                                      int tillViewId)
    {
        int requestId = tillViewId;
        NekoMessage m;
        List ll = new LinkedList();
        for (int n = fromSeqNum; true; n++) {
            Content c;
            synchronized (lock) {
                c = (Content) messages.get(new Integer(n));
            } // synchronized

            if ((c == null) || (c.getViewId() >= tillViewId)) {
                // we do not need to send the next message,
                // but we have to notify our state transfer partner
                // that state transfer is completed.
                c = new Content(null, n, tillViewId);
                c.setRequestId(requestId);
                m = new NekoMessage(dest, getId(), c, VS_ST_LAST_MESSAGE);
                if (sendBunch) {
                    ll.add(m);
                    m = new NekoMessage(dest, getId(), ll, VS_ST_BUNCH_MESSAGE);
                }
                sender.send(m);
                break;
            }
            if (c.getMessage() == null) {
                throw new RuntimeException
                    ("Internal error Null message in messages! " + c);
            }

            c.setRequestId(requestId);
            m = new
                NekoMessage(dest, getId(), c, VS_ST_MESSAGE);
            logger.fine("Sending STATE Tranfer Messsage " + c.getSeqNum());
            if (sendBunch) {
                ll.add(m);
            } else {
                sender.send(m);
            }
        } // for
    }

    protected void handleBunchMessage(List ll) {
        Iterator it = ll.iterator();
        while (it.hasNext()) {
            deliver((NekoMessage) it.next());
        }
    }

    public void handleNewView (GroupView newView) {
        synchronized (lock) {
            if (this.view.getViewId() + 1 != newView.getViewId()) {
                assert (bufferedMessages.size() == 0);
                stateTransferCompleted = false;
                partner = -1; // we have to send new request
            }

            if (!stateTransferCompleted) {
                // Do we already have a partner?
                if (!newView.contains(partner)) {
                    // We have to start State transfer again
                    logger.info("Discard suspended Messages: "
                                + suspendedMessages.size());
                    suspendedMessages.clear();
                    int oldc = currentRequestId;
                    currentRequestId = newView.getViewId();
                    partner = newView.getProcesses()[0];
                    logger.fine("New View: " + newView);
                    if (partner == process.getID()) {
                        // I can not send such request to
                        throw new RuntimeException
                            ("I can not send the request to my self "
                             + " current View: " + view
                             + " new View " + newView
                             + " currentRequestId " + oldc);
                    }
                    RequestContent request =
                        new RequestContent(seqNum + 1,  currentRequestId);
                    NekoMessage m =
                        new NekoMessage(new int[] { partner },
                                        getId(),
                                        request,
                                        VS_ST_REQUEST);
                    sender.send(m);
                    logger.fine("Sending a new Request to: " + partner);
                }
            }

            this.view = newView;
        } // synchronized
        processBufferedMessages();

    }

    protected void finishStateTransfer(int requestId) {

        logger.fine("Finishing state transfer");
        synchronized (lock) {
            if (stateTransferCompleted) {
                throw new RuntimeException
                    ("unexpected call to finishStateTransfer");
            }
            stateTransferCompleted = true;
            Iterator it = suspendedMessages.iterator();
            while (it.hasNext()) {
                seqNum++;
                Content c = (Content) it.next();
                // I have to fix seqNum of this content
                Content newContent = new Content(c.getMessage(),
                                                 seqNum,
                                                 c.getViewId());
                if (!super.addMessageToDelivered(seqNum)) {
                    throw new RuntimeException
                        ("Internal Bug");
                }
                messages.put(new Integer(seqNum), newContent);
                receiver.deliver(c.getMessage()); // XXX: was super.deliver
            }
            suspendedMessages.clear();
            stateTransferCompleted = true;

            completedStateTransferListener.completedStateTransfer(requestId);
        } // synchronized
    }

    private CompletedStateTransferListener completedStateTransferListener;

    public void
    setCompletedStateTransferListener(CompletedStateTransferListener
                                          completedStateTransferListener)
    {
        this.completedStateTransferListener = completedStateTransferListener;
    }

    protected void handleLastStateTransferMessage(NekoMessage m) {
        Content c = (Content) m.getContent();
        if (c.getRequestId() != currentRequestId) {
            logger.fine("I am discarding this message "
                        + "absolete stateTranferRequestId" + m);
            return;
        }
        logger.fine("Received last message of state transfer" + m);
        handleStateTransferMessage(m);
    }



    protected void handleStateTransferMessage(NekoMessage m) {

        Content c = (Content) m.getContent();
        logger.fine("State transfer message is received "
                    + c.getSeqNum());
        synchronized (lock) {
            if (stateTransferCompleted) {
                logger.warning("unexpected State Transfer Message" + m
                               + " current seqNum is: " + seqNum);
                return;
            }
            logger.fine("current seqNum is: " + seqNum);
            int sn = c.getSeqNum();
            if (sn <= seqNum) {
                logger.fine("It was a known message so nothing to do");
                return;
            }

            if (messages.put(new Integer(sn), c) != null) {
                logger.fine("It was an known message so nothing to do");
                return;
            }

            // let's deliver all deliverable messages.
            while ((c = (Content) messages.get(new Integer(seqNum + 1)))
                   != null)
            {
                if (c.getMessage() != null) {
                    seqNum++;
                    if (!super.addMessageToDelivered(seqNum)) {
                        throw new RuntimeException
                            ("Internal Bug");
                    }
                    receiver.deliver(c.getMessage());
                } else {
                    messages.remove(new Integer(seqNum + 1));
                    if (c.getRequestId() == currentRequestId) {
                        logger.fine("Finishing state transfer: " + c);
                        finishStateTransfer(currentRequestId);
                    } else {
                        logger.fine("I am discarding this message (it "
                                    + "was absolete stateTranferRequestId"
                                    + m);
                    }
                    break;
                }
            }
        } // synchronized

    } // handleStateTransferMessage

    public void deliver(NekoMessage m) {
        //logger.finest("Message received " + m);
        if (m.getType() == VSWrapper.VS_WRAPPED) { // improve this!!!
            handleWrappedMessage(m);
        } else if (m.getType() == VS_ST_REQUEST) {
            handleStateTranferRequestMessage(m);
        } else if (m.getType() == VS_ST_MESSAGE) {
            handleStateTransferMessage(m);
        } else if (m.getType() == VS_ST_LAST_MESSAGE) {
            handleLastStateTransferMessage(m);
        } else if (m.getType() == VS_ST_BUNCH_MESSAGE) {
            handleBunchMessage((LinkedList) m.getContent());
        } else {
            super.deliver(m); // definitely not receiver.deliver(m)
        } // end of switch
    } // end of deliver

    /**
     * This method is called when number of required acks has been
     * collected for at least one message.
     */
    protected void updateLimit(int newLimit, int prevLimitParam) {
        int prevLimit = prevLimitParam;
        while (prevLimit < newLimit) {
            if (messages.remove(new Integer(prevLimit)) == null) {
                throw new RuntimeException
                    ("Can not find message in messages set " + prevLimit);
            } else {
                //logger.finest("Message number: " + prevLimit +
                //            " removed from messages");
            }
            prevLimit++;
        }
    }

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

    public static class Content
        implements Serializable, Comparable
    {

        private NekoMessage message;
        private int seqNum;
        private int viewId;
        private int requestId = -1;

        public Content(NekoMessage m, int seqNum, int viewId) {
            this.message = m;
            this.seqNum = seqNum;
            this.viewId = viewId;
        } // end constructor

        public NekoMessage getMessage() {
            return message;
        } // end getMessage

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public int getRequestId() {
            return requestId;
        } // end getStateTranferRequestId

        public void setRequestId(int requestId) {
            this.requestId =  requestId;
        } // end setStateTranferRequestId

        public int getViewId() {
            return viewId;
        } // end getViewId

        public int compareTo(Object e) {
            Content other = (Content) e;
            if (seqNum == other.getSeqNum()) {
                return 0;
            }

            if (seqNum < other.getSeqNum()) {
                return -1;
            }

            return 1;
        }

        public String toString() {
            if (message != null) {
                return "seqNum " + seqNum + " viewId "
                    + viewId + " requestId " + requestId
                    + " message " + message;
            } else {
                return "seqNum " + seqNum + " viewId "
                    + viewId + " requestId " + requestId;
            }
        } // end toString()

    } // Content

    public static class RequestContent
        implements Serializable
    {

        private int seqNum;
        private int viewId;

        public RequestContent(int seqNum, int viewId) {
            this.seqNum = seqNum;
            this.viewId = viewId;
        } // end constructor

        public int getViewId() {
            return viewId;
        } // end getMessage

        public int getSeqNum() {
            return seqNum;
        } // end getSeqNum

        public String toString() {
            return "seqNum " + seqNum + " viewId " + viewId;
        } // end toString()
    } // end of class StateTranferRequestContent
}


