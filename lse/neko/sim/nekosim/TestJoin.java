package lse.neko.sim.nekosim;

// java imports:
import java.util.ArrayList;
import java.util.List;

// lse.neko imports:
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.util.Timer; // ambiguous with: java.util.Timer
import lse.neko.util.TimerTask; // ambiguous with: java.util.TimerTask

// other imports:
import org.apache.java.util.Configurations;
import org.apache.java.util.ExtendedProperties;


/**
 * Unit test for <code>NekoThread.join</code>.
 * @see lse.neko.sim.nekosim.SimThread
 */
public class TestJoin {

    public static void assertTrue(boolean condition) {
        if (!condition) {
            throw new RuntimeException("Test failed!");
        }
    }

    // constants
    static double[] referenceTimes = {
        0.0,
        2.0,
        3.0,
        5.0
    };

    // records parameters of SchedulerHook calls
    static List times = new ArrayList();

    public static void timestamp() {
        times.add(new Double(NekoSystem.instance().clock()));
    }

    public static void main(String[] args) {

        // initalize the system
        final NekoSimSystem system =
            new NekoSimSystem(new Configurations(new ExtendedProperties()));

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
        }, 9e9);

        timer.schedule(new TimerTask() {
            public void run() {
                doTest();
            }
        }, 0);

        system.run();
    }

    public static void doTest() {
        NekoThread t =
            new NekoThread() {
                public void run() {
                    try {
                        sleep(2.0);
                    } catch (InterruptedException ex) {
                        assertTrue(false);
                    }
                }
            };
        t.start();
        timestamp();
        try {
            t.join();
        } catch (InterruptedException ex) {
            assertTrue(false);
        }
        timestamp();
        NekoThread t2 =
            new NekoThread() {
                public void run() {
                    try {
                        sleep(2.0);
                    } catch (InterruptedException ex) {
                        assertTrue(false);
                    }
                }
            };
        t2.start();
        try {
            t2.join(1.0);
        } catch (InterruptedException ex) {
            assertTrue(false);
        }
        timestamp();
        NekoThread t3 =
            new NekoThread() {
                public void run() {
                    try {
                        sleep(2.0);
                    } catch (InterruptedException ex) {
                        assertTrue(false);
                    }
                }
            };
        t3.start();
        try {
            t3.join(3.0);
        } catch (InterruptedException ex) {
            assertTrue(false);
        }
        timestamp();
    }

}

