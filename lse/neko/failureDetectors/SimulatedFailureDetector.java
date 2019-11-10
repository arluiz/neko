package lse.neko.failureDetectors;

// java imports:
import java.io.Serializable;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ReceiverInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.Timer;
import lse.neko.util.TimerTask;
import lse.neko.util.Util;


public class SimulatedFailureDetector
    extends FailureDetector
    implements ReceiverInterface
{
    private double detectionTime;
    private TimerTask[] suspectTasks;

    public SimulatedFailureDetector(NekoProcess process,
                                    double detectionTime)
    {
        super(process);

        if (Double.isNaN(detectionTime) || detectionTime < 0) {
            throw new IllegalArgumentException();
        }
        this.detectionTime = detectionTime;

        int n = NekoSystem.instance().getProcessNum();
        suspectTasks = new SuspectTask[n];
        for (int i = 0; i < n; i++) {
            suspectTasks[i] = new SuspectTask(i);
        }
    }

    public static final int CRASH = 1235;
    static {
        MessageTypes.instance().register(CRASH, "CRASH");
    }

    private class SuspectTask extends TimerTask {

        private int proc;

        public SuspectTask(int proc) {
            this.proc = proc;
        }

        public void run() {
            suspect(proc);
        }

        public String toString() {
            return SimulatedFailureDetector.this + "-SuspectTask-" + proc;
        }
    }

    public static class CrashContent
        implements Serializable
    {

        private boolean isCrash;
        private int processId;
        private double time;

        public CrashContent(boolean isCrash, int processId, double time) {
            this.isCrash = isCrash;
            this.processId = processId;
            this.time = time;
        }

        public boolean getIsCrash() { return isCrash; }
        public int getProcessId() { return processId; }
        public double getTime() { return time; }

        public String toString() {
            return "isCrash=" + isCrash + " processId=" + processId
                + " time=" + Util.timeToString(time);
        }
    }

    public void deliver(NekoMessage m) {
        if (m.getType() == CRASH) {
            CrashContent content = (CrashContent) m.getContent();
            int processId = content.getProcessId();
            if (content.getIsCrash()) {
                Timer timer = NekoSystem.instance().getTimer();
                timer.schedule(suspectTasks[processId],
                               content.getTime() + detectionTime
                               - NekoSystem.instance().clock());
            } else {
                unsuspect(processId);
                suspectTasks[processId].cancel();
            }
            return;
        }
        throw new UnexpectedMessageException(m);
    }
}
