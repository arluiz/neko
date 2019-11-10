package lse.neko.abcast;

// java imports:
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypeConst;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ReceiverInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.consensus.ConsensusInterface;
import lse.neko.consensus.ConsensusValue;
import lse.neko.consensus.DecisionListener;
import lse.neko.networks.QueueSizeObservable;
import lse.neko.util.ContentRB;
import lse.neko.util.ObservableInteger;
import lse.neko.util.logging.NekoLogger;


/**
 * A variant of the Chandra-Toueg atomic broadcast.
 * Consensus is not on vectors of messages,
 * but on vectors of message identifiers.
 */
public class ChandraToueg
    extends ChandraTouegClient
    implements QueueSizeObservable, DecisionListener, ReceiverInterface
{

    /**
     * Consensus object. Must be a layer below this object.
     */
    private ConsensusInterface cons;

    /**
     * R-delivered but not A-delivered messages.
     * The key is the id of the message, the value is the content.
     */
    private Map aUndelivered = new HashMap();

    /**
     * IDs of messages whose order has been decided.
     * The message has not necessarily been A-delivered yet.
     */
    private Set aOrdered = new HashSet();

    /**
     * IDs of messages whose order has not been decided yet.
     * All these messages have been R-delivered.
     */
    // Changed by Richard. This variable is needed by
    // the TerminatingChandraToueg subclass
    Set aUnordered = new LinkedHashSet();

    public ChandraToueg(NekoProcess process,
                        ConsensusInterface cons)
    {
        super(process);
        this.cons = cons;
        proposalMaxSize = NekoSystem.instance().getConfig().
            getInteger("ChandraToueg.proposal.max.size", Integer.MAX_VALUE);
        bufferMaxSize = NekoSystem.instance().getConfig().
            getInteger("ChandraToueg.buffer.max.size", Integer.MAX_VALUE);
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    /**
     * The serial number of the consensus whose decision
     * we are awaiting, or whose decision
     * we already have but not all messages
     * with their ID in the decision have been delivered yet.
     */
    private int nextConsensusId = 1;

    /**
     * The serial number of the next consensus
     * whose decision we are awaiting. Different from
     * nextConsensusId.
     */
    private int nextDecision = 1;

    /**
     * The serial number of the next consensus
     * we are going to propose a value for.
     */
    private int nextProposal = 1;

    /**
     * The index of the ID in the decision we are delivering.
     */
    private int nextToBeDelivered = 0;

    private void aDeliver() {

        Iterator it = decisions.iterator();

    delivery:
        while (it.hasNext()) {

            ConsensusValue decision = (ConsensusValue) it.next();
            if (decision.getNumber() > nextConsensusId) {

                // decision of future consensus
                break delivery;

            } else if (decision.getNumber() < nextConsensusId) {
                throw new RuntimeException("assertion failed: "
                                           + "decision.getNumber() >= "
                                           + "nextConsensusId");
            }

            Object[] ids = (Object[]) decision.getValue();
            for (; nextToBeDelivered < ids.length; nextToBeDelivered++) {
                Object id = ids[nextToBeDelivered];

                // check if the message with this id
                // has already been received.
                // if not, quit delivering.
                NekoMessage m = (NekoMessage) aUndelivered.get(id);
                if (m == null) {
                    break delivery;
                }

                ContentRB content = (ContentRB) m.getContent();

                NekoMessage t =
                    new NekoMessage(content.getSource(),
                                    m.getDestinations(),
                                    content.getProtocolId(),
                                    content.getContent(),
                                    content.getType());

                logger.log(Level.FINE, "delivering ", t);
                receiver.deliver(t);

                logger.log(Level.FINE, "delivered ", t);

                aUndelivered.remove(id);
                aOrdered.remove(id);
                updateQueueSize();
            }

            it.remove();
            nextToBeDelivered = 0;
            nextConsensusId++;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "nextDecision ", new Object[] {
                new Integer(nextDecision),
                "nextProposal",
                new Integer(nextProposal),
                "aUndelivered",
                aUnordered.toArray()
            });
        }

        if (nextDecision >= nextProposal && aUnordered.size() != 0) {
            //Object[] proposal = aUnordered.toArray();
            Object[] proposal;
            if (aUnordered.size() <= proposalMaxSize) {
                proposal = aUnordered.toArray();
            } else {
            //        logger.fine(process.clock()+": p"+process.getID()+
            //                           " tried to propose "+
            //                           aUnordered.size()+" ids");
                proposal = new Object[proposalMaxSize];
                Iterator iter = aUnordered.iterator();
                for (int i = 0; i < proposal.length; i++) {
                    proposal[i] = iter.next();
                }
            }

            logger.log(Level.FINE, "proposing ", proposal);

            while (nextDecision >= nextProposal) {
                cons.propose(proposal);
                nextProposal++;
            }

            logger.log(Level.FINE, "proposed ", proposal);
        }

    }

    private int proposalMaxSize;
    private int bufferMaxSize;

    /**
     * Decisions of consensus.
     */
    private SortedSet decisions =
        new TreeSet(new ConsensusValueComparator());

    private static class ConsensusValueComparator
        implements Comparator
    {
        /**
         * We exploit that 0 == compare(o1, o2) implies o1 == o2
         * in the algorithm! That's why we can use a Set.
         */
        public int compare(Object o1, Object o2) {
            ConsensusValue left = (ConsensusValue) o1;
            ConsensusValue right = (ConsensusValue) o2;
            return left.getNumber() - right.getNumber();
        }

        public boolean equals(Object o1, Object o2) {
            return 0 == compare(o1, o2);
        }
    }

    public void deliver(NekoMessage m) {

        logger.log(Level.FINE, "deliver ", m);

        synchronized (this) {

            logger.log(Level.FINE, "sync deliver ", m);

            switch (m.getType()) {

            case MessageTypeConst.AB_START:

                ContentRB content = (ContentRB) m.getContent();

                //logger.fine("p"+process.getID()+
                //               ": received message with ID "+content.getId());
                aUndelivered.put(content.getId(), m);
                if (!aOrdered.contains(content.getId())) {
                    aUnordered.add(content.getId());
                }
                updateQueueSize();
                if (aUndelivered.size() > bufferMaxSize
                    || aUnordered.size() > bufferMaxSize)
                {
                    throw new RuntimeException("Maximum buffer size ("
                                               + bufferMaxSize
                                               + ") exceeded");
                }
                aDeliver();
                break;

            default:

                throw new UnexpectedMessageException(m);

            }

            logger.log(Level.FINE, "end sync deliver ", m);
        }

        logger.log(Level.FINE, "end deliver ", m);
    }

    /**
     * Method that is called whenever a decision has been taken by
     * Consensus. This method needs to be synchronized in order to
     * avoid concurrent modifications of aUnordered with deliver(...)
     *
     * @param k The consensus run for which a decision has been reached
     * @param decision The value of the decision
     */
    public synchronized void notifyDecision(int k, Object decision) {

        ConsensusValue consensusResult =
            new ConsensusValue(k, decision);

        decisions.add(consensusResult);

        SortedSet d = decisions.tailSet(consensusResult);
        Iterator it = d.iterator();
        while (it.hasNext()) {
            ConsensusValue v = (ConsensusValue) it.next();
            if (v.getNumber() == nextDecision) {
                nextDecision++;
            } else {
                break;
            }
        }

        Object[] ids = (Object[]) consensusResult.getValue();
        for (int i = 0; i < ids.length; i++) {
            aUnordered.remove(ids[i]);
            aOrdered.add(ids[i]);
        }
        updateQueueSize();

        aDeliver();
        return;
    }

    public int getOutgoingQueueSize() {
        return queueSize.get();
    }

    private ObservableInteger queueSize = new ObservableInteger(0);

    public void addQueueSizeObserver(Observer observer,
                                     int queueSizeThreshold,
                                     boolean getsBelow)
    {
        queueSize.addObserver(observer, queueSizeThreshold, getsBelow);
    }

    private void updateQueueSize() {
        int max = Math.max(aUndelivered.size(), aUnordered.size());
        max = Math.max(max, aOrdered.size());
        queueSize.set(max);
    }

    private static final Logger logger =
        NekoLogger.getLogger(ChandraToueg.class.getName());

}

