package lse.neko.util;

// java imports:
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
// import lse.neko.util.TimerTask;
// ambiguous with: java.util.TimerTask
import lse.neko.util.logging.NekoLogger;

public class Timer {

    private SortedSet queue = new TreeSet(TimerTask.COMPARATOR);
    private TimerThread thread = new TimerThread();

    public Timer() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("creating timer " + this + " at time "
                        + NekoSystem.instance().clock());
        }
        thread.start();
    }

    public void schedule(TimerTask task, double delay) {
        double time = NekoSystem.instance().clock();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("scheduling task " + task + " on timer " + this
                        + " delay " + delay + " at time " + time);
        }
        synchronized (queue) {
            task.schedule(this, time + delay);
            queue.add(task);
            synchronized (thread) {
                thread.doNotify();
            }
        }
    }

    void cancel(TimerTask task) {
        synchronized (queue) {
            boolean removed = queue.remove(task);
            task.doCancel();
        }
    }

    private class TimerThread extends NekoThread {

        public TimerThread() {
            super("TimerThread");
        }

        public void run() {
            while (true) {
                TimerTask task;
                double waitDelay;
                synchronized (queue) {
                    if (queue.isEmpty()) {
                        task = null;
                        waitDelay = Double.NaN; // ignored
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer("timer " + Timer.this
                                         + " woke up / no task scheduled");
                        }
                    } else {
                        task = (TimerTask) queue.first();
                        waitDelay = task.getNextExecutionTime()
                            - NekoSystem.instance().clock();
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer("timer " + Timer.this
                                         + " woke up / top task is " + task
                                         + " scheduled in "
                                         + waitDelay + " time");
                        }
                        if (waitDelay <= 0) {
                            queue.remove(task);
                            task.prepareRun();
                        }
                    }
                }

                NekoMessage m;
                if (task == null) {
                    synchronized (this) {
                        try {
                            doWait();
                        } catch (InterruptedException ex) {
                        }
                    }
                } else if (waitDelay <= 0) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("timer " + Timer.this + " running task "
                                    + task + " at time "
                                    + NekoSystem.instance().clock());
                    }
                    task.run();
                } else {
                    synchronized (this) {
                        try {
                            doWait(waitDelay);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }
    }

    static Logger getLogger() {
        return logger;
    }

    private static final Logger logger =
        NekoLogger.getLogger(Timer.class.getName());
}

