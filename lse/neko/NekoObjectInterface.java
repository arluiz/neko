package lse.neko;

/**
 * Replacements for methods of java.lang.Object
 * related to synchronization.
 * Needed because Object's methods are final.
 * These methods, just like Object's methods,
 * can only be used in a synchronized block.
 */
public interface NekoObjectInterface {

    /**
     * Replaces Object.notify().
     */
    void doNotify();

    /**
     * Replaces Object.notifyAll().
     */
    void doNotifyAll();

    /**
     * Replaces Object.wait().
     */
    void doWait()
        throws InterruptedException;

    /**
     * Replaces Object.wait(double).
     */
    void doWait(double timeout)
        throws InterruptedException;

}
