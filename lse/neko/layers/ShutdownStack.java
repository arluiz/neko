package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypeConst;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ProcessReceiver;
import lse.neko.ProcessSender;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.comm.CommNetwork;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.failureDetectors.Heartbeat;
import lse.neko.util.logging.NekoLogger;


/**
 * The shutdown protocol.
 * Shutdown goes through Phases k, k-1, ..., 1, 0 (k &gt;= 2).
 * Entering each phase is associated with actions; see enterPhase().
 * Phase k is uncoordinated.
 * Phase i-1 is entered when the process knows that all processes
 * executed the actions for Phase i.
 *<br>
 * The protocol uses SHUTDOWN messages
 * which hold the number of the phase the sender is in.
 * Shutdown is started when the NekoProcess delivers a
 * SHUTDOWN message of Phase k to this protocol.
 */
public class ShutdownStack
    extends ProtocolImpl
    implements ReceiverInterface, FailureDetectorListener
{

    private NekoProcess process;

    public ShutdownStack(NekoProcess process,
                         Heartbeat fd,
                         CommNetwork controlNetwork)
    {
        this.process = process;
        // init data members
        int n = process.getN();
        me = process.getID();
        awareProcesses = new boolean[n];
        allButMe = new int[n - 1];
        for (int i = 0; i < allButMe.length; i++) {
            allButMe[i] = (i < me) ? i : i + 1;
        }
        this.fd = fd;
        this.controlNetwork = controlNetwork;

    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private int me;
    private int[] allButMe;

    private Heartbeat fd;
    private CommNetwork controlNetwork;

    private int phase = Integer.MAX_VALUE;

    public static final double SHUTDOWN_TIMEOUT = 5000;

    public void statusChange(boolean isSuspected, int p) {
        if (isSuspected) {
            maybeNewPhase();
        }
    }

    public void deliver(NekoMessage m) {

        logger.log(Level.FINE, "delivering {0}", m);

        if (m.getType() != MessageTypeConst.SHUTDOWN) {
            throw new UnexpectedMessageException(m);
        }

        Content content = (Content) m.getContent();
        int phaseInMessage = content.getPhase();
        if (content.getErrorMessage() != null) {
            synchronized (this) {
                if (errorMessage == null) {
                    errorMessage = content.getErrorMessage();
                    // XXX: why is the global logger used
                    // rather than the local one?
                    Logger globalLogger = NekoLogger.getLogger("global.neko");
                    globalLogger.severe(errorMessage);
                }
            }
        }

        if (phaseInMessage < phase) {
            clearAwareProcesses();
            if (phase == Integer.MAX_VALUE) {
                phase = phaseInMessage + 1;
                // start the failure detector
                fd.useApplicationMessages(true);
                fd.setParameters(Double.MAX_VALUE,
                                 SHUTDOWN_TIMEOUT);
                logger.fine("Failure detector started");
            }
            for (int i = phase - 1; i >= phaseInMessage; i--) {
                enterPhase(i);
            }
            phase = phaseInMessage;
        }

        if (phaseInMessage == phase) {
            registerAwareProcess(m.getSource());
        }

    }

    private boolean[] awareProcesses;

    private synchronized void clearAwareProcesses() {
        for (int i = 0; i < awareProcesses.length; i++) {
            awareProcesses[i] = false;
        }
        awareProcesses[me] = true;
    }

    private void registerAwareProcess(int id) {
        synchronized (this) {
            awareProcesses[id] = true;
        }
        maybeNewPhase();
    }

    /**
     * A new phase can start if each process is
     * either aware of the current phase or is suspected.
     */
    private void maybeNewPhase() {

        synchronized (this) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("maybeNewPhase()");
                StringBuffer sb = new StringBuffer();
                sb.append("  processes aware of the current phase: ");
                for (int i = 0; i < awareProcesses.length; i++) {
                    sb.append((awareProcesses[i]) ? "X" : "-");
                }
                logger.fine(sb.toString());
                sb = new StringBuffer();
                sb.append("  processes suspected:                  ");
                for (int i = 0; i < awareProcesses.length; i++) {
                    sb.append((fd.isSuspected(i)) ? "X" : "-");
                }
                logger.fine(sb.toString());
            }

            for (int i = 0; i < awareProcesses.length; i++) {
                if (!awareProcesses[i] && !fd.isSuspected(i)) {
                    return;
                }
            }

            // new phase
            logger.fine("  new phase");
        }

        int[] to = { process.getID() };
        String em;
        synchronized (this) {
            em = errorMessage;
        }
        NekoMessage m = new NekoMessage(process.getID(),
                                        to,
                                        getId(),
                                        new Content(phase - 1, em),
                                        MessageTypeConst.SHUTDOWN);
        fd.deliver(m);
    }

    private void enterPhase(int aPhase) {

        logger.log(Level.FINE, "Entering Phase {0,number,#} of shutdown",
                   new Integer(aPhase));

        SenderInterface[] nets = NekoSystem.instance().getNetworks();
        for (int i = 0; i < nets.length; i++) {
            SenderInterface net = nets[i];
            if (net instanceof CommNetwork) {
                ((CommNetwork) net).shutdown(aPhase);
            }
        }

        // FIXME: network errors are printed twice, for the control
        // network detects them also, independently from the network
        // that detected the error first.

        if (aPhase <= 0) {
            logger.fine("Exiting");
            controlNetwork.shutdown(aPhase);
            // Needed when Neko is run with hprof
            // to get realistic SITES information
            System.gc();
            System.exit((errorMessage == null) ? 0 : 1);
        } else if (aPhase == 1) {
            // nothing
        } else if (aPhase == 2) {
            ProcessReceiver[] receivers = process.getReceivers();
            for (int i = 0; i < receivers.length; i++) {
                ProcessReceiver aReceiver = receivers[i];
                aReceiver.dropMessages();
            }
            SenderInterface[] senders = process.getNetworks();
            for (int i = 0; i < senders.length; i++) {
                ProcessSender aSender = (ProcessSender) senders[i];
                aSender.dropMessages();
            }
        }
        String em;
        synchronized (this) {
            em = errorMessage;
        }
        NekoMessage m = new NekoMessage(process.getID(),
                                        allButMe,
                                        getId(),
                                        new Content(aPhase, em),
                                        MessageTypeConst.SHUTDOWN);
        sender.send(m);
    }

    private String errorMessage = null;

    public static class Content
        implements Serializable
    {
        private int phase;
        private String message;

        /**
         * @param phase Number of shutdown phase.
         * @param message Error message, or <code>null</code> if
         * the shutdown is not due to an error.
         */
        public Content(int phase, String message) {
            this.phase = phase;
            this.message = message;
        }

        public int getPhase() {
            return phase;
        }

        public String getErrorMessage() {
            return message;
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(ShutdownStack.class.getName());
}

