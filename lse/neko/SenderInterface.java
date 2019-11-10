package lse.neko;

/**
 * An interface to send outgoing messages.
 */
public interface SenderInterface
    extends Protocol
{

    /**
     * Sends a message.
     *
     * @param m the outgoing message.
     */
    void send(NekoMessage m);

}
