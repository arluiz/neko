package lse.neko.failureDetectors;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.util.logging.NekoLogger;


public abstract class RingFailureDetector
    extends ProtocolImpl
    implements FailureDetectorInterface
{

    /**
     * Variable indicating if the predecessor is suspected.
     */
    protected boolean suspected = false;

    /**
     * Id of the monitored process.
     */
    protected int monitored = -1;

    /**
     * Id of the successor of this process (destination of the
    * heartbeat messages.
    */
    protected int successor = -1;

    private NekoProcess process;

    public RingFailureDetector(NekoProcess process) {
        this.process = process;
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                       "Created Ring Failure Detector for {0}",
                       new Integer(process.getID()));
        }
        suspected = false;
        this.monitored = ((process.getID() - 1) + process.getN())
            % process.getN(); // predecessor of 'process'
        this.successor = ((process.getID() + 1) % process.getN());
    }

    /**
     * Indicates if the processes 'id' i
    * suspected. In the case of a Ring failure
    * detector, 'id' must be the monitored process
    * (i.e. the predecessor of the process querying the
    * failure detector).
     * @param id the monitored process
     * @throws IllegalArgumentException if 'id' is
     * not the monitored process
     */
    public boolean isSuspected(int id) {
        if (monitored == id) {
            return suspected;
        } else {
            throw new IllegalArgumentException("This failure detector monitors "
                                            + monitored
                                            + ". Asked for state of " + id);
        }
    }

    /**
     * Returns the number of suspected processes. This
    * can be either 1 or 0 in the case of a RingFailureDetector
    */
    public int getNumberOfSuspected() {
        return suspected ? 1 : 0;
    }

    /**
     * Sets the listener to call if suspicions or un-suspicions
     * happen.
     */
    public void setListener(FailureDetectorListener listener) {
        synchronized (this) {
            if (this.listener != null) {
                throw new IllegalStateException("The listener for "
                                                + this + " has already "
                                                + "been initialized");
            }
            this.listener = listener;
        }
    }

    /**
     * Implementation of the listener that will be called
     * whenever a suspicion occurs or that a process is not
     * suspected anymore.
     */
    FailureDetectorListener listener = null;

    /**
     * Suspect the process that is monitored by this
     * failure detector module.
     */
    protected void suspect() {

        if (suspected) {
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "suspecting {0}", new Integer(monitored));
        }

        // suspect the monitored process
        synchronized (this) {
            if (suspected) {
                return;
            }
            suspected = true;

            if (listener != null) {
                // signal that the predecessor is suspected
                listener.statusChange(suspected, monitored);
            }
        }

    }

    /**
     * Removes the monitored process from the
     * suspected list.
     */
    protected void unsuspect() {

        if (!suspected) {
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "unsuspecting {0}", new Integer(monitored));
        }

        synchronized (this) {
            if (!suspected) {
                return;
            }
            suspected = false;

            if (listener != null) {
                // signal that the predecessor is not suspected
                listener.statusChange(suspected, monitored);
            }
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(RingFailureDetector.class.getName());
}
