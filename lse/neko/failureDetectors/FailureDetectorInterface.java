package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.Protocol;

/**
 * Interface for failure detectors. You should implement
 * this interface and install an instance at each process.
 * The failure detector maintains a list of suspected processes.
 * (It may make mistakes in suspecting processes.)
 * Also, when a process becomes suspected or trusted,
 * the registered listener is notified.
 *
 * @see lse.neko.failureDetectors.FailureDetector
 */
public interface FailureDetectorInterface
    extends Protocol
{

    /**
     * Queries the state of a monitored process.
     *
     * @return true if process p is suspected to have crashed.
     */
    boolean isSuspected(int p);

    /**
     * A quick check if any process is suspected.
     *
     * @return the number of suspected processes.
     */
    int getNumberOfSuspected();

   /**
    * Set the listener to be notified
    * whenever the status of any monitored process changes.
    */
    void setListener(FailureDetectorListener listener);

}








