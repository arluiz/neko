package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.ProtocolImpl;

/**
 * A failure detector that never suspects any process.
 */
public class NoSuspicion
    extends ProtocolImpl
    implements FailureDetectorInterface
{

    public boolean isSuspected(int p) {
        return false;
    }

    public int getNumberOfSuspected() {
        return 0;
    }

    public void setListener(FailureDetectorListener listener) {
        // do nothing: no listener will ever be needed,
        // as no suspicions occur.
    }

}








