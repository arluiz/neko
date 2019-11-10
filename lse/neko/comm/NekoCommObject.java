package lse.neko.comm;

// lse.neko imports:
import lse.neko.NekoObjectInterface;


/**
 * Delegates the calls to the corresponding methods of
 * java.lang.Object.
 */
public class NekoCommObject
    implements NekoObjectInterface
{

    public void doNotify() {
        lock.notify();
    }

    public void doNotifyAll() {
        lock.notifyAll();
    }

    public void doWait()
        throws InterruptedException
    {
        lock.wait();
    }

    public void doWait(double timeout)
        throws InterruptedException
    {
        long nanoDuration = (long) (Math.ceil(timeout * 1000000));
        long millis = nanoDuration / 1000000L;
        int nanos = (int) (nanoDuration % 1000000L);
        lock.wait(millis, nanos);
    }

    public NekoCommObject() {
        lock = this;
    }

    public NekoCommObject(Object lock) {
        this.lock = lock;
    }

    private Object lock;

}
