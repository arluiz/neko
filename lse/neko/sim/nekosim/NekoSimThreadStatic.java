package lse.neko.sim.nekosim;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoThread;
import lse.neko.NekoThreadStaticInterface;
import lse.neko.util.logging.NekoLogger;


class NekoSimThreadStatic
    implements NekoThreadStaticInterface
{

    public NekoThread currentThread() {
        SimThread t = SimScheduler.currentThread();
        if (t == null) {
            if (starterThread == null) {
                starterThread =
                    new NekoThread((lse.neko.NekoThreadInterface) null) {
                        public int getPriority() {
                            return Thread.NORM_PRIORITY;
                        }
                        public String getName() {
                            return "main";
                        }
                    };
            }
            return starterThread;
        }
        return SimScheduler.currentThread().getNekoPeer();
    }

    /**
     * This dummy NekoThread represents the main thread that runs
     * the Neko startup code. Its sole purpose is to enable the
     * creation of new NekoThreads.
     */
    private static NekoThread starterThread = null;

    public void yield() {
        try {
            sleep(0);
        } catch (InterruptedException ex) {
            // ignore
        }
    }

    public void sleep(double duration)
        throws InterruptedException
    {
        if (duration < 0) {
            throw new IllegalArgumentException("duration < 0");
        }
        double wakeUpTime = SimScheduler.clock() + duration;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("sleeping until " + wakeUpTime);
        }
        SimScheduler.schedule(SimScheduler.currentThread(), wakeUpTime);
        SimScheduler.yield();
    }

    public boolean interrupted() {
        throw new RuntimeException("Not implemented");
    }

    public String toString() {
        return getClass().getName();
    }

    private static final Logger logger =
        NekoLogger.getLogger(NekoSimThreadStatic.class.getName());

}
