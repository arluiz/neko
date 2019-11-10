package lse.neko;

/**
 */

public class PullProtocol
    extends ProtocolImpl
    implements PullInterface, ReceiverInterface
{
    protected NekoMessageQueue messageQueue =
        new NekoMessageQueue();

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




