package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.Protocol;

public interface OmegaFailureDetectorInterface
    extends Protocol
{

    /**
     * @return at most one process which has the most minimum number
     *  among trusted processes
     */
    int getTrustedProcess(int n);
    // FIXME: why the n parameter?

    void setListener(OmegaFailureDetectorListener listener);

}








