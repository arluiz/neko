package lse.neko.abcast;

// java imports:
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer; // ambiguous with: lse.neko.util.Timer
import java.util.TimerTask; // ambiguous with: lse.neko.util.TimerTask
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.abcast.tests.TestConstants;
import lse.neko.util.GUID;
import lse.neko.util.logging.NekoLogger;


/**
 * This class implements the Atomic Broadcast
 * with the WAB oracle (version 2).
 *  @author David Cavin, sept. 2001
 */
public class WABCast
    extends ProtocolImpl
    implements ReceiverInterface, SenderInterface
{
    // FIXME: not usable for simulations, because it uses java.util.Timer
    // rather than lse.neko.util.Timer

    // W-ADelivered message type (l. 10)
    static final int WABCAST_STEP1 = 200;
    static {
        MessageTypes.instance().register(WABCAST_STEP1, "WABCAST_STEP1");
    }

    // Received message type (l. 13)
    static final int WABCAST_STEP2 = 201;
    static {
        MessageTypes.instance().register(WABCAST_STEP2, "WABCAST_STEP2");
    }

    // Internal states of the state machine
    protected static final int STEP1 = 400;
    protected static final int STEP2 = 401;
    protected static final int IDLE_STEP1 = 402;
    protected static final int IDLE_STEP2 = 403;
    protected static final int STOPPED = 404;

    // Array of destination for messages (ie. all)
    protected final int[] all;

    // round
    protected int round;

    // estimate
    protected List estimate;

    // delivered
    protected Set delivered;

    // Current internal state
    protected int state;

    // n-f
    protected final int threshold;

    // n
    protected final int total;

    // Input buffer which stores WADelivered messages (l. 10)
    protected final Buffer firstStepBuffer;

    // Input buffer which stores received messages (l. 13)
    protected final Buffer secondStepBuffer;

    private NekoProcess process;

    public WABCast(NekoProcess process) {
        this.process = process;

        estimate = new ArrayList();
        delivered = new HashSet();
        round = 1;
        state = IDLE_STEP1;
        all = new int[process.getN()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        firstStepBuffer = new Buffer(new RoundComparator(true));
        secondStepBuffer = new Buffer(new RoundComparator(false));
        total = process.getN();
        threshold = 2 * total / 3 + 1;
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private SenderInterface udpSender;

    public void setUdpSender(SenderInterface udpSender) {
        this.udpSender = udpSender;
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    // To execute A-broadcast(m)
    public synchronized void send(NekoMessage m) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "send ",
                       new Object[] { m, "round", new Integer(round) });
        }
        boolean wasEmpty;
        synchronized (estimate) {
            wasEmpty = estimate.isEmpty();
            estimate.add(encodeMessage(m));
        }
        if (wasEmpty) {
            wABroadcast();
        }
    }

    protected Object encodeMessage(NekoMessage m) {
        if (!CHEAT) {
            return new WABCastMessage(m, new GUID(process));
        } else {
            if (m.getContent() != null) {
                return m.getContent();
            } else {
                return new Integer(fakeContent++);
            }
        }
    }

    protected NekoMessage decodeMessage(Object o) {
        if (!CHEAT) {
            return ((WABCastMessage) o).getMessage();
        } else {
            if (o instanceof GUID) {
                return new NekoMessage(new int[]{0},
                                       getId(),
                                       o,
                                       TestConstants.REQUEST);
            } else {
                return new NekoMessage(new int[]{0},
                                       getId(),
                                       null,
                                       TestConstants.STOP_SERVER);
            }
        }
    }

    private int fakeContent = 0;
    private static final boolean CHEAT = true;

    // To execute a W-ABroadcast(r,estimate)
    private void wABroadcast() {
        Object[] copy;
        synchronized (estimate) {
            copy = estimate.toArray();
        }
        WABCastContent content = new WABCastContent(round, copy);
        NekoMessage m = new NekoMessage(all, getId(), content, WABCAST_STEP1);
        udpSender.send(m);
        timer.schedule(new IPMulticastTimer(m, round), 10);
    }

    private Timer timer = new Timer();

    // To execute a send(FIRST,r,estimate)
    protected void send() {
        Object[] copy;
        synchronized (estimate) {
            copy = estimate.toArray();
        }
        WABCastContent content = new WABCastContent(round, copy);
        NekoMessage m = new NekoMessage(all, getId(), content, WABCAST_STEP2);
        sender.send(m);
    }

    private void handleWADeliver(WABCastContent content)  {
        synchronized (estimate) {
            List receivedEstimate = new ArrayList(content.getEstimate());
            removeDelivered(receivedEstimate);
            estimate = concatenation(estimate, receivedEstimate);
        }
    }

    /**
     * Called by TCPNetwork or SimpleMulticastNetwork when
     * new messages are received.
     * New messages are stored in the correct input buffer
     * regarding the message type.
     */
    public void deliver(NekoMessage m) {
        int type = m.getType();
        switch (type) {
        case WABCAST_STEP1:
        {
            //if(state == STOPPED) return;
            WABCastContent step1 = (WABCastContent) m.getContent();
            int step1Round = step1.getRound();
            synchronized (this) {
                handleWADeliver(step1);
                if (step1Round >= round) {
                    firstStepBuffer.add(step1);
                    if (step1Round == round
                        && getState() == IDLE_STEP1)
                    {
                        handle();
                    }
                }
            }
            break;
        }
        case WABCAST_STEP2:
        {
            //if(state == STOPPED) return;
            WABCastContent step2 = (WABCastContent) m.getContent();
            int step2Round = step2.getRound();
            synchronized (this) {
                if (step2Round >= round) {
                    secondStepBuffer.add(step2);
                    if (step2Round == round
                        && getState() == IDLE_STEP2)
                    {
                        handle();
                    }
                }
            }
            break;
        }
        default:
            throw new UnexpectedMessageException(m);
        }
    }

    private synchronized  int getState() {
        return state;
    }

    /**
     * Main recursive procedure that controls the state machine
     * execution.
     */
    protected synchronized void handle() {
        switch (state) {
        case STEP1:
            WABCastContent[] step1 = firstStepBuffer.deliver(round, 1);
            if (step1 != null) {
                handleFirstStep(step1[0]);
                state = STEP2;
                handle();
            } else {
                state = IDLE_STEP1;
            }
            break;
        case STEP2:
            WABCastContent[] step2 = secondStepBuffer.deliver(round, threshold);
            if (step2 != null) {
                synchronized (delivered) {
                    handleSecondStep(step2);
                    round++;
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                               "*************** round ",
                               new Integer(round));
                }
                state = STEP1;
                if (!estimate.isEmpty()) {
                    wABroadcast();
                }
                handle();
            } else {
                state = IDLE_STEP2;
            }
            break;
        case IDLE_STEP1:
            state = STEP1;
            handle();
            break;
        case IDLE_STEP2:
            state = STEP2;
            handle();
            break;
        case STOPPED:
            break;
        default:
            throw new RuntimeException("Invalid state : " + state);
        }
    }

    // Execute first step code (l. 11-12)
    protected void handleFirstStep(WABCastContent content) {
        logger.fine("start FIRST step");
        synchronized (estimate) {
            removeDelivered(content.getEstimate());
            estimate = concatenation(content.getEstimate(), estimate);
        }
        send();
        logger.fine("end FIRST step");
    }

    // Execute second step code (lines 14-24)
    protected void handleSecondStep(WABCastContent[] contents) {
        logger.fine("start SECOND step");
        List[] estimates = new List[contents.length];
        for (int i = 0; i < contents.length; i++) {
            estimates[i] = contents[i].getEstimate();
            removeDelivered(estimates[i]);
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Cleared estimates");
            for (int i = 0; i < estimates.length; i++) {
                logger.finer("estimates[" + i + "] = " + estimates[i]);
            }
        }
        List majSeq = getLongestPrefix(estimates);
        // Line 15 : estimate update
        synchronized (estimate) {
            removeDelivered(estimate);
            logger.log(Level.FINE, "Cleared estimate ", estimate);
            estimate = concatenation(majSeq, estimate);
        }
        // Line 16 : allSeq construction
        List allSeq = getAllSeq(estimates);
        ListIterator allSeqElements = allSeq.listIterator();
        while (allSeqElements.hasNext()) {
            Object next = allSeqElements.next();
            NekoMessage m = decodeMessage(next);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "aDeliver ",
                           new Object[] {
                               m, "round", new Integer(round)
                           });
            }
            receiver.deliver(m);
            //delivered.add(m);
        }
        synchronized (delivered) {
            delivered.addAll(allSeq);
        }
        logger.fine("end SECOND step");
    }

    /**
     * Procedure that removes all delivered messages
     * from an estimate.
     */
    private void removeDelivered(List content) {
        int p = 0;
        for (int i = 0; i < content.size(); i++) {
            Object element = content.get(i);
            if (!delivered.contains(element)) {
                content.set(p, element);
                p++;
            }
        }
        content.subList(p, content.size()).clear();
    }

    /**
     * Function that finds the longest prefix common
     * to a majority of datas.
     */
    private List getLongestPrefix(List[] datas) {
        List input = new ArrayList(Arrays.asList(datas));
        List prefix = new ArrayList();
        int maj = datas.length / 2;
        int index = 0;
        while (true) {
            // Construction of the table of occurences.
            Map countTable = new HashMap();
            for (int i = 0; i < input.size(); i++) {
                List data = (List) input.get(i);
                if (data != null && index < data.size()) {
                    List list = (List) countTable.get(data.get(index));
                    if (list == null) {
                        list = new ArrayList();
                        countTable.put(data.get(index), list);
                    }
                    list.add(new Integer(i));
                }
            }
            // Search of a majority in the table of occurences.
            if (countTable == null) {
                break;
            }
            Iterator keyIterator = countTable.keySet().iterator();
            int maxSize = Integer.MIN_VALUE;
            boolean found = false;
            while (keyIterator.hasNext()) {
                Object key = keyIterator.next();
                List list = (List) countTable.get(key);
                int size = list.size();
                if (size > maj) {
                    prefix.add(key);
                    found = true;
                } else {
                    ListIterator listIterator = list.listIterator();
                    while (listIterator.hasNext()) {
                        input.set(((Integer) listIterator.next()).intValue(),
                                  null);
                    }
                }
            }
            if (!found) {
                break;
            } else {
                index++;
            }
        }
        return prefix;
    }

    /**
     * Function that computes allSeq (l. 16)
     */
    private List getAllSeq(List[] estimates) {
        List allSeq = estimates[0];
        for (int i = 1; i < estimates.length; i++) {
            allSeq = prefix(allSeq, estimates[i]);
        }
        return allSeq;
    }


    // Concatenation sequence operator (+)
    protected List concatenation(List m1, List m2) {
        List res =  new ArrayList(m1);
        for (int i = 0; i < m2.size(); i++) {
            Object content = m2.get(i);
            if (!res.contains(content)) {
                res.add(content);
            }
        }
        return res;
    }

    // Prefix sequence operator (x)
    protected List prefix(List m1, List m2) {
        List res = new ArrayList();
        if (m1 == null || m2 == null) {
            return res;
        }
        Iterator it1 = m1.iterator();
        Iterator it2 = m2.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            Object c1 = it1.next();
            Object c2 = it2.next();
            if (c1.equals(c2)) {
                res.add(c1);
            } else {
                break;
            }
        }
        return res;
    }

    // Comparator that sorts the incommming buffered messages.
    class RoundComparator implements Comparator {

        boolean strict;

        /**
         * A strict round comparator avoid the presence of
         * comparable entries.
         */
        public RoundComparator(boolean strict) {
            this.strict = strict;
        }

        public int compare(Object o1, Object o2) {
            if (((WABCastContent) o1).getRound()
                < ((WABCastContent) o2).getRound())
            {
                return -1;
            } else if (((WABCastContent) o1).getRound()
                       > ((WABCastContent) o2).getRound())
            {
                return 1;
            } else if (!strict) {
                return -1;
            } else {
                return 0;
            }
        }

        public boolean equals(Object obj) {
            return false;
        }

        public int hashCode() {
            throw new RuntimeException("To use objects of this class "
                                       + "in hash-based collections, "
                                       + "implement a hashCode method "
                                       + "that is consistent with equals!");
        }
    }

    // Internal class that buffers incomming messages.
    class Buffer extends TreeSet {

        protected Buffer(RoundComparator comparator) {
            super(comparator);
        }

        protected synchronized boolean add(WABCastContent content) {
            return super.add(content);
        }

        protected synchronized WABCastContent[] deliver(int r, int minSize) {
            if (isEmpty()) {
                return null;
            }
            headSet(new WABCastContent(r, null)).clear();
            SortedSet head = headSet(new WABCastContent(r + 1, null));
            if (head.size() < minSize) {
                return null;
            } else {
                return (WABCastContent[]) head.toArray(new WABCastContent[0]);
            }
        }
    }

    class IPMulticastTimer extends TimerTask {

        NekoMessage m;
        int r;

        public IPMulticastTimer(NekoMessage m, int r) {
            this.m = m;
            this.r = r;
        }

        public void run() {
            if (round == r && state == IDLE_STEP1) {
                deliver(m);
                logger.fine("IPMulticast Timer deliver");
            } else
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                               "IPMulticast Timer ignored because "
                               + round + " != " + r + " or "
                               + state + " != " + IDLE_STEP1);
                }
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(WABCast.class.getName());
}
