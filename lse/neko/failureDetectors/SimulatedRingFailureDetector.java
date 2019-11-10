package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ReceiverInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.Timer;
import lse.neko.util.TimerTask;


public class SimulatedRingFailureDetector
    extends RingFailureDetector
    implements ReceiverInterface
{
    private double detectionTime;
    private TimerTask suspectTask;

    private int predecessor;

    public SimulatedRingFailureDetector(NekoProcess process,
                                    double detectionTime)
    {
        super(process);

        if (Double.isNaN(detectionTime) || detectionTime < 0) {
            throw new IllegalArgumentException();
        }
        this.detectionTime = detectionTime;

        suspectTask = new SuspectTask();

        predecessor = (process.getID() - 1 + process.getN()) % process.getN();
    }

    private class SuspectTask extends TimerTask {

        public void run() {
            suspect();
        }

        public String toString() {
            return SimulatedRingFailureDetector.this
                + "-SuspectTask-" + predecessor;
        }
    }

    public void deliver(NekoMessage m) {
        if (m.getType() == SimulatedFailureDetector.CRASH) {
            SimulatedFailureDetector.CrashContent content =
                (SimulatedFailureDetector.CrashContent) m.getContent();
            int processId = content.getProcessId();
            if (processId != predecessor) {
               // ignore messages that don't concern me
               return;
            } else if (content.getIsCrash()) {
                Timer timer = NekoSystem.instance().getTimer();
                timer.schedule(suspectTask,
                               content.getTime() + detectionTime
                               - NekoSystem.instance().clock());
            } else { // getIsCrash == false && processId == predecessor
                unsuspect();
                suspectTask.cancel();
            }
            return;
        }
        throw new UnexpectedMessageException(m);
    }
}
