package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.HierarchicalId;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoThread;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.util.Util;
import lse.neko.util.logging.NekoLogger;


/**
 * Use this class if you have to manage several concurrent
 * executions of a given algorithm.
 * Useful if the concurrent executions do not need to
 * communicate with each other.
 * All messages of the algorithm have to have ExecutionID
 * (or a derived object) as content.
 * Executions should be numbered from 1.
 * Implement createReceiver.
 * @see ExecutionID
 */
public abstract class MultipleExecutions
    extends ProtocolImpl
    implements ReceiverInterface
{

    public MultipleExecutions() {
    }

    /**
     * Holds concurrent executions of an algorithm.
     * Algorithms are LayerInterfaces.
     */
    protected Map executions = new HashMap();
    // FIXME: should be private

    /**
     * Dispatches a message to one of the executions.
     * The message content should be of type ExecutionID,
     * containing the execution ID.
     * If the ID has not been encountered yet, the execution
     * is created with createReceiver().
     */
    public void deliver(NekoMessage m) {

        logger.log(Level.FINE, "deliverToOne ", m);

        ReceiverInterface receiver;
        synchronized (this) {

            logger.log(Level.FINE, "sync deliverToOne ", m);

            // The message should contain an execution ID
            int execID = ((ExecutionID) m.getContent()).getNumber();
            if (hasFinished(execID)) {

                logger.log(Level.FINE, "end deliverToOne (finished) ", m);

                return;
            }

            receiver = (ReceiverInterface) executions.get(new Integer(execID));
            if (receiver == null) {
                receiver = createReceiver(execID);
                executions.put(new Integer(execID), receiver);
                receiver.setId(new Id(getId(), execID));
                receiver.launch();
            }
        }

        logger.log(Level.FINE, "end deliverToOne (not finished) ", m);

        // FIXME: maybe throw new UnexpectedMessageException(m);
        receiver.deliver(m);

        logger.log(Level.FINE, "end deliverToOne ", m);
    }

    private boolean hasFinished(int execID) {
        if (execID <= finishedUpTo) {
            // already finished
            return true;
        }
        Integer id = new Integer(execID);
        return finished.contains(id);
    }

    /**
     * Discards information about a finished execution.
     * All future messages from this execution should be discarded as well.
     */
    public synchronized void finishExecution(int execID) {
        skipExecution(execID);

        // remove also from the dispatcher
        // XXX: the protocol must be present in the dispatcher
        // if it is not present, you should use skipExecution
        // FIXME: skipExecution should check that the protocol is
        // not present in the dispatcher
        NekoProcess process = NekoThread.currentThread().getProcess();
        process.getDispatcher().removeProtocol(new Id(getId(), execID));
    }

    public synchronized void skipExecution(int execID) {
        if (hasFinished(execID)) {
            return;
        }
        logger.log(Level.FINE, "Finishing execution {0,number,#}",
                   new Integer(execID));
        Integer id = new Integer(execID);
        executions.remove(id);
        // XXX: CompressedIntSet could be used
        finished.add(id);
        while (finished.contains(new Integer(finishedUpTo + 1))) {
            finished.remove(new Integer(finishedUpTo + 1));
            finishedUpTo++;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "  finishedUpTo {0,number,#}",
                       new Integer(finishedUpTo));
            // XXX: inefficient (string concatenations)
            logger.finer("  finished "
                         + Util.toString(finished.toArray()));
        }
    }

    /**
     * Just a set.
     */
    private Set finished = new HashSet();

    /**
     * The finished executions have numbers 1 up to (and including)
     * finishedUpTo plus the elements of finished.
     * @see #finished
     */
    private int finishedUpTo = 0;

    /**
     * Identifier for protocols created by this class.
     */
    public static class Id
        implements HierarchicalId, Serializable
    {

        private Object parentId;

        private int execId;

        public Id(Object parentId, int execId) {
            this.parentId = parentId;
            this.execId = execId;
        }

        public Object getParent() {
            return parentId;
        }

        public int hashCode() {
            return parentId.hashCode() + execId;
        }

        public boolean equals(Object o) {
            if (getClass() != o.getClass()) {
                return false;
            }
            Id right = (Id) o;
            return execId == right.execId
                && parentId.equals(right.parentId);
        }

        public String toString() {
            return parentId + "-" + execId;
        }
    }

    /**
     * This function generates a new receiver for the given execution ID.
     */
    public abstract ReceiverInterface createReceiver(int execID);

    private static final Logger logger =
        NekoLogger.getLogger(MultipleExecutions.class.getName());

}

