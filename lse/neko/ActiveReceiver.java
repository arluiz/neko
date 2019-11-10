package lse.neko;

/**
 * The default active implementation of <code>Protocol</code>. It has
 * an associated thread and a queue for incoming messages.  Subclass
 * it and override the <code>run</code> method. You can consume
 * messages from the queue by calling <code>receive</code> from within
 * your <code>run</code> method. You might want to override
 * <code>deliver</code> if the default implementation (supplying the
 * queue) is not adequate.
 *
 * <p>
 *
 * You can also subclass {@link ProtocolImpl} and create and manage a
 * {@link NekoThread} within the subclass.
 *
 * @see Protocol
 * @see ProtocolImpl
 */

public abstract class ActiveReceiver
    extends NekoThread
    implements PullInterface, ReceiverInterface
{
    protected NekoMessageQueue messageQueue =
        new NekoMessageQueue();

    protected final NekoProcess process;

    // FIXME: remove the process field from every layer. It gives
    // difficult-to-track bugs (the following getProcess() hid the
    // getProcess() of NekoThread)
/*
    public NekoProcess getProcess() {
        return process;
    }
*/

    public ActiveReceiver(NekoProcess process, String name) {
        super(null, name);
        this.process = process;
    }

    private Object id;

    public void setId(Object id) {
        this.id = id;
    }

    public Object getId() {
        return id;
    }

    public void launch() {
        ProtocolImpl.staticLaunch(this);
        start();
    }

    /**
     * Receives a message. Blocks if no message is available.
     */
    public NekoMessage receive() {
        return messageQueue.get();
    }

    /**
     * Receives a message with a timeout. Blocks waiting for a message for the
     * specified amount of time or until it receives a message, whichever
     * occurs first.
     *
     * @param timeout the operation times out after this amount of time.
     * @return the received message, or null if no message was received before
     *         the timeout.
     */
    public NekoMessage receive(double timeout) {
        return messageQueue.get(timeout);
    }

    public void deliver(NekoMessage m) {
        messageQueue.put(m);
    }

}




