package lse.neko.sim.nekosim;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoObjectInterface;
import lse.neko.util.logging.NekoLogger;


/**
 * An object that SimThreads can synchronize on.
 */
public class SimObject
    implements NekoObjectInterface
{

    public void doNotify() {
        if (queue == null || queue.isEmpty()) {
            return;
        }
        SimThread thread = queue.pop();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(thread + " notified on " + lock);
        }
        SimScheduler.unschedule(thread);
        SimScheduler.schedule(thread, SimScheduler.clock());
    }

    public void doNotifyAll() {
        if (queue == null) {
            return;
        }
        while (!queue.isEmpty()) {
            doNotify();
        }
    }

    public void doWait() {
        if (queue == null) {
            queue = new ObjectQueue();
        }
        SimThread thread = SimScheduler.currentThread();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(thread + " waiting on " + lock);
        }
        queue.add(thread);
        SimScheduler.yield(lock);
    }

    public void doWait(double timeout) {
        if (queue == null) {
            queue = new ObjectQueue();
        }
        SimThread thread = SimScheduler.currentThread();
        final double wakeUpTime = SimScheduler.clock() + timeout;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(thread + " waiting on " + lock
                        + " until " + wakeUpTime);
        }
        queue.add(thread);
        SimScheduler.schedule(thread, wakeUpTime);
        SimScheduler.yield(lock);
    }

    /**
     * An event queue for the threads waiting on this object.
     */
    private ObjectQueue queue = null;

    public SimObject() {
        lock = this;
    }

    public SimObject(Object object) {
        lock = object;
    }

    private Object lock;

    private static final Logger logger =
        NekoLogger.getLogger(SimObject.class.getName());

}
