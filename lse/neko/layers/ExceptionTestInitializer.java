package lse.neko.layers;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.NekoThread;

// other imports:
import org.apache.java.util.Configurations;


public class ExceptionTestInitializer
    implements NekoProcessInitializer
{
    private static final String CF_CRASH = "process.crash";

    public void init(NekoProcess process, Configurations config) {

        final int id = config.getInteger(CF_CRASH);
        if (id < 0 || id >= process.getN()) {
            throw new RuntimeException("The value of " + CF_CRASH
                                       + " is not a valid process ID!");
        }

        if (id == process.getID()) {
            NekoThread thread =
                new NekoThread("crasher") {
                    public void run() {
                        throw new RuntimeException("Process #" + id
                                                   + " raised an exception");
                    }
                };
            thread.start();
        }
    }


}
