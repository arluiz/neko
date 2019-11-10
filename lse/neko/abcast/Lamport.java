package lse.neko.abcast;

// java imports:
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.Timer; // ambiguous with: java.util.Timer
import lse.neko.util.TimerTask; // ambiguous with: java.util.TimerTask
import lse.neko.util.logging.NekoLogger;


/**
 * This class implements Lamport's algorithm for total ordering,
 * as described in X. D&eacute;fago's thesis.
 *
 * <p>Details of the algorithm:<br>
 *
 *    received = sorted list<br>
 *    deliverable = sorted list<br>
 *    LC = sorted list</p>
 *
 * <p>Init received := null<br>
 *  &nbsp;&nbsp;&nbsp;deliverable := null<br>
 *  &nbsp;&nbsp;&nbsp;LC[p1,..,pn] := {0,...,0}</p>
 *
 * <p>1) p broadcast(m) :<br>
 *  &nbsp;&nbsp;&nbsp;LC[p]++<br>
 *  &nbsp;&nbsp;&nbsp;send(m, LC[p]) to all<br>
 *
 * <p>2) If no message has been received for delta-t units of time
 *       and no message has been sent spontaneously:<br>
 *  &nbsp;&nbsp;&nbsp;LC[p]++<br>
 *  &nbsp;&nbsp;&nbsp;send(empty, LC[p]) to all</p>
 *
 * <p>3) When p receives (m, ts(m)) from q :<br>
 *  &nbsp;&nbsp;&nbsp;LC[p] := max{ts(m), LC[p]} + 1<br>
 *  &nbsp;&nbsp;&nbsp;LC[q] := ts(m)<br>
 *  &nbsp;&nbsp;&nbsp;received += m<br>
 *  &nbsp;&nbsp;&nbsp;DeliveryTest</p>
 *
 * <p>DeliveryTest :<br>
 *  &nbsp;&nbsp;&nbsp;deliverable.clear()<br>
 *  &nbsp;&nbsp;&nbsp;forAll m in received<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;m' = received.first<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if ts(m') < min LC[q] Then<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;deliverable += m<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;received -= m<br>
 *  &nbsp;&nbsp;&nbsp;forAll m in deliverable<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;delivrer m<br>
 *
 * @author  Sing Souksavanh
 */
