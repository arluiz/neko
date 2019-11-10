package lse.neko.comm;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoThread;
import lse.neko.NekoThreadStaticInterface;
import lse.neko.util.logging.NekoLogger;


class NekoCommThreadStatic
    implements NekoThreadStaticInterface
{

    public NekoThread currentThread() {
        // returns the current Java thread's peer NekoThread
        Thread current = Thread.currentThread();
        if (current instanceof NekoCommThread) {
            NekoCommThread t = (NekoCommThread) current;
            return t.getPeer();
        } else {
            // Threads which are not NekoCommThreads have the peer
            // stored in a ThreadLocal variable
            return (NekoThread) peer.get();
        }
    }

    private static void copyContext(NekoThread parent,
                                    NekoThread child)
    {
        child.setProcess(parent.getProcess());
    }

    /**
     * This <code>InheritableThreadLocal</code> object
     * makes sure that the process context (accessible through
     * <code>NekoThread.getProcess()</code>) is propagated from
     * parent to child.
     */
    private static ThreadLocal peer =
        new InheritableThreadLocal() {

            protected synchronized Object initialValue() {
                // parent is non-NekoCommThread with no context
                // so no context is propagated
                Thread current = Thread.currentThread();
                if (current instanceof NekoCommThread) {
                    // the parent is a NekoCommThread,
                    // so the child must be a NekoCommThread
                    // return null: no ThreadLocal variable needed
                    logger.log(Level.FINE, "{0} initialValue returns null",
                               current);
                    return null;
                } else {
                    // the parent is not a NekoCommThread
                    // return a wrapping NekoThread
                    logger.log(Level.FINE, "{0} initialValue returns non-null",
                               current);
                    return new WrappingNekoThread(current);
                }
            }

            protected synchronized Object childValue(Object parentValue) {
                // parent is non-NekoCommThread with context
                // so the context is propagated
                Thread current = Thread.currentThread();
                logger.log(Level.FINE, "{0} childValue", current);
                NekoThread parentPeer = (NekoThread) parentValue;
                // return a wrapping NekoThread
                NekoThread r = new WrappingNekoThread(current);
                copyContext(parentPeer, r);
                return r;
            }

        };

    public void yield() {
        Thread.yield();
    }

    /**
     * Sleeps for the specified duration.
     *
     * @param duration duration after which the thread wakes up
     */
    public void sleep(double duration) throws InterruptedException {

        long nanoDuration = (long) (Math.ceil(duration * 1000000));
        long millis = nanoDuration / 1000000L;
        int nanos = (int) (nanoDuration % 1000000L);
        Thread.sleep(millis, nanos);
    }

    public boolean interrupted() {
        return Thread.interrupted();
    }

    private static final Logger logger =
        NekoLogger.getLogger(NekoCommThreadStatic.class.getName());
}
