package lse.neko.util.logging;

// java imports:
import java.util.logging.Level;
import java.util.logging.LogRecord;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;


/**
 * LogRecord type used by Neko.
 * It registers the Neko time (simulated or real)
 * when created. Also, it remembers the name
 * of the thread which created it and the Neko process
 * in which it was created.
 */
public class NekoLogRecord
    extends LogRecord
{

    /**
     * Neko time of creating the log record, simulated or real.
     */
    private double time;

    /**
     * Name of the creating thread.
     */
    private String threadName;

    /**
     * ID of the creating Neko process.
     * Integer.MIN_VALUE if the object was not created by a process.
     */
    private int processId;

    public NekoLogRecord(Level level, String msg) {
        super(level, msg);

        // set the time in millis to 0.
        // This is used to detect that the Java logging classes are
        // used instead of the Neko classes.
        setMillis(0L);

        try {
            time = NekoSystem.instance().clock();
            threadName = NekoThread.currentThread().getName();
            NekoProcess process = NekoThread.currentThread().getProcess();
            processId =
                (process != null) ? process.getID() : Integer.MIN_VALUE;
        } catch (NullPointerException ex) {
            // NekoSystem is not yet initialized
            time = 0;
            threadName = Thread.currentThread().getName();
            processId = Integer.MIN_VALUE;
            // XXX: this branch is taken when there is problem
            // contacting slave factories. At this point, NekoSystem
            // is not yet initialized. A better solution would
            // initialize NekoSystem before any communication on the
            // network.
        }
    }

    public double getTime() {
        return time;
    }

    public String getThreadName() {
        return threadName;
    }

    public int getProcessId() {
        return processId;
    }

}
