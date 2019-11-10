package lse.neko.failureDetectors;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.util.logging.NekoLogger;


public abstract class FailureDetector
    extends ProtocolImpl
    implements FailureDetectorInterface
{

    /**
     * Set of processes suspected by this process.
     * suspected[i] == true means that process #i is suspected.
     */
    protected boolean[] suspected;
    protected int numSuspected;

    protected NekoProcess process;

    public FailureDetector(NekoProcess process) {
        this.process = process;
        suspected = new boolean[process.getN()];
        numSuspected = 0;
    }

    protected FailureDetectorListener listener;

    public void setListener(FailureDetectorListener listener) {
        this.listener = listener;
    }

    /**
     * Tests if a process is suspected by this process.
     * @param id the process whose status is queried
     */
    public boolean isSuspected(int id) {
        return suspected[id];
    }

    public int getNumberOfSuspected() {
        return numSuspected;
    }

    /**
     * Adds a process to the set of suspected processes.
     * @param id ID of the suspected process
     */
    protected void suspect(int id) {

        if (suspected[id]) {
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "suspecting {0}", new Integer(id));
        }

        synchronized (this) {
            if (suspected[id]) {
                return;
            }
            suspected[id] = true;
            numSuspected++;
        }

        if (listener != null) {
            listener.statusChange(true, id);
        }
    }

    /**
     * Removes a process from the set of suspected processes.
     * @param id ID of the trusted process
     */
    protected void unsuspect(int id) {

        if (!suspected[id]) {
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "unsuspecting {0}", new Integer(id));
        }

        synchronized (this) {
            if (!suspected[id]) {
                return;
            }
            suspected[id] = false;
            numSuspected--;
        }

        if (listener != null) {
            listener.statusChange(false, id);
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(FailureDetector.class.getName());
}
