package lse.neko;

/**
 * An interface that provides pull-style access to messages.
 */
public interface PullInterface
    extends Protocol
{
    /**
     * Receives a message. Blocks if no message is available.
     */
    NekoMessage receive();

    /**
     * Receives a message with a timeout. Blocks waiting for a message for the
     * specified amount of time or until it receives a message, whichever
     * occurs first.
     *
     * @param timeout the operation times out after this amount of time.
     * @return the received message, or null if no message was received before
     *         the timeout.
     */
    NekoMessage receive(double timeout);
}
