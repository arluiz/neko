package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.ProtocolImpl;

public class OmegaNoSuspicion
    extends ProtocolImpl
    implements OmegaFailureDetectorInterface
{

    /**
     * @return at most one process which has the most minimum number
     *  among trusted processes
     */
    public int getTrustedProcess(int n) {
        return 0;
    }

    public void setListener(OmegaFailureDetectorListener listener) {
        // do nothing
        // the listener never needs to be notified
    }

}








