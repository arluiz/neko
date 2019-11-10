package lse.neko.sim.nekosim;

// java imports:
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


/**
 * Queue that stores threads waiting for an object.
 * The queue is ordered by priority.
 */
public class ObjectQueue {

    /**
     * Returns true if the event queue is empty.
     */
    public boolean isEmpty() {
        return set.isEmpty();
    }

    /**
     * Returns the first thread and removes it from the
     * queue. The first thread is the one with the highest
     * priority. If several such events
     * exist, threads are popped in the order of insertion.
     */
    public SimThread pop() {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("ENTRY pop\n" + this);
        }
        SimThread thread = (SimThread) set.first();
        boolean contained = set.remove(thread);
        if (!contained) {
            throw new RuntimeException("Assertion failed: "
                                       + "queue could not remove "
                                       + "its first element! " + this);
        }
        assert thread.queue == this;
        thread.queue = null;
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN pop: " + thread + "\n" + this);
        }
        return thread;
    }

    /**
     * Adds a thread to the queue.
     */
    public void add(SimThread thread) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("ENTRY add " + thread + "\n" + this);
        }
        synchronized (this) {
            thread.serialNumberUntimed = serialNumber;
            serialNumber++;
        }
        assert thread.queue == null;
        thread.queue = this;
        set.add(thread);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN add " + thread + "\n" + this);
        }
    }

    /**
     * Removes a thread from the queue.
     */
    public void remove(SimThread thread) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("ENTRY remove " + thread + "\n" + this);
        }
        boolean contained = set.remove(thread);
        if (!contained) {
            throw new RuntimeException("Assertion failed: "
                                       + "queue could not remove "
                                       + thread + "! " + this);
        }
        assert thread.queue == this;
        thread.queue = null;
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN remove " + thread + "\n" + this);
        }
    }

    /**
     * Removes a thread from the <code>ObjectQueue</code> it is in.
     * If the thread is not in an <code>ObjectQueue</code>, does nothing.
     */
    public static void removeThread(SimThread thread) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("ENTRY removeThread " + thread);
        }
        if (thread.queue != null) {
            thread.queue.remove(thread);
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN removeThread " + thread);
        }
    }

    /**
     * Stores all events in an ordered manner.
     */
    private SortedSet set = new TreeSet(new UntimedComparator());

    /**
     * A unique serial number to all added threads.
     * Grows when a thread is added.
     * Needed to ensure that events are returned in the order
     * of insertion by pop().
     */
    private long serialNumber = 1;

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("EventQueue:\n");
        Iterator it = set.iterator();
        while (it.hasNext()) {
            SimThread thread = (SimThread) it.next();
            buf.append("  {" + thread.time + "(pr" + thread.getPriority()
                       + ",#" + thread.serialNumberUntimed + "),"
                       + thread + "}\n");
        }
        return buf.toString();
    }

    private class UntimedComparator
        implements Comparator
    {
        public int compare(Object left, Object right) {
            SimThread l = (SimThread) left;
            SimThread r = (SimThread) right;
            if (l.getPriority() > r.getPriority()) {
                return -1;
            } else if (l.getPriority() < r.getPriority()) {
                return +1;
            } else if (l.serialNumberUntimed < r.serialNumberUntimed) {
                return -1;
            } else if (l.serialNumberUntimed > r.serialNumberUntimed) {
                return +1;
            } else {
                return 0;
            }
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(ObjectQueue.class.getName());

}
