package lse.neko.sim.nekosim;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


/**
 * The scheduler of all simulated threads.
 * A singleton class, with only static methods.
 * FIXME: handle interrupt()
 */
class SimScheduler {

    /**
     * Schedules a thread for later execution.
     * @param thread Thread to be scheduled.
     * @param time Time at which the thread is scheduled.
     * Must be a time in the present or the future.
     */
    public static void schedule(SimThread thread, double scheduleTime) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(thread + " scheduled at " + scheduleTime);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("ENTRY schedule " + thread
                             + " at " + scheduleTime + "\n" + queue);
            }
        }
        if (SimScheduler.time - scheduleTime > EventQueue.EPS
            || Double.isNaN(scheduleTime))
        {
            throw new RuntimeException("Trying to schedule "
                                       + "thread " + thread
                                       + " for " + scheduleTime
                                       + " at " + SimScheduler.time);
        }
        queue.add(thread, scheduleTime);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN schedule " + thread
                         + " at " + scheduleTime + "\n" + queue);
        }
    }

    /**
     * Removes a thread from the scheduler queue.
     * @param thread Thread to be unscheduled.
     */
    public static void unschedule(SimThread thread) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("ENTRY unschedule " + thread + "\n" + queue);
        }
        queue.remove(thread);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN unschedule " + thread + "\n" + queue);
        }
    }

    private static final Object myLock = new Object() {
            public String toString() {
                return "SimScheduler's private lock";
            }
        };

    /**
     * Called by the current thread when it yields the processor
     * to another thread. The current thread will wait on
     * an object private to this class. It should not hold
     * any lock on objects.
     */
    public static void yield() {
        synchronized (myLock) {
            yield(myLock);
        }
    }

    /**
     * Called by the current thread when it yields the processor
     * to another thread. The current thread will wait on
     * the specified object. It should already hold a lock on
     * that object.
     *
     * @param lock object that the current thread will be
     * waiting on.
     */
    public static void yield(Object lock) {
        if (logger.isLoggable(Level.FINE)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("ENTRY yield " + lock + "\n" + queue);
            }
            logger.fine(currentThread + " yields " + lock);
        }
        assert lock != null && Thread.holdsLock(lock);
        assert currentThread.lock == null;
        SimThread oldThread = currentThread;
        advanceTime();
        prepareNextThread();
        if (oldThread != currentThread) {
            oldThread.lock = lock;
            startNextThread();
            try {
                lock.wait();
            } catch (InterruptedException ex) {
                // FIXME: complete here
            }
            assert oldThread.lock == null;
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("RETURN yield\n" + queue);
        }
    }

    /**
     * Called by the current thread when it finishes execution.
     */
    public static void yieldForever() {
        if (logger.isLoggable(Level.FINE)) {
            if (currentThread != null) {
                logger.fine(currentThread + " finished");
            } else {
                logger.fine("simulation starting");
            }
        }
        assert currentThread == null || currentThread.lock == null;
        advanceTime();
        prepareNextThread();
        startNextThread();
    }

    private static void advanceTime() {
        double newTime = queue.nextTime();
        if (time - newTime > EventQueue.EPS) {
            throw new RuntimeException("scheduler error! current time: "
                                       + time + " new time: " + newTime);
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("advancing time to " + newTime);
        }
        if (hook != null && newTime - time >= EventQueue.EPS) {
            // XXX: maybe another scheduler also needs the priority!
            // Extend SchedulerHook to take also the priority.
            hook.steppingTime(newTime);
            newTime = queue.nextTime();
        }
        time = newTime;
    }

    private static void prepareNextThread() {
        currentThread = (queue.isEmpty()) ? null : queue.pop();
        if (currentThread == null) {
            logger.fine("simulation finished");
            // FIXME: not a proper exit
            System.exit(0);
        }
        ObjectQueue.removeThread(currentThread);
    }

    private static void startNextThread() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(currentThread + " active");
        }
        Object lock = currentThread.lock;
        if (lock != null) {
            synchronized (lock) {
                // It is necessary to synchronize on lock.
                // Otherwise we cannot be sure that the current thread
                // has started wait() when it yielded the last time.
                currentThread.lock = null;
            }
            currentThread.interruptThread();
            // XXX: using interrupt() is 25% slower than using notify().
            // However, using notify() was not correct if more than
            // one threads waited on the same object.
            //synchronized (lock) {
            //        lock.notify();
            //}
        } else {
            currentThread.startThread();
        }
    }

    private static SimThread currentThread = null;

    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return  the currently executing thread.
     */
    public static SimThread currentThread() {
        return currentThread;
    }

    private static EventQueue queue = new EventQueue();

    /**
     * The current simulation time.
     */
    private static double time = 0.0;

    /**
     * Returns the current simulation time.
     */
    public static double clock() {
        return time;
    }

    private static SchedulerHook hook;

    /**
     * Registers a scheduler hook.
     * <code>null</code> means that no hook should be registered.
     *
     * @see SchedulerHook
     */
    public static void registerHook(SchedulerHook newHook) {
        SimScheduler.hook = newHook;
    }

    private static final Logger logger =
        NekoLogger.getLogger(SimScheduler.class.getName());

}
