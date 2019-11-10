package lse.neko.comm;

// lse.neko imports:
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.NekoThreadInterface;


public class NekoCommThread
    extends Thread
    implements NekoThreadInterface
{

    public NekoCommThread(NekoThread peer,
                          Runnable runnable,
                          String name)
    {
        super(runnable, name);
        this.peer = peer;
    }

    private NekoThread peer;

    public NekoThread getPeer() {
        return peer;
    }

    public void start() {
        NekoCommSystem system = (NekoCommSystem) NekoSystem.instance();
        if (!system.registerThread(this)) {
            super.start();
        }
    }

    public void join(double duration) throws InterruptedException {
        join(this, duration);
    }

    static void join(Thread thread, double duration)
        throws InterruptedException
    {
        long nanoDuration = (long) (Math.ceil(duration * 1000000));
        long millis = nanoDuration / 1000000L;
        int nanos = (int) (nanoDuration % 1000000L);
        thread.join(millis, nanos);
    }

}
