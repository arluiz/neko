package lse.neko.comm;

// lse.neko imports:
import lse.neko.NekoThread;
import lse.neko.NekoThreadInterface;


/**
 * When the current thread is a non-Neko thread,
 * NekoThread.currentThread() returns an instance of this class. This
 * class is needed because some parts of a Neko application (external
 * packages in the network layer) might create and use instances of
 * java.lang.Thread directly, rather than NekoThread.
 */
class WrappingNekoThread
    extends NekoThread
{

    protected WrappingNekoThread(Thread thread) {
        super((NekoThreadInterface) null);
        this.thread = thread;
    }

    private Thread thread;

    /*
     * Now comes the implementation of NekoThreadInterface.
     * All methods just delegate the actual task to the peer.
     */

    public void start() {
        thread.start();
    }

    public void interrupt() {
        thread.interrupt();
    }

    public boolean isInterrupted() {
        return thread.isInterrupted();
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

    public void setPriority(int newPriority) {
        thread.setPriority(newPriority);
    }

    public int getPriority() {
        return thread.getPriority();
    }

    public void setName(String name) {
        thread.setName(name);
    }

    public String getName() {
        return thread.getName();
    }

    public void join(double duration) throws InterruptedException {
        NekoCommThread.join(thread, duration);
    }

    public void join() throws InterruptedException {
        thread.join();
    }

}

