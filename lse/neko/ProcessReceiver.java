package lse.neko;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;

/**
 * A microprotocol that intercepts all messages going into a process.
 * It puts the process into the context of the thread delivering the
 * message so that microprotocols know which process they are in.  It
 * can also log messages.
 *
 * <p>
 *
 * There is one such microprotocol for each process-network pair,
 * and the process provides access to them.
 *
 * @see NekoProcess#getReceivers
 */
public class ProcessReceiver
    extends ProtocolImpl
    implements ReceiverInterface
{
    private NekoProcess process;

    public ProcessReceiver(NekoProcess process) {
        this.process = process;
    }

    protected ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
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

    public static final String LOG_RECEIVE = "r";
    public static final String LOG_RECEIVE_FINISHED = "rf";

    public void deliver(NekoMessage m) {
        // The thread that deliver messages to this process
        // is attached to this process.
        NekoThread.currentThread().setProcess(process);

        // log delivering
        if (messageLogger.isLoggable(Level.FINE)) {
            messageLogger.log(Level.FINE,
                              "",
                              new Object[] {
                                  new NekoMessageEvent(LOG_RECEIVE, m)
                              });
        }

        if (!shouldDropMessages) {
            receiver.deliver(m);
        } else if (m.getType() == MessageTypeConst.STOP) {
            receiver.deliver(m);
        }

        // log delivering finished
        if (messageLogger.isLoggable(Level.FINER)) {
            messageLogger.log(Level.FINER,
                              "",
                              new Object[] {
                                  new NekoMessageEvent(LOG_RECEIVE_FINISHED, m)
                              });
        }
    }
}
