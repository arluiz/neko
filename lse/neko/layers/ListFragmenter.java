package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.ActiveReceiver;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoObjectInterface;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.IntHolder;
import lse.neko.util.ObjectBuffer;
import lse.neko.util.SerializableIterator;
import lse.neko.util.Util;
import lse.neko.util.logging.NekoLogger;


/**
 * This layer takes care of transmitting huge lists over reliable channels.
 * It avoids copying the whole list during all of the transmission.
 * The sender can transmit any <code>List</code>. The list is fragmented
 * into pieces, and reassembled on the receiver side.
 * Flow control is implemented by using a window scheme with acks
 * from the receiver. This is flow control on the protocol level.
 * Flow control on the application level is implemented by a
 * circular buffer.
 */
public class ListFragmenter
    extends ProtocolImpl
    implements ReceiverInterface
{

    /**
     * @param fragmentSize number of list elements in a fragment.
     * @param windowSize maximal number of fragments not acknowledged
     *   at any time.
     * @param bufferSize size of the application level buffer on
     *   the receiver side.
     */
    public ListFragmenter(int fragmentSize,
                          int windowSize,
                          int bufferSize)
    {
        if (fragmentSize < 1 || windowSize < 1 || bufferSize < 1) {
            throw new IllegalArgumentException();
        }
        this.fragmentSize = fragmentSize;
        this.windowSize = windowSize;
        this.bufferSize = bufferSize;
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private int fragmentSize;
    private int windowSize;
    private int bufferSize;

    /**
     * Sends a list to other processes.
     * A new thread is started to take care of the transmission, in
     * order to ensure that this method does not block.
     * You should transmit the identifier to the destination processes,
     * which can use the identifier to access their copy of the list
     * using <code>receiveList</code>.
     *
     * @param it a special iterator on the list to be transmitted.
     * @param dests set of receiver processes.
     * @see #getIterator
     */
    public void sendIterator(SerializableIterator it, int[] dests) {
        it.setFragmenterId(getId());
        List list = it.getList();
        Object id = it.getId();
        if (list == null || id == null || dests == null) {
            throw new NullPointerException();
        }
        if (dests.length == 0) {
            throw new IllegalArgumentException();
        }
        new SenderThread(list, id, dests);
    }

    /**
     * Maps from list IDs to SenderThreads.
     * List IDs are assigned when the sending of a list starts.
     */
    private Map senderThreads = new HashMap();

    /**
     * Receives a list from another process.
     * Only called by SerializableIterator.
     * A new thread is started to take care of the transmission, in
     * order to ensure that this method does not block.
     * You should use the list identifier transmitted by the sender.
     * Beware: upon return, the list is probably <em>not</em> complete.
     *
     * @param id the list identifier transmitted by the sender.
     */

    public Iterator getIterator(Object id) {
        if (id == null) {
            throw new NullPointerException();
        }

        ReceiverThread thread;
        synchronized (receiverThreads) {
            thread = (ReceiverThread) receiverThreads.get(id);
            if (thread == null) {
                thread = new ReceiverThread(id);
            }
            return thread.getIterator();
        }
    }

    /**
     * Maps from list IDs to ReceiverThreads.
     * List IDs are assigned by the sender side,
     * and are received in LIST_FRAGMENT messages.
     */
    private Map receiverThreads = new HashMap();

    /**
     * Handles LIST_FRAGMENT and LIST_FRAGMENT_ACK messages.
     */
    public void deliver(NekoMessage m) {
        if (m.getType() == LIST_FRAGMENT_ACK) {
            ListFragmentAck content = (ListFragmentAck) m.getContent();
            Object id = content.getId();
            SenderThread thread;
            synchronized (senderThreads) {
                thread = (SenderThread) senderThreads.get(id);
            }
            if (thread == null) {
                final String s = "Protocol error: list id " + id
                    + " in LIST_FRAGMENT_ACK message unknown!";
                throw new RuntimeException(s);
            }
            thread.deliver(m);
            return;
        } else if (m.getType() == LIST_FRAGMENT) {
            ListFragment content = (ListFragment) m.getContent();
            Object id = content.getId();
            ReceiverThread thread;
            synchronized (receiverThreads) {
                thread = (ReceiverThread) receiverThreads.get(id);
                if (thread == null) {
                    thread = new ReceiverThread(id);
                }
                thread.deliver(m);
            }
            return;
        }
        throw new UnexpectedMessageException(m);
    }

    public static final int LIST_FRAGMENT = 1117;
    public static final int LIST_FRAGMENT_ACK = 1118;

    private class SenderThread
        extends NekoThread
    {
        public SenderThread(List list, Object id, int[] dests) {
            super("sender thread");
            this.list = list;
            this.id = id;
            this.dests = dests;
            window = new HashMap();
            synchronized (senderThreads) {
                Object oldValue = senderThreads.put(id, this);
                if (oldValue != null) {
                    throw new IllegalArgumentException("The id is not unique!");
                }
            }
            start();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "SenderThread created id " + id
                           + " dests " + Util.toString(dests)
                           + " list " + list);
            }
        }

        private List list;
        private Object id;
        private int[] dests;

        private Map window;

        private NekoObjectInterface lock =
            NekoSystem.instance().createObject();

        public void run() {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "SenderThread run list.size() ",
                           new Integer(list.size()));
            }
            boolean last = false;
            for (int fromIndex = 0; !last; fromIndex += fragmentSize) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "SenderThread loop fromIndex ",
                               new Integer(fromIndex));
                }
                synchronized (lock) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("SenderThread window size "
                                    + window.size() + "(" + windowSize + ")");
                    }
                    while (window.size() >= windowSize) {
                        try {
                            lock.doWait();
                        } catch (InterruptedException ex) {
                        }
                    }
                    window.put(new Integer(fromIndex),
                               new IntHolder(dests.length));
                }
                int toIndex = fromIndex + fragmentSize;
                last = false;
                if (toIndex >= list.size()) {
                    toIndex = list.size();
                    last = true;
                }
                ListFragment fragment =
                    new ListFragment(id, fromIndex,
                                     list.subList(fromIndex,
                                                  toIndex).toArray(),
                                     last);
                NekoMessage newM = new NekoMessage(dests,
                                                   ListFragmenter.this.getId(),
                                                   fragment,
                                                   LIST_FRAGMENT);
                ListFragmenter.this.sender.send(newM);
            }
            synchronized (lock) {
                while (window.size() > 0) {
                    try {
                        lock.doWait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
            synchronized (senderThreads) {
                senderThreads.remove(id);
            }
        }

        public void deliver(NekoMessage m) {
            ListFragmentAck content = (ListFragmentAck) m.getContent();
            Integer fromIndex = new Integer(content.getFromIndex());
            IntHolder acks = (IntHolder) window.get(fromIndex);
            acks.value--;
            if (acks.value == 0) {
                synchronized (lock) {
                    if (window.size() >= windowSize) {
                        lock.doNotify();
                    }
                    window.remove(fromIndex);
                    if (window.size() <= 0) {
                        lock.doNotify();
                    }
                }
            }
        }

    }

    public class ReceiverThread
        extends ActiveReceiver
    {
        public ReceiverThread(Object id) {
            super(null, "receiver thread");
            this.id = id;
            receiverThreads.put(id, this);
            start();
        }

        private Object id;
        private int acked = 0;
        private ObjectBuffer list = new ObjectBuffer(bufferSize);
        private SortedSet fragments = new TreeSet();

        private boolean gaveIterator = false;

        public synchronized Iterator getIterator() {
            gaveIterator = true;
            doNotify();
            return list.iterator();
        }

        public void run() {

            boolean earlyDelivery = false;

        main:
            while (true) {

                // receive the list fragment message
                NekoMessage m = receive();
                // check the list fragment message
                if (m.getType() != LIST_FRAGMENT
                    || !(m.getContent() instanceof ListFragment))
                {
                    throw new RuntimeException("Assertion failed!");
                }
                ListFragment content = (ListFragment) m.getContent();
                if (!content.getId().equals(id)) {
                    throw new RuntimeException("Assertion failed! content "
                                               + content + " id " + id);
                }

                // insert the message into the sorted set of fragments
                fragments.add(content);

                logger.log(Level.FINE, "ReceiverThread fragments ",
                           fragments.toArray());

                // append fragments to the end of the list
                Iterator it = fragments.iterator();
                while (it.hasNext()) {
                    ListFragment fragment = (ListFragment) it.next();

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,
                                   "ReceiverThread next fragment ",
                                   new Object[] {
                                       fragment,
                                       "acked",
                                       new Integer(acked)
                                   });
                    }

                    // if not the next fragment, wait for the next fragment
                    if (fragment.getFromIndex() != acked) {
                        break;
                    }

                    // remove the fragment
                    it.remove();

                    // ack the fragment
                    ListFragmentAck ackContent =
                        new ListFragmentAck(id, acked);
                    NekoMessage newM =
                        new NekoMessage(new int[] { m.getSource() },
                                        ListFragmenter.this.getId(),
                                        ackContent,
                                        LIST_FRAGMENT_ACK);
                    ListFragmenter.this.sender.send(newM);

                    Object[] array = fragment.getArray();
                    acked += array.length;

                    // append the fragment onto the list
                    list.addAll(Arrays.asList(array));
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "ReceiverThread list ",
                                   new Object[] {
                                       list,
                                       "added", array,
                                       "acked", new Integer(acked)
                                   });
                    }
                    if (fragment.isLast()) {
                        break main;
                    }
                }

            }

            list.close();
            synchronized (this) {
                if (!gaveIterator) {
                    try {
                        doWait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
            synchronized (receiverThreads) {
                receiverThreads.remove(id);
            }

        }
    }

    public static class ListFragmentAck
        implements Serializable
    {
        static {
            MessageTypes.instance().register(LIST_FRAGMENT_ACK,
                                             "LIST_FRAGMENT_ACK");
        }

        public ListFragmentAck(Object id,
                               int fromIndex)
        {
            if (fromIndex < 0 || id == null) {
                throw new IllegalArgumentException();
            }
            this.id = id;
            this.fromIndex = fromIndex;
        }

        private Object id;
        private int fromIndex;

        public Object getId() { return id; }
        public int getFromIndex() { return fromIndex; }

        public String toString() {
            return "ListFragmentAck[id=" + id + ",fromIndex=" + fromIndex + "]";
        }
    }

    public static class ListFragment
        implements Serializable, Comparable
    {
        static {
            MessageTypes.instance().register(LIST_FRAGMENT, "LIST_FRAGMENT");
        }

        public ListFragment(Object id,
                            int fromIndex,
                            Object[] array,
                            boolean last)
        {
            if (fromIndex < 0 || array == null || id == null) {
                throw new IllegalArgumentException();
            }
            this.id = id;
            this.fromIndex = fromIndex;
            this.array = array;
            this.last = last;
        }

        private Object id;
        private int fromIndex;
        private Object[] array;
        private boolean last;

        public Object getId() { return id; }
        public int getFromIndex() { return fromIndex; }
        public Object[] getArray() { return array; }
        public boolean isLast() { return last; }

        public String toString() {
            return "ListFragment[id=" + id + ",fromIndex=" + fromIndex
                + ",array=" + Util.toString(array) + ",last=" + last + "]";
        }

        public int compareTo(Object o) {
            ListFragment right = (ListFragment) o;
            if (fromIndex < right.fromIndex) {
                return -1;
            } else if (fromIndex == right.fromIndex) {
                return 0;
            } else {
                return +1;
            }
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(ListFragmenter.class.getName());
}


