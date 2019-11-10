package lse.neko;

/**
 * A <i>thread</i> is a thread of execution in a program. Neko allows
 * an application to have multiple threads of execution running
 * concurrently. In a distributed execution, the Neko thread is
 * backed by a Java thread, and in a simulation, by a thread
 * of the simulation package.
 *
 * This interface has been modeled after <code>java.lang.Thread</code>,
 * with a few differences:
 * <ul>
 * <li>Times are specified as variables of type double, in milliseconds.
 *   This affects the prototype of methods like sleep.</li>
 * <li>Priorities are interpreted differently when doing simulation:
 *   the priorities of threads determine their execution order
 *   if they are activated at the same time.</li>
 * </ul>
 *
 * Static methods of <code>java.lang.Thread</code> are not part of
 * this interface (as an interface cannot have static methods).
 * <code>NekoThread</code> implements them.
 * <p>
 * Also, the following parts of <code>java.lang.Thread</code>'s
 * interface are missing:
 * <ul>
 * <li>Daemon threads. Daemon threads are just a convenient
 *   mechanism to specify when the application should quit.
 *   In Neko, such a mechanism is not sufficient because the application is
 *   distributed. Applications have to use
 *   <code>NekoProcess.shutdown(int)</code> to quit.</li>
 * <li>Functionality related to security, classloaders, thread groups
 *   and <code>ThreadLocal</code>s.
 *   This functionality did not seem useful enough or would have been
 *   too difficult to design and implement. This includes the methods
 *   <code>getThreadGroup()</code>, <code>activeCount()</code>,
 *   <code>enumerate(Thread[])</code>, <code>checkAccess()</code>,
 *   <code>getContextClassLoader()</code> and
 *   <code>setContextClassLoader(ClassLoader cl)</code>.</li>
 * <li>Deprecated methods are missing and will never be implemented:
 *   <code>stop()</code>,
 *   <code>stop(Throwable)</code>,
 *   <code>destroy()</code>,
 *   <code>suspend()</code>,
 *   <code>resume()</code> and <code>countStackFrames()</code>.</li>
 * </ul>
 *
 * @see java.lang.Thread
 */

public interface NekoThreadInterface {

    /**
     * Causes this thread to begin execution.
     * The result is that two threads are running concurrently: the
     * current thread (which returns from the call to the
     * <code>start</code> method) and the other thread (which executes its
     * <code>run</code> method).
     *
     * @exception  IllegalThreadStateException  if the thread was already
     *               started.
     * @see        java.lang.Thread#start()
     */
    void start();

    /**
     * Interrupts this thread.
     *
     * <p> If this thread is blocked in an invocation of the {@link
     * NekoObjectInterface#doWait() doWait()},
     * or {@link NekoObjectInterface#doWait(double) doWait(double)}
     * methods of the {@link NekoObjectInterface}
     * interface, or of the {@link #join()}, {@link #join(double)},
     * or {@link NekoThreadStaticInterface#sleep(double)},
     * methods of this class, then its interrupt status will be cleared and it
     * will receive an {@link InterruptedException}.
     *
     * <p> If this thread is blocked in an I/O operation upon an
     * {@link java.nio.channels.InterruptibleChannel <code>interruptible
     *  channel</code>} then the channel will be closed, the thread's interrupt
     * status will be set, and the thread will receive a {@link
     * java.nio.channels.ClosedByInterruptException}.
     *
     * <p> If this thread is blocked in a {@link java.nio.channels.Selector}
     * then the thread's interrupt status will be set and it will return
     * immediately from the selection operation, possibly with a non-zero
     * value, just as if the selector's {@link
     * java.nio.channels.Selector#wakeup wakeup} method were invoked.
     *
     * <p> If none of the previous conditions hold then this thread's interrupt
     * status will be set. </p>
     */
    void interrupt();

    /**
     * Tests whether this thread has been interrupted.  The <i>interrupted
     * status</i> of the thread is unaffected by this method.
     *
     * @return  <code>true</code> if this thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see     java.lang.Thread#interrupted()
     */
    boolean isInterrupted();

    /**
     * Tests if this thread is alive. A thread is alive if it has
     * been started and has not yet died.
     *
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     */
    boolean isAlive();

    /**
     * Changes the priority of this thread to the specified
     * new priority.
     * The priority of this thread is set to the smaller of
     * the specified <code>newPriority</code> and the maximum permitted
     * priority of the thread's thread group.
     *
     * @param newPriority priority to set this thread to
     * @exception  IllegalArgumentException  If the priority is not in the
     *               range <code>MIN_PRIORITY</code> to
     *               <code>MAX_PRIORITY</code>.
     * @see        #getPriority
     * @see        java.lang.Thread#setPriority(int)
     * @see        java.lang.Thread#MAX_PRIORITY
     * @see        java.lang.Thread#MIN_PRIORITY
     */
    void setPriority(int newPriority);

    /**
     * Returns this thread's priority.
     *
     * @return  this thread's priority.
     * @see     #setPriority
     * @see     java.lang.Thread#getPriority()
     */
    int getPriority();

    /**
     * Changes the name of this thread to be equal to the argument
     * <code>name</code>.
     *
     * @param      name   the new name for this thread.
     * @see        #getName
     * @see        java.lang.Thread#setName(java.lang.String)
     */
    void setName(String name);

    /**
     * Returns this thread's name.
     *
     * @return  this thread's name.
     * @see     #setName
     * @see     java.lang.Thread#getName()
     */
    String getName();

    /**
     * Waits at most <code>millis</code> milliseconds for this thread to
     * die. A timeout of <code>0</code> means to wait forever.
     *
     * @param      millis   the time to wait in milliseconds.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    void join(double millis) throws InterruptedException;

    /**
     * Waits for this thread to die.
     *
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    void join() throws InterruptedException;

}
