package lse.neko.layers;

// java imports:
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.ActiveReceiver;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.logging.NekoLogger;


/**
 * Synchronizes the clocks of all machines. Should be on the protocol
 * stack of process 0, and ClockSynchronizerSlaves should be
 * on the stacks of all other processes.
 * @see ClockSynchronizerSlave
 */
public class ClockSynchronizer
    extends ActiveReceiver
{

    public ClockSynchronizer(NekoProcess process,
                             LatencyTest latencyTest)
    {
        this(process, latencyTest, getDefaultGroup(process));
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private static int[] getDefaultGroup(NekoProcess process) {
        int[] group = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            group[i] = i;
        }
        return group;
    }

    public ClockSynchronizer(NekoProcess process,
                             LatencyTest latencyTest,
                             int[] group)
    {
        super(process, "ClockSynchronizer");
        this.latencyTest = latencyTest;
        // this.group = group union { process.getID() }
        Arrays.sort(group);
        int index = Arrays.binarySearch(group, process.getID());
        if (index >= 0) {
            this.group = group;
        } else {
            this.group = new int[group.length + 1];
            int insertionPoint = -(index + 1);
            System.arraycopy(group, 0, this.group, 0, insertionPoint);
            this.group[insertionPoint] = process.getID();
            System.arraycopy(group, insertionPoint,
                             this.group, insertionPoint + 1,
                             group.length - insertionPoint);
        }
    }

    private LatencyTest latencyTest;
    private int[] group;

    static final int SYNC = 157;
    static {
        MessageTypes.instance().register(SYNC, "SYNC");
    }

    private LatencyTest.EventCollector eventCollector;

    private NekoMessage[] messages;

    public void run() {
        // no action
    }

    public void synchronize(double precision) {
        synchronize(precision, 3, 30);
    }

    public void synchronize(double precision,
                            int minGoodPass,
                            int maxPass)
    {
        int[] goodPasses = new int[group.length]; // init to 0
        int numSynchronized = 0;

        for (int pass = 0;
             pass < maxPass && numSynchronized < group.length - 1;
             pass++)
        {

            logger.log(Level.FINE, "Pass #{0,number,#}", new Integer(pass));
            measure();
            double[] increments = processResults();

            for (int i = 1; i < group.length; i++) {
                if (goodPasses[i] >= minGoodPass) {
                    increments[i] = 0; // no more synchronization needed
                } else if (Math.abs(increments[i]) <= precision) {
                    goodPasses[i]++;
                    if (goodPasses[i] >= minGoodPass) {
                        logger.log(Level.FINE,
                                   "Process #{0,number,#}: clock synchronized",
                                   new Integer(i));
                        numSynchronized++;
                    }
                }
            }

            doSynchronize(increments);

        }
    }

    // XXX : this variable is also used by ClockSynchronizerSlave. Is this OK?
    static final Object EVENT_COLLECTOR_ID = "latencyTest.EventCollector";

    public void measure() {

        eventCollector =
            new LatencyTest.EventCollector(process, 2 * group.length);
        eventCollector.setLatencyTestId(latencyTest.getId());
        eventCollector.setId(EVENT_COLLECTOR_ID);
        eventCollector.setSender(sender);
        eventCollector.launch();

        registerEvent = true;
        for (int i = 1; i < group.length; i++) {
            int[] dest = { group[i] };
            NekoMessage m = new NekoMessage(dest,
                                            getId(),
                                            null,
                                            SYNC);
            eventName = "ack" + group[i];
            eventCollector.register("send" + group[i], 0);
            sender.send(m);
            receive();
        }
        registerEvent = false;
        eventName = null;
        eventCollector.finish();

        int[] dest = new int[group.length - 1];
        for (int i = 0; i < dest.length; i++) {
            dest[i] = group[i + 1];
        }
        NekoMessage m = new NekoMessage(dest, getId(), null, SYNC);
        sender.send(m);

    }

    private boolean registerEvent;
    private String eventName;

    /**
     * Overrides NekoThread.deliver(NekoMessage) for speed.
     */
    public void deliver(NekoMessage m) {
        if (m.getType() == SYNC) {
            if (registerEvent) {
                eventCollector.register(eventName, 0);
            }
            super.deliver(m);
        } else {
            throw new UnexpectedMessageException(m);
        }
    }

    public double[] processResults() {

        double[] increments = new double[group.length];

        LatencyTest.EventsIterator it = latencyTest.getEvents(group);
        LatencyTest.Events events = it.next();

        for (int i = 1; i < group.length; i++) {

            double sendTime = events.get("send" + group[i]).getTime();
            double receiveTime = events.get("receive" + group[i]).getTime();
            double ackTime = events.get("ack" + group[i]).getTime();

            increments[i] = (sendTime + ackTime) / 2 - receiveTime;
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                           "Process #{0,number,#}: adjust clock "
                           + "by {1,number,#.###} ms",
                           new Object[] {
                               new Integer(group[i]),
                               new Double(increments[i])
                           });
            }
        }

        return increments;
    }

    public void doSynchronize(double[] increments) {

        for (int i = 1; i < group.length; i++) {

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                           "Process #{0,number,#}: clock adjusted "
                           + "by {1,number,#.###} ms",
                           new Object[] {
                               new Integer(group[i]),
                               new Double(increments[i])
                           });
            }
            int[] dest = { group[i] };
            NekoMessage m = new NekoMessage(process.getID(),
                                            dest,
                                            getId(),
                                            new Double(increments[i]),
                                            SYNC);
            sender.send(m);

        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(ClockSynchronizer.class.getName());
}

