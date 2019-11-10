package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.consensus.ConsensusCT;
import lse.neko.consensus.ConsensusInterface;
import lse.neko.consensus.ConsensusValue;
import lse.neko.consensus.DecisionListener;
import lse.neko.failureDetectors.FailureDetectorInterface;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.util.logging.NekoLogger;


/**
 * Implementation of the state machine of a group.
 *
 * Note: If state transfer has not been completed, I do not propose any value
 * to the consesus. I am only waiting for decision.
 * @author Ilya Shnaiderman
 */
public abstract class GroupMembershipState
    extends ProtocolImpl
    implements ReceiverInterface, DecisionListener, FailureDetectorListener,
               AckMembershipChangeListener,
               CompletedStateTransferListener
{
    // I am waiting for at least one unstable message
    private static final int WAITING_FOR_UNSTABLE_MESSAGES = 0;
    // I am waiting for consensus decision
    private static final int WAITING_FOR_DECISION = 1;

    // operating mode, a view has been installed
    private static final int OPERATING = 2;
    // I was excluded from the View. I will try to Join
    private static final int TRYING_TO_JOIN = 3;

    /**
     * A refence for Failure Detector implementation.
     *
     */
    protected FailureDetectorInterface fd;

    /**
     * The current view: id and list of members.
     */
    protected GroupView view;

    /**
     * The current state of the protocols state machine.
     */
    protected int state = OPERATING;

    /**
     * Contains list of process that we are waiting either for
     * unstable message from or suspicion notification about them.
     */
    protected Set unstableNotificationDebtors;

    /**
     * Proposal for next consensus.
     */
    protected Proposal proposal = null;

    /**
     * Reference to consensus implementation.
     */
    protected ConsensusInterface consensus;

    /**
     * Messages that will be handled after a new view will be installed.
     */
    protected List bufferedMessages = new LinkedList();;

    /**
     * Number of the view where this process has been seen.
     */
    private int[] seenInTheView;

    /**
     * List of members that wish to join the group.
     */
    private List joiners = new LinkedList();;

    /**
     * True if there is no need to execute state transfer.
     */
    protected boolean stateTransferCompleted = true;

    /**
     * Id of the latest 'Forwarded View'.
     */
    protected int lastForwarderedViewId = -1;

    /**
     * Message IDs local to the algorithm.
     */
    private static final int UNSTABLE = 2227;
    private static final int JOIN_REQUEST = 2228;
    private static final int FORWARDED_VIEW = 2229;

    static {
        MessageTypes.instance().register(JOIN_REQUEST,
                                         "JOIN_REQUEST");
        MessageTypes.instance().register(UNSTABLE,
                                         "UNSTABLE");
        MessageTypes.instance().register(FORWARDED_VIEW,
                                         "FORWARDED_VIEW");
    }

    private static final Logger logger =
        NekoLogger.getLogger(GroupMembershipState.class.getName());

    // dummy Object for synchronization
    private Object lock = new Object();

    protected NekoProcess process;

    /**
     * Constructor. Requires a failure detector and a consensus
     * implementation.
     */
    public GroupMembershipState(NekoProcess process,
                                FailureDetectorInterface fd,
                                ConsensusInterface consensus)
    {
        this.process = process;

        this.fd = fd;
        this.consensus = consensus;

        int n = process.getN();
        int[] all = new int[n];
        seenInTheView = new int[n];

        for (int i = 0; i < n; i++) {
            seenInTheView[i] = 0;
            all[i] = i; // to send first UNSTABLE message to everyone
        }
        ((ConsensusCT) consensus).setGroup(all);

        view = new GroupView(0, all);

    } // constructor

    protected SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    /**
     * When we agree upon an new view, all messages that were suspended before
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
            Object o = it.next();
            if (o instanceof ConsensusValue) {
                ConsensusValue cv = (ConsensusValue) o;
                notifyDecision(cv.getNumber(), cv.getValue());
            } else {
                deliver((NekoMessage) o);
            }
        }
    }

    /**
     * Handles a request from a member to join the group.
     */
    protected boolean handleJoinRequest(int source, Integer viewId) {
        logger.fine("Joiner Request Received from " + source);
        if (state != OPERATING) {
            return true; // let's process the message later
        }

        int id = viewId.intValue();
        if (id >= view.getViewId()) {
            logger.info("This process " + source
                        + " asks to join mentioning yet unknown ViewId "
                        + viewId);
            return true; // it seems that this one was excluded from one of
            // the next views, we will try to include him later.
        }

        if (seenInTheView[source] > id) {
            logger.info("This process " + source
                        + " asks to join mentioning " + viewId
                        + " ,but I have seen it in more recent view:"
                        + seenInTheView[source]);
            return false; // and old request
        }

        synchronized (lock) {
            joiners.add(new Integer(source));
        }

        startMembershipChange();

        return false;
    }

    /**
     * When we suspect a member this method is called.
     */
    public void statusChange(boolean isSuspected, int p) {
        if (!isSuspected) {
            return;
        }
        Integer susp = new Integer(p);
        logger.fine("Received Suspicion notification about " + susp);
        synchronized (lock) {
            if (state == WAITING_FOR_UNSTABLE_MESSAGES) {
                unstableNotificationDebtors.remove(susp);
                if (unstableNotificationDebtors.size() == 0) {
                    startConsensus();
                }
            } else if (state == TRYING_TO_JOIN) {
                return; // nothing could be done
            } else if (state == WAITING_FOR_DECISION) {
                return; // let's wait for decision
            } else {
                assert state == OPERATING;
                if (view.contains(susp)) {
                    startMembershipChange();
                }
            }
        } // synchronized
    } // handleSuspicion

    // checks that this view is still in use and we trust
    // every member of the group
    private boolean doWeNeedStartMembshipChange(GroupView newView) {

        if (state != OPERATING) {
            return false;
        }

        if (joiners.size() > 0) {
            return true;
        }

        if (newView.getViewId() == view.getViewId()) {
            // We are still using the same view
            // Do we trust every member of this view?
            Iterator it = view.getProcessesIterator();
            while (it.hasNext()) {
                Integer p = (Integer) (it.next());
                if (fd.isSuspected(p.intValue())) {
                    return true;
                }
            }
            return false;
        } else {
            if (newView.getViewId() > view.getViewId()) {
                throw new RuntimeException
                    ("New View has not been istalled properly");
            }
        }

        // It seems that we have already moved to another round
        return false;
    }

    protected void unexpectedTransision(String error) {
        logger.warning("Unexpected transision current view: "
                       + view + "\nstate: " + getStateName(state)
                       + " stateTransferCompleted: " + stateTransferCompleted
                       + "\n" + error);
        throw new RuntimeException
            ("Unexpected transision " + error);

    }

    /**
     *  Handles a received decision from consensus layer.
     */
    public void notifyDecision(int consensusNumber, Object d) {

        // Consensus may be reached only if majority of members of the
        // previous View participated in it, so we have majority!

        logger.fine("Handling consensus number:" + consensusNumber);

        Proposal decision = (Proposal) d;

        if (consensusNumber <= view.getViewId()) {
            logger.warning(" Received decision with wrong Number"
                           + consensusNumber);
            return;
        }

        GroupView newView =
            new GroupView(view.getViewId() + 1, decision.getProcesses());
        boolean isInludesMe = newView.contains(process.getID());


        synchronized (lock) {
            if (state == OPERATING) {
                logger.fine("New View received " + newView);
                if (isInludesMe) {
                    throw new IllegalArgumentException
                        (" Received unexpected decision");
                } else {
                    startJoinProtocol();
                }
                return;
            }

            if (state == TRYING_TO_JOIN) {
                bufferedMessages.add(new ConsensusValue(consensusNumber, d));
                return;
            }

            if (consensusNumber > view.getViewId() + 1) {

                if (isInludesMe) {
                    unexpectedTransision("Unexpected decision "
                                         + "consensusNumber: "
                                         + consensusNumber);
                }
                bufferedMessages.add(new ConsensusValue(consensusNumber, d));
                return; // let's keep the message or latter
            }

            // now consensusNumber == view.getViewId()
            //  two states remained: WAITING_FOR_UNSTABLE_MESSAGES and
            // WAITING_FOR_DECISION
            if (state == WAITING_FOR_UNSTABLE_MESSAGES) {
                logger.fine("Propopsing null value to Consensus in order to"
                            + " advance consensusNumber");
                consensus.propose(null);
                setState(WAITING_FOR_DECISION);
            } else {
                if (proposal.getFullyUpdatedProcess() == -1) {
                    logger.fine("Propopsing null value to Consensus in order"
                                + "to advance consensusNumber");
                    consensus.propose(null);
                }
            }

            if (state != WAITING_FOR_DECISION) {
                throw new RuntimeException
                    ("Internal error! Unhandled case:" + getStateName(state));
            }

            if (!isInludesMe) {
                startJoinProtocol();
                return;
            }

            decision.sortOrderedMessages();
            logger.fine("New View passed to Install new VIEW " + newView);
            installNewView(newView,
                           decision.getOrderedMessages().values().iterator(),
                           decision.getUnorderedMessages().values().iterator());

            processBufferedMessages();

            if (doWeNeedStartMembshipChange(newView)) {
                startMembershipChange();
            }

        } // end synchronized
    }

    /**
     * Installs new View that process agreed on it.  Also delivers all
     * messages @messageIterator that have to be delivered in this
     * view. (before delivering the new view to the application)
     */
    protected void installNewView(GroupView newView,
                                  Iterator orderedMessages,
                                  Iterator unorderedMessages)
    {

        logger.fine("Installing new View " + newView);
        this.view = newView;

        logger.fine("Set Group: " + view
                    + " consensusNumber: " + (view.getViewId() + 1));
        ((ConsensusCT) consensus).setGroup(view.getProcesses());
        setState(OPERATING);

        Iterator it;
        synchronized (lock) {
            it = joiners.iterator();
            joiners = new LinkedList();
        }

        while (it.hasNext()) {
            Integer j = (Integer) it.next();
            if (newView.contains(j)) {
                int p = j.intValue();
                NekoMessage m = new NekoMessage(new int[] {p},
                                                getId(),
                                                newView,
                                                FORWARDED_VIEW);
                sender.send(m);
            } else {
                synchronized (lock) {
                    joiners.add(j);
                }
            }
        }

        int viewId =  newView.getViewId();
        Iterator itp = newView.getProcessesIterator();
        synchronized (lock) {
            while (itp.hasNext()) {
                Integer p = (Integer) itp.next();
                seenInTheView[p.intValue()] = viewId;
            }
        } // synchronized
    }

    /**
     * This method called we want to notify application about
     * membership changes.
     */
    protected abstract void notifyApplicationAboutMembershipChange(int viewId);

    /**
     * Handles unstable message.
     * Returns true if this message have to be stored and processed later.
     */
    protected boolean handleUnstableMsg(int source, Proposal prop) {

        Integer s = new Integer(source);

        logger.fine("Received unstable message from " + s);

        if (prop.getViewId() < view.getViewId()) {
            // an old unstable message
            logger.fine("Message discarded wrong old viewId " + prop);
            return false;
        }

        synchronized (lock) {
            if (state == TRYING_TO_JOIN) {
                return true;
            }

            if (state == OPERATING) {
                startMembershipChange();
            }

            if (prop.getViewId() > view.getViewId()) {
                logger.fine("We will use this message in the future: "
                            + prop.getViewId() + " " + view.getViewId());
                return true; // we will use this message in the future
            }

            if (state == WAITING_FOR_DECISION) {
                if (prop.containsProcess(s)
                    && (source != process.getID()))
                {
                    unexpectedTransision("unexpected unstable " + prop
                                         + "from: " + source);
                    throw new RuntimeException
                        ("Unstable message has been received twice from:"
                         + s + "by: " + process.getID());
                }

                if (fd.isSuspected(source)) {
                    logger.fine("Received a unstable message from a "
                                + "suspected member: " + s);
                } else {
                    logger.info("Received an unexpected "
                                + "(may be it was previously suspected)"
                                + "unstable message from:" + s);
                }
                return false;
            }

            if (!view.contains(s)) {
                throw new RuntimeException
                    ("received unstable message from not member of the view");
            }

            // So now state is WAITING_FOR_UNSTABLE_MESSAGES and
            // we are sure that message has correct viewId
            if (!unstableNotificationDebtors.remove(s)) {
                if (fd.isSuspected(source)) {
                    logger.fine("Received a unstable message from a "
                                + "suspected member: " + s);
                } else {
                    logger.info("Received a unexpected "
                                + "(may be=it was previously suspected)"
                                + "unstable message from:" + s);
                }
            }

            this.proposal.addProcess(s);
            this.proposal.addProposal(prop);

            logger.fine("Handling unstable message from " + s);

            // !!! possible optimisation to remove messages from proposal
            // if we know that they are stable
            if (unstableNotificationDebtors.size() == 0) {
                startConsensus();
            } else {
                logger.fine("Debtors " + unstableNotificationDebtors);
            }

        } // synchronized

        return false;
    }


    /**
     * Sends MEMBERSHIP_CHANGE message.
     */
    protected void startMembershipChange() {
        synchronized (lock) {
            logger.fine("Starting Membeship Change");
            if (state != OPERATING) {
                throw new RuntimeException
                    ("Called startMembershipChange, unexpectedly");
            }


            setState(WAITING_FOR_UNSTABLE_MESSAGES);

            unstableNotificationDebtors = new LinkedHashSet();

            Iterator it = view.getProcessesIterator();

            while (it.hasNext()) {
                Integer pr = (Integer) it.next();
                if (!fd.isSuspected(pr.intValue())) {
                    unstableNotificationDebtors.add(pr);
                }
            }

            proposal = new Proposal(view.getViewId());

            notifyApplicationAboutMembershipChange(view.getViewId());
        } // synchronized
    } // startMembershipChange

    /**
     * Sends 'UNSTABLE' message to every member of the group.
     */
    public void ackMembershipChange(Map unstable) {
        logger.fine("Ack on membership changes has been received");
        Integer viewId = (Integer) unstable.remove(new Integer(-1));
        if (viewId.intValue() !=  view.getViewId()) {
            logger.warning("Received an old ACK_MEMBERSHIP_CHANGE");
            return;
        }

        Proposal p = new Proposal(view.getViewId());

        if (stateTransferCompleted) {
            // I have all message I list my self as fullyUpdatedProcess
            p.setFullyUpdatedProcess(process.getID());
        }

        Map unorderedMessages = (Map) unstable.remove(new Integer(-2));
        p.addOrderedMessages(unstable);
        p.addUnorderedMessages(unorderedMessages);

        // now add new joiners to the proposal, does'n matter whose
        // proposal for Consensus will be adopted, if it includes me
        // (that means my unstable message has been received)
        // it will include those new joiners
        synchronized (lock) {
            Iterator it = joiners.iterator();
            while (it.hasNext()) {
                p.addProcess((Integer) (it.next()));
            }
        }

        NekoMessage m = new NekoMessage(view.getProcesses(),
                                        getId(),
                                        p,
                                        UNSTABLE);
        sender.send(m);
    }


    /**
     * Returns true iff more than half of processes
     * participated in previous view listed in current proposal.
     */
    private boolean isMajorityToContinue() {
        Iterator it = proposal.getSetOfProcesses().iterator();
        Integer p;
        int intersectionSize = 0;

        while (it.hasNext()) {
            p = (Integer) (it.next());
            if (view.contains(p)) {
                intersectionSize++;
            }
        }
        return (intersectionSize > (view.getMembersNumber() / 2));
    }

    /**
     * If state transfer completed propose a value to consensus,
     * anyway move the state to Waiting_For_Decision.
     */
    protected void startConsensus() {
        synchronized (lock) {
            if (!isMajorityToContinue()) {
                return; // we don't have majority of processes participated in
                        // the previous view.
            }
            if (proposal.getFullyUpdatedProcess() != -1) {
                logger.fine("PROPOSING a value to consensus! viewId: "
                            + proposal.viewId);
                consensus.propose(proposal);
            } else {
                logger.fine("NOT PROPOSING any value to consensus! viewId: "
                            + proposal.viewId);
                logger.fine("There is no fully updated process, "
                            + "can not propose, "
                            + "waiting for completion of state transfer");
            }


            logger.info("Number of unstable messages in "
                        + "consensus proposal is: "
                        + (proposal.getOrderedMessages().size()
                           + proposal.getUnorderedMessages().size()));
            setState(WAITING_FOR_DECISION);
        } // synchronized
    } // startConsensus




    /**
     * Returns name of the state.
     */
    protected static String getStateName(int state) {
        switch (state) {
        case WAITING_FOR_UNSTABLE_MESSAGES:
            return "WAITING_FOR_UNSTABLE_MESSAGES";
        case WAITING_FOR_DECISION:
            return "WAITING_FOR_DECISION";
        case OPERATING:
            return "OPERATING";
        case TRYING_TO_JOIN:
            return "TRYING_TO_JOIN";
        default:
            throw new RuntimeException
                ("Unknown state" + getStateName(state));
        }
    }

    /**
     * Sets the state of the Group State machine to toState.
     */
    protected void setState(int toState) {
        logger.fine("Process Number: " + process.getID()
                    + " changes state from: " + getStateName(state)
                    + " to " + getStateName(toState));
        state = toState;
    }


    /**
     * Sends JOIN REQUEST to everybody.
     */
    protected void startJoinProtocol() {
        synchronized (lock) {
            setState(TRYING_TO_JOIN);
        }

        int me = process.getID();
        int[] allButMe = new int[process.getN() - 1];
        for (int i = 0; i < allButMe.length; i++) {
            allButMe[i] = (i < me) ? i : i + 1;
        }
        NekoMessage m = new NekoMessage(allButMe,
                                        getId(),
                                        new Integer(view.getViewId()),
                                        JOIN_REQUEST);
        sender.send(m);

    }

    /**
     * Called whenever a View was forwarded to us.
     * A view forwarded to new comers to the group by every member
     * that continues from previous View to this one.
     */
    protected void handleForwardedView(int source, GroupView newView) {

        synchronized (view) {
            logger.fine("A forwarded view has been received "
                        + newView.getViewId());
            if (view.getViewId() >= newView.getViewId()) {
                if (view.getViewId() > newView.getViewId()) {
                    logger.info("Have received an old View" + view);
                } else {
                    logger.fine("Have received another copy"
                                + " of the same View.");
                }
                return;
            }
            if (state != TRYING_TO_JOIN) {
                unexpectedTransision("Forwarded view has been received "
                                     + "unexpectedly " + newView
                                     + " from: " + source);
            }
            logger.fine("setNextConsensusId: " + (newView.getViewId() + 1));
            ((ConsensusCT) consensus).setNextConsensusId(newView.getViewId()
                                                         + 1);
            // I can not be listed first in the View, I am not fully updated
            assert (newView.getProcesses()[0] != process.getID());
            stateTransferCompleted = false;
            lastForwarderedViewId = newView.getViewId();
            processBufferedMessages();
            installNewView(newView, null , null);
            if (doWeNeedStartMembshipChange(newView)) {
                startMembershipChange();
            }
        } // synchronized
    }

    /**
     * Sets stateTransferCompleted to true, and propose a value to consensus
     * if needed.
     */
    public void completedStateTransfer(int viewId) {
        synchronized (lock) {
            if (viewId != lastForwarderedViewId) {
                logger.warning("Received completedStateTransfer "
                               + "for an old Forwarded view");
                return;
            }
            if (stateTransferCompleted) {
                throw new RuntimeException
                    ("unexpected call to completedStateTransfer");
            }
            logger.fine("State Transfer Completed for requestId " + viewId);
            stateTransferCompleted = true;
            if (state == WAITING_FOR_UNSTABLE_MESSAGES) {
                proposal.setFullyUpdatedProcess(process.getID());
            }
            if (state == WAITING_FOR_DECISION) {
                if (proposal.getFullyUpdatedProcess() == -1) {
                    proposal.setFullyUpdatedProcess(process.getID());

                    logger.fine("State Transfer Completed, so I am PROPOSING"
                                + " to consensus viewId: "
                                + proposal.getViewId());
                    consensus.propose(proposal);
                }
            }
        } // synchronized
    }

    public void deliver(NekoMessage m) {
        //logger.finest("message received " + m);

        boolean bufferTheMessage = false;

        if (m.getType() == JOIN_REQUEST) {
            bufferTheMessage =
                handleJoinRequest(m.getSource(), (Integer) m.getContent());
        } else if (m.getType() == FORWARDED_VIEW) {
            handleForwardedView(m.getSource(), (GroupView) m.getContent());
        } else if (m.getType() == UNSTABLE) {
            bufferTheMessage =
                handleUnstableMsg(m.getSource(), (Proposal) m.getContent());
        } else {
            throw new UnexpectedMessageException(m);
        } // end of switch

        if (bufferTheMessage) {
            synchronized (lock) {
                bufferedMessages.add(m);
            }
        }

    } // end of deliver

    protected static class Proposal
        implements Serializable
    {
        /**
         * Identifier of the previous View.
         */
        protected int viewId;

        /**
         * Keeps all unstable messages.
         */
        Map unstableOrderedMessages;
        Map unstableUnorderedMessages;

        /**
         * Keeps list of members from which unstable notification has
         * been received, and also members that wish to join the group.
         */
        Set processes;

        /**
         *
         * Number of a process that have received all previois messages.
         */
        int fullyUpdatedProcess = -1;

        public Proposal(int viewId) {
            unstableOrderedMessages = null;
            unstableUnorderedMessages = null;
            processes = null;
            this.viewId = viewId;
        }

        public void addProcess(Integer p) {
            if (processes == null) {
                processes = new LinkedHashSet();
            }
            processes.add(p);
        }

        public void addOrderedMessages(Map m) {
            if (unstableOrderedMessages == null) {
                unstableOrderedMessages = new TreeMap();
            }
            unstableOrderedMessages.putAll(m);
        }


        public void addUnorderedMessages(Map m) {
            if (unstableUnorderedMessages == null) {
                unstableUnorderedMessages = new TreeMap();
            }
            unstableUnorderedMessages.putAll(m);
        }

        public void sortOrderedMessages() {
            SortedMap m = new TreeMap();

            Iterator it = unstableOrderedMessages.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                m.put(entry.getKey(), entry.getValue());
            }

            unstableOrderedMessages = m;

        }

        /**
         * Returns array of processes
         * If there is a fullyUpdatedProcess returns it is listed first.
         */
        public int[] getProcesses() {
            int[] p = new int[processes.size()];
            Iterator it = processes.iterator();
            int i = 0;

            if (fullyUpdatedProcess != -1) {
                if (!processes.contains(new Integer(fullyUpdatedProcess))) {
                    throw new RuntimeException
                        ("fullyUpdatedProcess: " + fullyUpdatedProcess
                         + " processes: " + processes);
                }
                p[i] = fullyUpdatedProcess;
                i++;
            }

            while (it.hasNext()) {
                int tmp = ((Integer) it.next()).intValue();
                if (tmp == fullyUpdatedProcess) {
                    continue;
                }
                p[i] = tmp;
                i++;
            }

            return p;
        }

        public Set getSetOfProcesses() {
            return processes;
        }

        public int getProcessesNumber() {
            return processes.size();
        }

        public void addProposal(Proposal p) {
            addOrderedMessages(p.getOrderedMessages());
            addUnorderedMessages(p.getUnorderedMessages());
            if (fullyUpdatedProcess == -1) {
                fullyUpdatedProcess = p.getFullyUpdatedProcess();
            }
            if (p.getSetOfProcesses() == null) {
                return;
            }
            if (processes == null) {
                processes = new LinkedHashSet();
            }
            processes.addAll(p.getSetOfProcesses());
        }

        public int getViewId() {
            return viewId;
        }

        public Map getOrderedMessages() {
            return unstableOrderedMessages;
        }

        public Map getUnorderedMessages() {
            return unstableUnorderedMessages;
        }

        public int getFullyUpdatedProcess() {
            return fullyUpdatedProcess;
        }

        public void setFullyUpdatedProcess(int fullyUpdatedProcess) {
            this.fullyUpdatedProcess = fullyUpdatedProcess;
        }

        public boolean containsProcess(Integer p) {
            if (processes == null) {
                return false;
            }
            return processes.contains(p);
        }

        public String toString() {
            StringBuffer s = new StringBuffer();
            s.append("ViewId ");
            s.append(viewId);
            s.append(" fullyUpdatedProcess: ");
            s.append(fullyUpdatedProcess);
            s.append(" ordered: ");
            s.append(unstableOrderedMessages.size());
            s.append(" unordered: ");
            s.append(unstableUnorderedMessages.size());
            if (processes != null) {
                s.append(" Processes: ");
                s.append(processes);
            }

            return s.toString();
        }
    }
}

