package lse.neko;

/**
 * An interface that lists methods similar to the static methods of
 * <code>java.lang.Thread</code>. <code>NekoThread</code> implements
 * these methods as static methods.
 *
 * @see java.lang.Thread
 * @see NekoThread
 * @see NekoSystem#getThreadStatic
 */
public interface NekoThreadStaticInterface {

    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return  the currently executing thread.
     */
    NekoThread currentThread();

    /**
     * Causes the currently executing thread object to temporarily pause
     * and allow other threads to execute.
     */
    void yield();

    /**
     * Causes the currently executing thread to sleep (cease execution)
     * for the specified number of milliseconds.
     * Note that the number of milliseconds is a double (unlike
     * in <code>java.lang.Thread</code>) and
     * can thus be a fraction.
     * <p>
     * Note: sleep does not throw InterruptedException as does
     * java.lang.Thread.sleep. FIXME: is there a good reason
     * for this difference?
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @exception  IllegalArgumentException  if the value of millis is
     *             negative.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        NekoObjectInterface#doNotify()
     */
    void sleep(double millis)
        throws InterruptedException;

    /**
     * Tests whether the current thread has been interrupted.  The
     * <i>interrupted status</i> of the thread is cleared by this method.  In
     * other words, if this method were to be called twice in succession, the
     * second call would return false (unless the current thread were
     * interrupted again, after the first call had cleared its interrupted
     * status and before the second call had examined it).
     *
     * @return  <code>true</code> if the current thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see java.lang.Thread#isInterrupted()
     */
    boolean interrupted();

}

