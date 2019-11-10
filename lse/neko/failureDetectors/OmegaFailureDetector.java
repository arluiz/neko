package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;


public class OmegaFailureDetector
    extends ProtocolImpl
    implements OmegaFailureDetectorInterface, FailureDetectorListener
{

    private int leader;

    private NekoProcess process;

    public OmegaFailureDetector(NekoProcess process) {
        this.process = process;
    }

    private FailureDetectorInterface failureDetector;

    public void setFailureDetector(FailureDetectorInterface failureDetector) {
        this.failureDetector = failureDetector;
    }

    private OmegaFailureDetectorListener listener;

    public void setListener(OmegaFailureDetectorListener listener) {
        this.listener = listener;
    }

    public void init() {
        OmegaFailureDetectorListener tmp = listener;
        listener = null;
        electLeader();
        listener = tmp;
    }

    public synchronized int getTrustedProcess(int number) {
        return leader;
    }

    public void statusChange(boolean isSuspected, int p) {
        electLeader();
    }

    private void electLeader() {

        int pnb;
        for (pnb = 0; pnb < process.getN(); pnb++) {
            if (!failureDetector.isSuspected(pnb)) {
                break;
            }
        }

        if (pnb >= process.getN()) {
            // all processes are suspected
            pnb = 0;
        }

        if (pnb != leader) {
            leader = pnb;
            // FIXME: the following "if" is not thread-safe
            if (listener != null) {
                listener.newLeader(leader);
            }
        }
    }

}
