package lse.neko.sim.nekosim;

// lse.neko imports:
import lse.neko.NekoObjectInterface;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.NekoThreadInterface;
import lse.neko.sim.AbstractNekoSimSystem;

// lse.neko.util imports:
import lse.neko.util.TimerTask;

// other imports:
import org.apache.java.util.Configurations;


public class NekoSimSystem
    extends AbstractNekoSimSystem
{

    protected NekoThreadInterface createPeerThread(NekoThread thread,
                                                   Runnable runnable,
                                                   String name)
    {
        return new SimThread(thread, runnable, name);
    }

    public NekoObjectInterface createObject() {
        return new SimObject();
    }

    public NekoObjectInterface createObject(Object object) {
        return new SimObject(object);
    }

    public NekoSimSystem(Configurations config) {
        super(config, new NekoSimThreadStatic());
    }

    // FIXME: initialization might need to be reworked
    // because networks can send messages too early,
    // before the protocol stack is constructed

    public synchronized void run() {
        // have the simulator start the real initialization
        NekoSystem.instance().getTimer().schedule(new TimerTask() {
            public void run() {
                NekoSimSystem.super.run();
            }
        }, 0);
        // start the simulator
        SimScheduler.yieldForever();
    }

    protected void start() {
        // does nothing
    }

    public double clock() {
        return SimScheduler.clock();
    }

    /**
     * Registers a hook that modifies the behavior of the scheduler.
     * <code>null</code> means that no hook should be registered.
     *
     * @see SchedulerHook
     */
    public void registerSchedulerHook(SchedulerHook hook) {
        SimScheduler.registerHook(hook);
    }

    public double getInitialClock() {
        return 0;
    }

}
