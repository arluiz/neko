package lse.neko;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


/**
 * Neko's thread implementation. Depending on the execution mode of
 * Neko, this thread can be a real or a simulated thread. The user of
 * this thread does not need to know if the thread is simualted or
 * not.
 *
 * It provides similar operations as java.lang.Thread. A noteworthy
 * difference is that times are doubles.
 *
 * @see NekoThreadInterface
 */
public class NekoThread
    extends NekoObject
    implements NekoThreadInterface, Runnable
{

    /**
     * The thread implementation that most operations are delegated
     * to.
     */
    private NekoThreadInterface peer;

    /**
     * The process in which this thread is running. We call it process
     * context. It changes when the thread crosses process boundaries
     * (simulated threads often do so).
     */
    private NekoProcess process;

    /*
     * Constructors.
     * They are like java.lang.Thread's constructors,
     * but no group and stackSize parameters.
     */

    /**
     * Initializes a new thread with a default name.
     */
    public NekoThread() {
        init(null, "Thread-" + nextThreadNum());
    }

    /**
     * Initializes a new thread with a default name.
     *
     * @param target the object whose run method is called.
     */
    public NekoThread(Runnable target) {
        init(target, "Thread-" + nextThreadNum());
    }

    /**
     * Initializes a new thread.
     *
     * @param name name of the thread.
     */
    public NekoThread(String name) {
        init(null, name);
    }

    /**
     * Initializes a new thread.
     *
     * @param name name of the thread.
     * @param target the object whose run method is called.
     */
    public NekoThread(Runnable target, String name) {
        init(target, name);
    }

    /**
     * Used by lse.neko.comm.WrappingNekoThread.
     *
     * @param peer the thread implementation that most oprations are
     * delegated to.
     *
     * @see lse.neko.comm.WrappingNekoThread
     */
    protected NekoThread(NekoThreadInterface peer) {
        this.peer = peer;
    }

    /**
     * Counter for autonumbering anonymous threads.
     */
    private static int threadInitNumber;

    /**
     * Method for autonumbering anonymous threads.
     */
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    /**
     * The actual constructor code.
     *
     * @param name name of the thread.
     * @param target the object whose run method is called.
     */
    private void init(Runnable target, String name) {
        logger.log(Level.FINE, "creating ", name);
        Runnable realTarget = (target == null) ? this : target;
        peer = NekoSystem.instance().createPeerThread(this, realTarget, name);
        NekoThread parent = currentThread();
        setPriority(parent.getPriority());
        process = parent.getProcess();
    }

    /**
     * Sets the process context.
     *
     * @param process the new process context.
     */
    public void setProcess(NekoProcess process) {
        if (logger.isLoggable(Level.FINE) && process != this.process) {
            logger.log(Level.FINE,
                       "{0} process set from {1} to {2}",
                       new Object[] { this, this.process, process });
        }
        this.process = process;
    }

    /**
     * Returns the process context.
     *
     * @return the current process context.
     */
    public NekoProcess getProcess() {
        return this.process;
    }

    /*
     * Now comes the implementation of NekoThreadInterface.
     * All methods just delegate the actual task to the peer.
     */

    public void start() {
        peer.start();
    }

    public void interrupt() {
        peer.interrupt();
    }

    public boolean isInterrupted() {
        return peer.isInterrupted();
    }

    public boolean isAlive() {
        return peer.isAlive();
    }

    public void setPriority(int newPriority) {
        peer.setPriority(newPriority);
    }

    public int getPriority() {
        return peer.getPriority();
    }

    public void setName(String name) {
        peer.setName(name);
    }

    public String getName() {
        return peer.getName();
    }

    public void join(double millis) throws InterruptedException {
        peer.join(millis);
    }

    public void join() throws InterruptedException {
        peer.join();
    }

    /*
     * End of the implementation of NekoThreadInterface.
     */

    /*
     * Now come the static methods.
     * All methods just delegate the task to
     * <code>NekoSystem.instance().getThreadStatic()</code>.
     */

    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return  the currently executing thread.
     */
    public static NekoThread currentThread() {
        return NekoSystem.instance().getThreadStatic().currentThread();
    }

    /**
     * Causes the currently executing thread object to temporarily pause
     * and allow other threads to execute.
     */
    public static void yield() {
        NekoSystem.instance().getThreadStatic().yield();
    }

    /**
     * Causes the currently executing thread to sleep (cease execution)
     * for the specified number of milliseconds.
     * Note that the number of milliseconds is a double (unlike
     * in <code>java.lang.Thread</code>) and
     * can thus be a fraction.
     * <p>
     * Note: sleep does not throw InterruptedException as does
     * java.lang.Thread.sleep. FIXME: is there a good reason
     * for this difference?
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @exception  IllegalArgumentException  if the value of millis is
     *             negative.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        NekoObjectInterface#doNotify()
     */
    public static void sleep(double millis) throws InterruptedException {
        NekoSystem.instance().getThreadStatic().sleep(millis);
    }

    /**
     * Tests whether the current thread has been interrupted.  The
     * <i>interrupted status</i> of the thread is cleared by this method.  In
     * other words, if this method were to be called twice in succession, the
     * second call would return false (unless the current thread were
     * interrupted again, after the first call had cleared its interrupted
     * status and before the second call had examined it).
     *
     * @return  <code>true</code> if the current thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see java.lang.Thread#isInterrupted()
     */
    public static boolean interrupted() {
        return NekoSystem.instance().getThreadStatic().interrupted();
    }

    /*
     * End of the static methods.
     */

    /**
     * Returns the current time.
     *
     * @return the current time
     */
    public static double clock() {
        return NekoSystem.instance().clock();
    }

    /**
     * The method that this thread calls by default.
     */
    public void run() {
        throw new RuntimeException("Implement this method!");
    }

    /**
     * Prints information about the thread: its name and priority.
     */
    public String toString() {
        return "NekoThread[" + process + ","
            + getName() + "," + getPriority() + "]";
    }

    /**
     * Logger object for this class.
     */
    private static final Logger logger =
        NekoLogger.getLogger(NekoThread.class.getName());
}
