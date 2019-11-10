package lse.neko.failureDetectors;

// java imports:
import java.util.Random;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.util.Timer; // ambiguous with: java.util.Timer
import lse.neko.util.TimerTask; // ambiguous with: java.util.TimerTask

// other imports:
import org.apache.java.util.Configurations;

public class SimulatedDetectorWithWrongSuspicions
    extends FailureDetector
{
    private final double tMR;
    private final double tM;

    private final Timer timer;

    private final Random random;

    private static final long DEFAULT_SEED = System.currentTimeMillis();
    public static final String CF_SEED =
        "SimulatedDetectorWithWrongSuspicions.seed";

    public SimulatedDetectorWithWrongSuspicions(NekoProcess process,
                                                int[] group,
                                                double tMR,
                                                double tM)
    {
        super(process);

        if (Double.isNaN(tMR) || tMR <= 0
            || Double.isNaN(tMR) || tM < 0)
        {
            throw new IllegalArgumentException();
        }

        this.tMR = tMR;
        this.tM = tM;

        timer = NekoSystem.instance().getTimer();

        int processId = process.getID();

        // initialize the random number generator.
        final long seed =
            NekoSystem.instance().getConfig()
                .getLong(CF_SEED, DEFAULT_SEED);
        // The seed should be different on each process
        random = new Random(seed + processId);
        Configurations config = NekoSystem.instance().getConfig();
        if (!config.getBoolean("random.backwardCompatible", false)) {
            random.nextDouble();
        }

        for (int i = 0; i < group.length; i++) {
            int p = group[i];
            if (p != processId) {
                new Detector(p);
            }
        }
    }

    private double nextExponential(double mean) {
        double r = random.nextDouble();
        if (r == 0) {
            r = Double.MIN_VALUE;
        }
        return -Math.log(r) * mean;
    }

    private class Detector {

        private int p;

        public Detector(int p) {
            this.p = p;
            timer.schedule(suspectTask, nextExponential(tMR));
        }

        private final TimerTask suspectTask = new TimerTask() {
                public void run() { runSuspect(); }
            };

        private final TimerTask unsuspectTask = new TimerTask() {
                public void run() { runUnsuspect(); }
            };

        private void runSuspect() {
            suspect(p);
            timer.schedule(suspectTask, nextExponential(tMR));
            unsuspectTask.cancel();
            timer.schedule(unsuspectTask, nextExponential(tM));
        }

        private void runUnsuspect() {
            unsuspect(p);
        }
    }

}
