package lse.neko.util;

// java imports:
import java.util.Comparator;
import java.util.logging.Level;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
// import lse.neko.util.Timer;
// ambiguous with: java.util.Timer

/**
 * A task that can be scheduled for execution by a <code>Timer</code>.
 * It is like <code>java.util.TimerTask</code>, but uses <code>double</code>
 * for representing times.
 * A difference is that tasks can be re-scheduled.
 * <p>
 * Another difference is that repeating tasks are not yet supported.
 * However, you can easily obtain the same functionality by re-scheduling
 * a task in its own <code>run</code> method.
 *
 * @see Timer
 */
public abstract class TimerTask
    implements Runnable
{

    /**
     * This object is used to control access to the TimerTask internals.
     */
    private Object lock = new Object();

    /**
     * The timer the task is scheduled in.  non-<code>null</code> if
     * the task is scheduled for execution (i.e., has been scheduled
     * but has not been cancelled and has not started execution yet),
     * <code>null</code> otherwise.  Also used to make sure that the
     * task is only in one scheduler queue.
     */
    private Timer timer = null;

    /**
     * The next execution time for this task.
     * Invalid if this task is not scheduled for execution.
     */
    private double nextExecutionTime;

    /**
     * Returns the next execution time for this task.  Returns
     * <code>Double.MAX_VALUE</code> if this task is not scheduled for
     * execution.
     */
    public double getNextExecutionTime() {
        synchronized (lock) {
            if (timer != null) {
                return nextExecutionTime;
            } else {
                return Double.MAX_VALUE;
            }
        }
    }

    /**
     * Serial number set when the task is scheduled.
     * Needed because we want to make Timer's behavior deterministic
     * (tasks scheduled at the same time are executed in the order
     * of scheduling).
     */
    private long serialNumber;

    private static long nextSerialNumber = 0;
    private static Object staticLock = new Object();

    /**
     * Only called by <code>Timer.schedule</code>.
     * @see Timer#schedule
     */
    void schedule(Timer newTimer, double newNextExecutionTime) {
        assert !Double.isNaN(newNextExecutionTime)
            && !Double.isInfinite(newNextExecutionTime)
            && newTimer != null;
        synchronized (lock) {
            if (this.timer != null) {
                throw new IllegalStateException("Task already scheduled");
            }
            this.nextExecutionTime = newNextExecutionTime;
            this.timer = newTimer;
            synchronized (staticLock) {
                serialNumber = nextSerialNumber++;
            }
        }
    }

    /**
     * Class to compare two TimerTasks.
     * First key: nextExecutionTime, second key: serialNumber.
     * @see #nextExecutionTime
     * @see #serialNumber
     */
    private static class MyComparator
        implements Comparator
    {
        public int compare(Object l, Object r) {
            assert l instanceof TimerTask && r instanceof TimerTask;
            TimerTask left = (TimerTask) l;
            TimerTask right = (TimerTask) r;

            if (left == right) {
                return 0;
            }
            int ret = Double.compare(left.nextExecutionTime,
                                     right.nextExecutionTime);
            if (ret != 0) { return ret; }
            return Util.compare(left.serialNumber, right.serialNumber);
        }

        public boolean equals(Object l, Object r) {
            assert l instanceof TimerTask && r instanceof TimerTask;
            TimerTask left = (TimerTask) l;
            TimerTask right = (TimerTask) r;

            return left == right;
        }
    }

    /**
     * Instance of <code>MyComparator</code>, only used by
     * <code>Timer</code>.
     */
    static final Comparator COMPARATOR = new MyComparator();

    /**
     * Creates a new timer task.
     */
    protected TimerTask() {
    }

    /**
     * The action to be performed by this timer task.
     */
    public abstract void run();

    /**
     * Cancels this timer task.  If the task is scheduled, it will
     * never run (unless it is scheduled again). Otherwise this method
     * has no effect. In particular, if the task is running when this
     * call occurs, the task will run to completion.
     *
     * @return true if the task is scheduled. Loosely speaking, this
     * method returns <tt>true</tt> if it prevents executions from
     * taking place.
     */
    public boolean cancel() {
        Timer t;
        synchronized (lock) {
            if (Timer.getLogger().isLoggable(Level.FINE)) {
                Timer.getLogger().fine("canceling task " + this + " on timer "
                                       + timer + " at time "
                                       + NekoSystem.instance().clock());
            }
            t = timer;
            if (t == null) {
                return false;
            }
        }
        t.cancel(this);
        return true;
    }

    /**
     * Only called by <code>Timer.cancel</code>.
     */
    void doCancel() {
        synchronized (lock) {
            timer = null;
        }
    }

    /**
     * Returns the <i>scheduled</i> execution time of the most recent
     * <i>actual</i> execution of this task.  (If this method is invoked
     * while task execution is in progress, the return value is the scheduled
     * execution time of the ongoing task execution.)
     *
     * <p>This method is typically invoked from within a task's run method, to
     * determine whether the current execution of the task is sufficiently
     * timely to warrant performing the scheduled activity:
     * <pre>
     *   public void run() {
     *       if (NekoSystem.instance().clock() - scheduledExecutionTime() >=
     *           MAX_TARDINESS)
     *               return;  // Too late; skip this execution.
     *       // Perform the task
     *   }
     * </pre>
     *
     * @return the time at which the most recent execution of this task was
     *         scheduled to occur, in milliseconds.
     *         The return value is undefined if the task has yet to commence
     *         its first execution.
     */
    public double scheduledExecutionTime() {
        synchronized (lock) {
            return scheduledExecutionTime;
        }
    }

    private double scheduledExecutionTime;

    /**
     * The process in which this task was created.  Tasks need to
     * store this information because they might be called from a
     * timer thread that has no process context: the thread of the
     * system-wide timer accessed by
     * <code>NekoSystem.getTimer()</code>, which is shared by all
     * processes.
     *
     * @see NekoSystem#getTimer
     */
    private final NekoProcess process =
        NekoThread.currentThread().getProcess();

    /**
     * Only called by <code>Timer</code>, right before calling the
     * <code>run</code> method. Sets the right process context
     * for the current thread.
     */
    void prepareRun() {
        synchronized (lock) {
            scheduledExecutionTime = nextExecutionTime;
            timer = null;
        }
        NekoThread.currentThread().setProcess(process);
    }

    public String toString() {
        return "TimerTask[timer=" + timer
            + ", nextExecutionTime=" + nextExecutionTime
            + ", scheduledExecutionTime=" + scheduledExecutionTime + "]";
    }

}
