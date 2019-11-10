package lse.neko.failureDetectors;

// java imports:
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypeConst;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.util.Timer; // ambiguous with: java.util.Timer
import lse.neko.util.TimerTask; // ambiguous with: java.util.TimerTask
import lse.neko.util.Util;
import lse.neko.util.logging.NekoLogger;


/**
 * A heartbeat failure detector.
 */
public class Heartbeat
    extends FailureDetector
    implements ReceiverInterface
{

    private double tSend;
    private double tReceive;

    private Timer timer = new Timer();

    protected SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    protected ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    protected void sendHeartbeat() {
        NekoMessage m =
            new NekoMessage(group,
                            getId(),
                            null,
                            MessageTypeConst.FD_I_M_ALIVE);
        sender.send(m);
    }

    private class SenderTask extends TimerTask {

        private Object lock = new Object();

        public void run() {
            synchronized (lock) {
                sendHeartbeat();
                timer.schedule(this, tSend);
            }
        }

        public void reset() {
            synchronized (lock) {
                cancel();
                if (tSend < Double.MAX_VALUE) {
                    timer.schedule(this, tSend);
                }
            }
        }

        public String toString() {
            return Heartbeat.this + "-SenderTask";
        }
    }

    private class SuspectTask extends TimerTask {

        private int proc;
        private Object lock = new Object();

        public SuspectTask(int proc) {
            this.proc = proc;
        }

        public void run() {
            synchronized (lock) {
                suspect(proc);
            }
        }

        public void reset() {
            synchronized (lock) {
                unsuspect(proc);
                cancel();
                if (tReceive < Double.MAX_VALUE) {
                    timer.schedule(this, tReceive);
                }
            }
        }

        public String toString() {
            return Heartbeat.this + "-SuspectTask-" + proc;
        }
    }

    // XXX: should be final
    protected int[] group;

    /**
     * Heartbeat failure detector for the given group.
     */
    public Heartbeat(NekoProcess process, double tSend, double tReceive,
                     int[] group0)
    {
        super(process);
        setGroup(group0);
        suspectTasks = new SuspectTask[group.length];
        for (int i = 0; i < group.length; i++) {
            suspectTasks[i] = new SuspectTask(group[i]);
        }
        setParameters(tSend, tReceive);
    }

    /**
     * Heartbeat failure detector for all processes
     * that sends messages on the default network.
     */
    public Heartbeat(NekoProcess process, double tSend, double tReceive) {
        this(process, tSend, tReceive, null);
    }

    private final SenderTask senderTask = new SenderTask();
    private SuspectTask[] suspectTasks;

    /**
     * Part of the constructor.
     * Computes the set of processes which will be sent
     * heartbeats.
     */
    private void setGroup(int[] groupParam) {

        int[] newGroup;
        if (groupParam == null) {
            newGroup = new int[process.getN()];
            for (int i = 0; i < process.getN(); i++) {
                newGroup[i] = i;
            }
        } else {
            newGroup = (int[]) groupParam.clone();
        }

        // remove the process itself from the group
    outer:
        {
            int index = 0;
            for (int i = 0; i < newGroup.length; i++) {
                if (newGroup[i] == process.getID()) {
                    group = new int[newGroup.length - 1];
                    System.arraycopy(newGroup, 0, group, 0, i);
                    System.arraycopy(newGroup, i + 1, group, i,
                                     newGroup.length - i - 1);
                    break outer;
                }
            }
            group = newGroup;
        }

        Arrays.sort(this.group);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("setGroup(" + Util.toString(newGroup)
                        + ") set group to " + Util.toString(group));
        }
    }

    /**
     * Modifies parameters of the failure detector.
     * Also called by the constructor.
     * tSend == Double.MAX_VALUE implies that no heartbeats are ever
     * sent. tReceive == Double.MAX_VALUE implies that no suspicion is
     * ever generated.
     */
    public void setParameters(double newTSend, double newTReceive) {
        // XXX: describe and implement a reasonable transition behavior
        // when parameters are changed.
        this.tSend = newTSend;
        this.tReceive = newTReceive;

        senderTask.reset();
        for (int i = 0; i < suspectTasks.length; i++) {
            suspectTasks[i].reset();
        }
    }

    private void processFDMessage(NekoMessage m) {
        int source = m.getSource();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("heartbeat from p" + source);
        }
        int i = Arrays.binarySearch(group, source);
        if (i >= 0) {
            suspectTasks[i].reset();
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

    /**
     * The failure detector's suspicions are updated when
     * the failure detector is queried.
     * (The receiver thread that normally updates the suspicions
     * might not get the CPU often enough if load on the machine is high.)
     */
    public boolean isSuspected(int id) {
        // FIXME: add update() method to Timer
        return super.isSuspected(id);
    }

    /**
     * The failure detector's suspicions are updated when
     * the failure detector is queried.
     * (The receiver thread that normally updates the suspicions
     * might not get the CPU often enough if load on the machine is high.)
     */
    public int getNumberOfSuspected() {
        // FIXME: add update() method to Timer
        return super.getNumberOfSuspected();
    }

    private static final Logger logger =
        NekoLogger.getLogger(Heartbeat.class.getName());
}
