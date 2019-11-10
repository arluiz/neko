package lse.neko.util;

// java imports:
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.NekoThread;
// import lse.neko.util.Timer;
// ambiguous with: java.util.Timer
// import lse.neko.util.TimerTask;
// ambiguous with: java.util.TimerTask

// other imports:
import org.apache.java.util.Configurations;


public class TimerTest
    extends NekoThread
    implements NekoProcessInitializer
{

    public void init(NekoProcess process, Configurations config) {
        start();
    }

    private List list = new LinkedList();

    public void run() {
        list.add(new Double(clock()));
        final Timer timer = new Timer();
        final TimerTask task =
            new TimerTask() {
                public void run() {
                    if (scheduledExecutionTime() < 200) {
                        timer.schedule(this, 20);
                    }
                    list.add(new Double(clock()));
                }
            };
        try {
            sleep(4);
        } catch (InterruptedException ex) {
        }
        timer.schedule(task, 6);
        try {
            timer.schedule(task, 15);
        } catch (IllegalStateException ex) {
            list.add(ex.toString());
        }
        try {
            sleep(100);
        } catch (InterruptedException ex) {
        }
        task.cancel();
        timer.schedule(task, 200);
        try {
            sleep(100);
        } catch (InterruptedException ex) {
        }
        task.cancel();
        try {
            sleep(1000);
        } catch (InterruptedException ex) {
        }

        List referenceList = Arrays.asList(new Object[] {
            new Double(0.0),
            "java.lang.IllegalStateException: Task already scheduled",
            new Double(10.0),
            new Double(30.0),
            new Double(50.0),
            new Double(70.0),
            new Double(90.0)
        });
        if (!list.equals(referenceList)) {
            System.out.println("Test failed");
            System.out.println("The result should be " + referenceList);
            System.out.println("The result is " + list);
            System.exit(1);
        }
        System.out.println("Test successful");
    }

}
