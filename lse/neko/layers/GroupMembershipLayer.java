package lse.neko.layers;

// java imports:
import java.util.Iterator;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.consensus.ConsensusInterface;
import lse.neko.failureDetectors.FailureDetectorInterface;
import lse.neko.util.logging.NekoLogger;


/**
 * Completes the implementation of the state machine of a group.
 *
 * <p>Status = working version :<br>
 * This module is responsible for integration Group State Machine
 * with layers above
 * @author Ilya Shnaiderman
 */
public class GroupMembershipLayer
    extends GroupMembershipState
{
    private int[] toMe; // dummy target to myself

    private static final Logger logger =
        NekoLogger.getLogger(GroupMembershipLayer.class.getName());

    // dummy Object for synchronization
    private Object lock = new Object();

    /**
     * Constructor.
     */
    public GroupMembershipLayer(NekoProcess process,
                                FailureDetectorInterface fd,
                                ConsensusInterface consensus)
    {
        super(process, fd, consensus);
        toMe = new int[] {process.getID()};
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    private MembershipChangeListener membershipChangeListener;

    public void setMembershipChangeListener(MembershipChangeListener
                                            membershipChangeListener)
    {
        this.membershipChangeListener = membershipChangeListener;
    }

    protected void notifyApplicationAboutMembershipChange(int viewId)  {

        synchronized (lock) {
            logger.fine("Membership change notification sent ViewId()"
                        + viewId);
            membershipChangeListener.handleMembershipChange(viewId);
        }
    }

    protected void installNewView(GroupView view, Iterator orderedMessages,
                                  Iterator unorderedMessages)
    {
        super.installNewView(view, orderedMessages, unorderedMessages);

        Iterator []its = new Iterator[2];
        its[0] = orderedMessages;
        its[1] = unorderedMessages;
        synchronized (lock) {
            messageFromConsensusListener.handleMessageFromConsensus(its);
            logger.fine("New View: " + view);
            newViewListener.handleNewView(view);
        } // synchronized
    } //installNewView

    private MessageFromConsensusListener messageFromConsensusListener;

    public void setMessageFromConsensusListener(MessageFromConsensusListener
                                                messageFromConsensusListener)
    {
        this.messageFromConsensusListener = messageFromConsensusListener;
    }

    private NewViewListener newViewListener;

    public void setNewViewListener(NewViewListener newViewListener) {
        this.newViewListener = newViewListener;
    }

} // GroupMembershipLayer
