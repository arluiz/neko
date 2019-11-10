package lse.neko.failureDetectors;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypeConst;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.util.Timer;
import lse.neko.util.TimerTask;
import lse.neko.util.logging.NekoLogger;


/**
 * A heartbeat Ring failure detector.
 */
public class RingHeartbeat
    extends RingFailureDetector
    implements ReceiverInterface
{

    private double tSend;
    private double tReceive;

    private Timer timer = new Timer();

    /**
     * Task that periodically sends FD_I_M_ALIVE messages to
     * the successor of this process.
     */
    private final SenderTask senderTask = new SenderTask();

    /**
     * Task that waits for FD_I_M_ALIVE messages from the
    * predecessor.
    */
    private SuspectTask suspectTask = new SuspectTask();


    /**
     * Send an FD_I_M_ALIVE message to the successor.
     */
    protected void sendHeartbeat() {
        NekoMessage m =
            new NekoMessage(new int[]{successor},
                            getId(),
                            null,
                            MessageTypeConst.FD_I_M_ALIVE);
        sender.send(m);
    }

    /**
     * TimerTask that periodically sends a heartbeat to the
     * successor of this process.
     */
    private class SenderTask extends TimerTask {

        private Object lock = new Object();

        /**
         * Sends a heartbeat to the successor and schedule the
         * next execution of this method.
         */
        public void run() {
            synchronized (lock) {
                sendHeartbeat();
                timer.schedule(this, tSend);
            }
        }

        /**
         * Resets this timer task and schedules a new execution
         * if tSend is less than Double.MAX_VALUE.
         */
        public void reset() {
            synchronized (lock) {
                cancel();
                if (tSend < Double.MAX_VALUE) {
                    timer.schedule(this, tSend);
                }
            }
        }

        public String toString() {
            return RingHeartbeat.this + "-SenderTask";
        }
    }

    /**
     * TimerTask that periodically suspects the monitored
    * process. Whenever a FD_I_M_ALIVE message is received,
    * this task is reset.
    */
    private class SuspectTask extends TimerTask {

        private Object lock = new Object();

        /**
         * Suspects the monitored process. Called periodically
         * unless SuspectTask reset() is called before.
         */
        public void run() {
            synchronized (lock) {
                suspect();
            }
        }

        /**
         * Resets this SuspectTask.
         */
        public void reset() {
            synchronized (lock) {
                unsuspect();
                cancel();
                if (tReceive < Double.MAX_VALUE) {
                    timer.schedule(this, tReceive);
                }
            }
        }

        public String toString() {
            return RingHeartbeat.this + "-SuspectTask";
        }
    }

    /**
     * Default constructor for the RingHeartbeat.
     */
    public RingHeartbeat(NekoProcess process) {
        // double tSend =
        //     NekoSystem.instance().getConfig().getDouble
        //         ("lse.neko.failureDetectors.RingHeartBeat.tSend");
        // double tReceive =
        //     NekoSystem.instance().getConfig.getDouble
        //         ("lse.neko.failureDetectors.RingHeartBeat.tReceive");
        this(process, 500, 1000);
    }

    /**
     * RingHeartbeat failure detector for the given process.
     */
    public RingHeartbeat(NekoProcess process, double tSend, double tReceive) {
        super(process);

        setParameters(tSend, tReceive);
        if (logger.isLoggable(Level.FINE)) {
            final String s =
                "Created RingHeartbeat FD. Recv from {0}, Send to {1}";
            logger.log(Level.FINE, s,
                       new Object[] {
                           new Integer(monitored),
                           new Integer(successor)
                       });
        }

    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    /**
     * Modifies parameters of the failure detector.
     * Also called by the constructor.
     * tSend == Double.MAX_VALUE implies that no heartbeats are ever
     * sent. tReceive == Double.MAX_VALUE implies that no suspicion is
     * ever generated.
     */
    public void setParameters(double newTSend, double newTReceive) {
        this.tSend = newTSend;
        this.tReceive = newTReceive;

        senderTask.reset();
        suspectTask.reset();
    }

    private void processFDMessage(NekoMessage m) {
        int source = m.getSource();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("heartbeat from p" + source);
        }
        if (source == monitored) {
            suspectTask.reset();
        }
    }

    public void deliver(NekoMessage m) {

        switch (m.getType()) {
        case MessageTypeConst.FD_I_M_ALIVE:
            processFDMessage(m);
            break;
        default:
            if (useApplicationMessages) {
                processFDMessage(m);
            }
            receiver.deliver(m);
        }

    }

    private boolean useApplicationMessages = false;

    /**
     * Tells the failure detector if application messages
     * (that is, messages which are not heartbeats)
     * should be used to unsuspect processes.
     */
    public void useApplicationMessages(boolean newUseApplicationMessages) {
        this.useApplicationMessages = newUseApplicationMessages;
    }

    private static final Logger logger =
        NekoLogger.getLogger(RingHeartbeat.class.getName());
}
