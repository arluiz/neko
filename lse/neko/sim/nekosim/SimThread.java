package lse.neko.sim.nekosim;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoThread;
import lse.neko.NekoThreadInterface;
import lse.neko.util.logging.NekoLogger;


/**
 * A thread of NekoSim.
 */
class SimThread
    extends SimObject
    implements NekoThreadInterface, Runnable
{

    /**
     * Creates a simulated thread.
     *
     * @param nekoPeer the peer NekoThread object.
     * @param runnable the object whose <code>run</code> method is called.
     * @param name the name of the new thread. If null, a name is generated.
     */
    public SimThread(NekoThread nekoPeer, Runnable runnable, String name) {
        super();
        if (nekoPeer == null || runnable == null) {
            throw new NullPointerException();
        }
        this.nekoPeer = nekoPeer;
        peer = new MyThread(name, runnable);
    }

    /**
     * The peer NekoThread object.
     */
    private final NekoThread nekoPeer;

    /**
     * Returns the peer <code>NekoThread</code> object.
     */
    public NekoThread getNekoPeer() {
        return nekoPeer;
    }

    /**
     * The <code>java.lang.Thread</code> associated with this thread.
     */
    private final Thread peer;

    private class MyThread
        extends Thread
    {
        public MyThread(String name, Runnable runnable) {
            super(name);
            this.runnable = runnable;
        }

        private Runnable runnable;

        public void run() {
            try {
                runnable.run();
            } catch (Throwable ex) {
                getThreadGroup().uncaughtException(this, ex);
            } finally {
                SimThread.this.isStopped = true;
                // this is to wake up the threads waiting in join
                // XXX:
                // note that all other threads that called doWait()
                // are woken up as well.  This might not be what was
                // intended.  java.lang.Thread has the same,
                // undocumented behavior.
                SimThread.this.doNotifyAll();
                SimScheduler.yieldForever();
            }
        }
    }

    /**
     * Causes this thread to begin execution:
     * the thread is scheduled to be executed at the current simulation
     * time.
     *
     * @exception IllegalThreadStateException
     * if the thread was already started.
     */
    public void start() {
        if (isStarted) {
            throw new IllegalThreadStateException
                ("Thread " + this + " has already been started!");
        }
        isStarted = true;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this + " starting");
        }
        SimScheduler.schedule(this, SimScheduler.clock());
    }

    public void interrupt() {
        // FIXME
        throw new RuntimeException("Not implemented!");
    }

    public boolean isInterrupted() {
        // FIXME
        throw new RuntimeException("Not implemented!");
    }

    public void setName(String name) {
        peer.setName(name);
    }

    public String getName() {
        return peer.getName();
    }

    public synchronized void join(double millis) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("Timeout value is negative");
        }
        double deadline = SimScheduler.clock() + millis;
        double delay = millis;
        // XXX: join exits immediately on threads that have not been
        // started yet. java.lang.Thread has the same undocumented
        // behavior.
        while (isAlive() & delay > 0) {
            doWait(delay);
            delay = deadline - SimScheduler.clock();
        }
    }

    public synchronized void join() throws InterruptedException {
        // XXX: join exits immediately on threads that have not been
        // started yet. java.lang.Thread has the same undocumented
        // behavior.
        while (isAlive()) {
            doWait();
        }
    }

    /**
     * Code that this thread is supposed to run.
     * Override it.
     */
    public void run() {
        throw new RuntimeException("Implement the run() method!");
    }

    /**
     * Flag that indicates if the thread has been started.
     */
    private boolean isStarted = false;

    /**
     * Flag that indicates if the thread has finished executing.
     */
    private boolean isStopped = false;

    public boolean isAlive() {
        return isStarted && !isStopped;
    }

    /**
     * Time when this thread will be executed next.
     * <code>NaN</code> means that the thread is not scheduled
     * (and thus this time is undefined).
     * Only used by <code>EventQueue</code> (and
     * <code>ObjectQueue.toString</code>).
     */
    double time = Double.NaN;

    /**
     * Serial number modified by <code>EventQueue.add</code>.
     * Needed to ensure a well-defined order of threads
     * in EventQueue.
     * Only used by <code>EventQueue</code>.
     */
    long serialNumberTimed;

    /**
     * Serial number modified by <code>ObjectQueue.add</code>.
     * Needed to ensure a well-defined order of threads
     * in ObjectQueue.
     * Only used by <code>ObjectQueue</code>.
     */
    long serialNumberUntimed;

    /**
     * Object waiting queue that has this thread.
     * Is only valid if the thread is doing wait(timeout)
     * on a SimObject.
     * The scheduler needs to remove the thread from this
     * queue if the timeout expires first.
     * Only used by <code>ObjectQueue</code>.
     */
    ObjectQueue queue = null;

    /**
     * The object this thread is waiting on.
     * Only used by <code>SimScheduler</code>
     */
    Object lock = null;

    public String toString() {
        return "SimThread[" + peer.getName() + "]";
    }

    /**
     * Calls interrupt() on the peer thread.
     * Only called by SimScheduler.
     * If the thread is waiting on an object,
     * the scheduler has to interrupt this waiting when
     * the thread is scheduled again.
     * FIXME: really necessary?
     */
    void interruptThread() {
        peer.interrupt();
    }

    /**
     * Calls start() on the peer thread.
     * Only called by SimScheduler.
     */
    void startThread() {
        peer.start();
    }

    private int priority = Thread.NORM_PRIORITY;

    public void setPriority(int priority) {
        // remove the thread from every event queue
        if (queue != null) {
            queue.remove(this);
        }
        double storedTime = this.time;
        SimScheduler.unschedule(this);
        // change the priority
        this.priority = priority;
        // re-insert the thread into every event queue
        if (queue != null) {
            queue.add(this);
        }
        if (!Double.isNaN(storedTime)) {
            SimScheduler.schedule(this, storedTime);
        }
    }

    public int getPriority() {
        return priority;
    }

    private static final Logger logger =
        NekoLogger.getLogger(SimThread.class.getName());

}
