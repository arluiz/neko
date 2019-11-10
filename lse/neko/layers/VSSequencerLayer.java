package lse.neko.layers;

// java imports:
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.util.CompressedIntSet;
import lse.neko.util.logging.NekoLogger;


/**
 * @author Shnayderman Ilya
 */
public class VSSequencerLayer
    extends FixedSequencerU
    implements SequencedMessageInterface, AckMembershipChangeListener,
               MessageFromConsensusListener, NewViewListener
{
    protected GroupView view;

    private static final Logger logger =
        NekoLogger.getLogger(VSSequencerLayer.class.getName());

    // dummy Object for synchronization
    private Object lock = new Object();

    /* CONSTRUCTOR ----------------------------------------*/

    public VSSequencerLayer(NekoProcess process) {
        super(process);

        int[] all = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            all[i] = i;
        }
        view = new GroupView(0, all);
        reset(view.getProcesses(), view.getViewId());
    } // end constructor

    private AckMembershipChangeListener ackMembershipChangeListener;

    public void setAckMembershipChangeListener(AckMembershipChangeListener
                                               ackMembershipChangeListener)
    {
        this.ackMembershipChangeListener = ackMembershipChangeListener;
    }

    /* METHODS -----------------------------------------------*/

    protected int getNumberOfRequiredAcks() {
        return (view.getMembersNumber() / 2 + 1);
    }

    protected int getSequencer() {
        if (view == null) {
            return super.getSequencer();
        }

        logger.fine("view: " + view + "Leader is " + view.getLeader());
        return (view.getLeader());
    }

    public void handleMessageFromConsensus(Iterator[] iterators) {
        int i;
        OrderedContent c;
        UnorderedContent uc;
        NekoMessage m = null;

        for (i = 0; i < 2; i++) {
            Iterator it = iterators[i];
            if (it == null) {
                continue;
            }
            synchronized (lock) { // in order to keep order of the messages
                while (it.hasNext()) {
                    // ??? May it is enough to put only content not
                    // Neko message?
                    if (i == 0) { // ordered messsages
                        m = (NekoMessage) it.next();
                        c = (OrderedContent) m.getContent();
                        if (c.getSeqNum() <= lastDelivered) {
                            logger.fine("Message listed in consensus decision"
                                        + " discarded " + c);
                            continue;
                        }
                        uc = (UnorderedContent) c.getUnorderedContent();
                    } else {
                        uc = (UnorderedContent) it.next();
                    }
                    CompressedIntSet st = deliveredPerMember[uc.getSourceId()];
                    if (st.contains(uc.getSeqNum())) {
                        assert (i == 1); // only unordered messages can be here
                        logger.fine("Message listed in consensus decision"
                                    + " discarded " + uc);
                        continue;
                    }
                    st.add(uc.getSeqNum());
                    logger.fine("Delivering## + " + uc);
                    receiver.deliver(uc.getMessage());
                    uc = null;
                }
            } // synchronized
        } // loop on iterators
    }

    /* Inherited and overridden methods */
    public void handleNewView(GroupView newView) {
        this.view = newView;
        logger.fine("Process (" + process.getID()
                    + " )installing new view: " + view.getViewId());
        reset(view.getProcesses(), view.getViewId());
    }

    public void ackMembershipChange(Map map) {
        synchronized (lock) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "ackMembershipChange {0}",
                           map.toString());
            }
            disable(); // disable sequencing and deliver
            // ??? may be we need to do it earlier: upon notification?
            map.putAll(received);
            // Change by Peter, 13/12/2002
            // Returning to the implementation in which GET_SEQNUM
            // messages are re-sent after the view change.
            // changed line:
            //map.put(new Integer(-2), aUnordered);
            map.put(new Integer(-2), new HashMap());
            // end of change
            logger.fine("Unordered Amount " + aUnordered.size());
            ackMembershipChangeListener.ackMembershipChange(map);
        }
    }

    public int getType() {
        return getMessageTypeSendSeqNum();
    }

    public  int getSeqNum(NekoMessage m) {
        if (m.getType() != SINGLE_SEQNUM) {
            throw new IllegalArgumentException
                ("Asked for sequence number of a message with unexpected type "
                 + m);
        }
        OrderedContent c = (OrderedContent) m.getContent();
        return (c.getSeqNum());
    }

    public int getSessionId(NekoMessage m) {
        if (m.getType() != getType()) {
            throw new IllegalArgumentException
                ("Asked for session number of a message with unexpected type "
                 + m);
        }
        List ll = (List) m.getContent();
        NekoMessage m1 = (NekoMessage) ll.get(0);
        OrderedContent c = (OrderedContent) m1.getContent();
        return c.getSessionId();
    }

    public Iterator iterator(NekoMessage m) {
        List ll = (List) m.getContent();
        return ll.iterator();
    }
} // end class SequencerLayer