public class Lamport
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface
{


    /* VARIABLES ------------------------------------------------*/

    private SortedMap received = new TreeMap();

    /**
     * Message type for messages with payload.
     */
    public static final int NEW_MSG = 160675;

    /**
     * Content type for NEW_MSG messages.
     */
    public static class Content implements Serializable {

        private final Object protocolId;
        private final Object content;
        private final int type;
        private final int timestamp;

        public Content(Object protocolId,
                       Object content, int type, int timestamp)
        {
            this.protocolId = protocolId;
            this.content = content;
            this.type = type;
            this.timestamp = timestamp;
        }

        public Object getProtocolId() {
            return protocolId;
        }

        public Object getContent() {
            return content;
        }
        public int getType() {
            return type;
        }
        public int getTimestamp() {
            return timestamp;
        }

        public String toString() {
            return "" + timestamp + "," + MessageTypes.instance().getName(type)
                + "," + content;
        }
    }

    /**
     * Type for empty message.
     */
    public static final int EMPTY_MSG = 100399;

    static {
        MessageTypes.instance().register(NEW_MSG, "NEW_MSG");
        MessageTypes.instance().register(EMPTY_MSG, "EMPTY_MSG");
    }

    /**
     * Set of all participating processes.
     */
    private final int[] dest;

    /**
     * ID of the current process.
     */
    private final int pid;

    /**
     * Task to send an empty message.
     */
    private final TimerTask emptySender =
        new TimerTask() {
            public void run() {
                int ts;
                synchronized (received) {
                    ts = vectorClock.send();
                    sender.send(new NekoMessage(dest,
                                                getId(),
                                                new Integer(ts),
                                                EMPTY_MSG));
                }
            }

            public String toString() {
                return "Lamport.emptySender";
            }
        };

    private final Timer timer = NekoSystem.instance().getTimer();

    private final double delta;

    private Collection deliverable = new ArrayList();

    private boolean isDelivering = false;

    /* CONSTRUCTOR ---------------------------------------------*/

    private NekoProcess process;

    public Lamport(NekoProcess process) {
        this(process, 0);
    }

    public Lamport(NekoProcess process, double delta) {
        this.process = process;
        pid = process.getID();
        int n = process.getN();
        dest = new int[n];
        for (int i = 0; i < n; i++) {
            dest[i] = i;
        }
        if (delta < 0) {
            throw new IllegalArgumentException();
        }
        this.delta = delta;
        vectorClock = new VectorClock(n, pid);
    }

    protected SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    /* METHODS --------------------------------------------------*/

    /**
     * Handles messages of type NEW_MSG and EMPTY_MSG.
     *
     * @param m message to be handled.
     */
    public void deliver(NekoMessage m) {

        boolean doDelivery;

        synchronized (received) {
            switch (m.getType()) {
            case NEW_MSG: {

                Content content = (Content) m.getContent();
                int timestamp = content.getTimestamp();
                int source = m.getSource();
                vectorClock.receive(timestamp, source);

                received.put(new MessageInfo(timestamp, source), content);

                deliveryTest(deliverable);

                if (emptySender.getNextExecutionTime() >= Double.MAX_VALUE
                    && source != pid)
                {
                    timer.schedule(emptySender, delta);
                }

                break;
            }
            case EMPTY_MSG: {

                int timestamp = ((Integer) m.getContent()).intValue();
                int source = m.getSource();
                vectorClock.receive(timestamp, source);

                deliveryTest(deliverable);

                break;
            }
            default: {
                throw new UnexpectedMessageException(m);
            }
            }

            if (!isDelivering && !deliverable.isEmpty()) {
                isDelivering = true;
                doDelivery = true;
            } else {
                doDelivery = false;
            }
        }

        switch (m.getType()) {

        case EMPTY_MSG:
        case NEW_MSG:
        {
            synchronized (received) {
                if (emptySender.getNextExecutionTime()
                    <= NekoSystem.instance().clock())
                {
                    emptySender.cancel();
                    emptySender.run();
                }
            }

            break;
        }
        default :
            throw new UnexpectedMessageException(m);
        }

        if (doDelivery) {
            while (true) {
                Iterator it;
                synchronized (received) {
                    if (deliverable.isEmpty()) {
                        isDelivering = false;
                        logger.fine("finished delivering");
                        break;
                    }
                    it = deliverable.iterator();
                    deliverable = new ArrayList();
                }
                while (it.hasNext()) {
                    NekoMessage f = (NekoMessage) it.next();
                    logger.log(Level.FINE, "delivering {0}", f);
                    receiver.deliver(f);
                }
            }
        }
    }

    private void deliveryTest(Collection someDeliverable) {

        MessageInfo minLC = vectorClock.first();
        Iterator it = received.keySet().iterator();
        while (it.hasNext()) {
            MessageInfo tmp = (MessageInfo) it.next();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "delivery test: {0} < {1} ?",
                           new Object[] { tmp, minLC });
            }
            if (tmp.compareTo(minLC) > 0) {
                break;
            }
            Content c2 = (Content) received.get(tmp);
            NekoMessage newM = new NekoMessage(tmp.getPID(),
                                               dest,
                                               c2.getProtocolId(),
                                               c2.getContent(),
                                               c2.getType());
            someDeliverable.add(newM);
            it.remove();
        }
        logger.fine("finished delivery test");
    }

    public void send(NekoMessage m) {

        int ts;
        synchronized (received) {
            ts = vectorClock.send();

            NekoMessage newM =
                new NekoMessage(dest,
                                getId(),
                                new Content(m.getProtocolId(),
                                            m.getContent(),
                                            m.getType(),
                                            ts),
                                NEW_MSG);
            sender.send(newM);
        }
    }

    private static class VectorClock {

        private final int pid;

        private final SortedSet sortedTimestamps;
        private final int[] timestamps;

        private int read(int i) {
            int ts = timestamps[i];
            sortedTimestamps.remove(new MessageInfo(ts, i));
            return ts;
        }

        private void write(int ts, int i) {
            timestamps[i] = ts;
            sortedTimestamps.add(new MessageInfo(ts, i));
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "setting VC[{0}] to {1}",
                           new Object[] { new Integer(i), new Integer(ts) });
            }
        }

        public MessageInfo first() {
            return (MessageInfo) sortedTimestamps.first();
        }

        public int send() {
            int ts = read(pid) + 1;
            write(ts, pid);
            return ts;
        }

        public void receive(int ts, int p) {
            {
                int newTs = Math.max(read(pid), ts) + 1;
                write(newTs, pid);
            }
            if (p != pid) {
                int newTs = Math.max(read(p), ts);
                write(newTs, p);
            }
        }

        public VectorClock(int n, int pid) {
            if (pid < 0 || pid >= n) {
                throw new IllegalArgumentException();
            }
            this.pid = pid;
            timestamps = new int[n];
            sortedTimestamps = new TreeSet();
            for (int i = 0; i < n; i++) {
                if (i != pid) {
                    sortedTimestamps.add(new MessageInfo(0, i));
                }
            }
        }
    }

    private final VectorClock vectorClock;

    private static final Logger logger =
        NekoLogger.getLogger(Lamport.class.getName());
}








