package lse.neko;


/**
 * Exception thrown when the <code>deliver</code> method of a
 * microprotocol does not recognize a message type.  This usually
 * indicates a bug, hence the exception is unchecked.
 *
 * @see ReceiverInterface#deliver
 * @see NekoMessage#getType
 */
public class UnexpectedMessageException
    extends RuntimeException
{

    private final NekoMessage message;

    /**
     * Constructs a new exception.
     *
     * @param message the unexpected message.
     */
    public UnexpectedMessageException(NekoMessage message) {
        this.message = message;
    }

    public String getMessage() {
        return "Unexpected message: " + message;
    }

}
