package lse.neko.sim.nekosim;

// java imports:
import java.util.ArrayList;
import java.util.List;

// lse.neko imports:
import lse.neko.util.Timer; // ambiguous with: java.util.Timer
import lse.neko.util.TimerTask; // ambiguous with: java.util.TimerTask

// other imports:
import org.apache.java.util.Configurations;
import org.apache.java.util.ExtendedProperties;


/**
 * Unit test for <code>SchedulerHook</code>.
 * @see SchedulerHook
 */
public class TestSchedulerHook {

    public static void assertTrue(boolean condition) {
        if (!condition) {
            throw new RuntimeException("Test failed!");
        }
    }

    public static void main(String[] args) {

        // initalize the system
        final NekoSimSystem system =
            new NekoSimSystem(new Configurations(new ExtendedProperties()));

        // constants
        final double t1 = 1.0;
        final double t2 = 2.0;
        final double[] referenceTimes = {
            0.0, t2,
            0.0, t1,
            t1, t2
        };

        // records parameters of SchedulerHook calls
        final List times = new ArrayList();

        // schedule the check of times at a late time
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {

                // check if times equals referenceTimes
                List ref = new ArrayList();
                for (int i = 0; i < referenceTimes.length; i++) {
                    ref.add(new Double(referenceTimes[i]));
                }
                assertTrue(times.equals(ref));

                System.out.println("Test successful");
            }
        }, t2);

        // the scheduler hook
        // when called the first time, schedules something before nextTime
        SchedulerHook hook = new SchedulerHook() {
                private boolean called = false;
                public void steppingTime(double nextTime) {
                    double clock = system.clock();
                    if (!called) {
                        called = true;
                        assertTrue(system.clock() == 0.0);
                        timer.schedule(new TimerTask() {
                            public void run() {
                                assertTrue(system.clock() == t1);
                            }
                        }, t1);
                    }
                    times.add(new Double(clock));
                    times.add(new Double(nextTime));
                }
            };

        // register the hook and run the test
        system.registerSchedulerHook(hook);
        system.run();
    }

}
