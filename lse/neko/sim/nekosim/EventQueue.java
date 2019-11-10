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
 * Queue that stores events: threads with a time when
 * they should be activated.
 */
public class EventQueue {

    /**
     * Returns true if the event queue is empty.
     */
    public boolean isEmpty() {
        boolean r = set.isEmpty();
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN isEmpty: " + r + "\n" + this);
        }
        return r;
    }

    /**
     * Returns the earliest event and removes it from the
     * event queue. The earliest event is the event
     * with the smallest time. If several such events
     * exist, events are popped in the order of insertion.
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
        thread.time = Double.NaN;
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN pop: " + thread + "\n" + this);
        }
        return thread;
    }

    /**
     * Returns the time of the earliest event.
     * If there are no events, returns <code>Double.MAX_VALUE</code>.
     */
    public double nextTime() {
        double r;
        if (set.isEmpty()) {
            r = Double.MAX_VALUE;
        } else {
            SimThread thread = (SimThread) set.first();
            r = thread.time;
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN nextTime: " + r + "\n" + this);
        }
        return r;
    }

    /**
     * Adds an event with an activation time to the event queue.
     */
    public void add(SimThread thread, double time) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("ENTRY add " + thread + " at " + time + "\n" + this);
        }
        thread.time = time;
        synchronized (this) {
            thread.serialNumberTimed = serialNumber;
            serialNumber++;
        }
        set.add(thread);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN add " + thread + " at " + time + "\n" + this);
        }
    }

    /**
     * Removes a thread from the event queue.
     */
    public void remove(SimThread thread) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("ENTRY remove " + thread + "\n" + this);
        }
        set.remove(thread);
        thread.time = Double.NaN;
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN remove " + thread + "\n" + this);
        }
    }

    /**
     * Stores all events in an ordered manner.
     */
    private SortedSet set = new TreeSet(new TimedComparator());

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
                       + ",#" + thread.serialNumberTimed + "),"
                       + thread + "}\n");
        }
        return buf.toString();
    }

    /**
     * If the difference of two times is smaller than EPS, they
     * are considered to be equal.
     */
    public static final double EPS = 1e-8;

    private class TimedComparator
        implements Comparator
    {
        public int compare(Object left, Object right) {
            SimThread l = (SimThread) left;
            SimThread r = (SimThread) right;
            double diff = l.time - r.time;
            if (diff <= -EPS) {
                return -1;
            } else if (diff >= EPS) {
                return +1;
            } else if (l.getPriority() > r.getPriority()) {
                return -1;
            } else if (l.getPriority() < r.getPriority()) {
                return +1;
            } else if (l.serialNumberTimed < r.serialNumberTimed) {
                return -1;
            } else if (l.serialNumberTimed > r.serialNumberTimed) {
                return +1;
            } else {
                return 0;
            }
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(EventQueue.class.getName());

}
