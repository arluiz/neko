package lse.neko;

/**
 * An interface to handle incoming messages.
 */
public interface ReceiverInterface
    extends Protocol
{

    /**
     * Handles incoming messages.
     *
     * @param m the incoming message.
     */
    void deliver(NekoMessage m);

}
