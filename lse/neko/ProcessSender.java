package lse.neko;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


/**
 * A microprotocol that intercepts all messages going out of a
 * process.  It inserts the ID of the sender protocol into the
 * message.  It can also log messages.
 *
 * <p>
 *
 * There is one such microprotocol for each (process, network) pair,
 * and the process provides access to them.
 *
 * @see NekoProcess#getNetworks
 * @see NekoProcess#getDefaultNetwork
 */
public class ProcessSender
    extends ProtocolImpl
    implements SenderInterface
{
    private NekoProcess process;

    public ProcessSender(NekoProcess process) {
        this.process = process;
    }

    protected SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private static Logger messageLogger =
        NekoLogger.getLogger("messages");

    private boolean shouldDropMessages = false;

    /**
     * Discard all messages that pass through in either direction.
     * The shutdown code calls this function.
     */
    public void dropMessages() {
        shouldDropMessages = true;
    }

    public static final String LOG_SEND = "s";
    public static final String LOG_SEND_FINISHED = "sf";

    public void send(NekoMessage m) {
        // Only threads of this process are allowed to call send().
        // Check this.
        if (NekoThread.currentThread().getProcess() != process) {
            throw new RuntimeException("Only threads of this process (p"
                                       + process.getID()
                                       + ") should call send()!");
        }

        m.setSource(process.getID());

        // log sending
        if (messageLogger.isLoggable(Level.FINE)) {
            messageLogger.log(Level.FINE,
                              "",
                              new Object[] {
                                  new NekoMessageEvent(LOG_SEND, m)
                              });
        }

        if (!shouldDropMessages) {
            sender.send(m);
        }

        // log sending finished
        if (messageLogger.isLoggable(Level.FINER)) {
            messageLogger.log(Level.FINER,
                              "",
                              new Object[] {
                                  new NekoMessageEvent(LOG_SEND_FINISHED, m)
                              });
        }

    }

}
