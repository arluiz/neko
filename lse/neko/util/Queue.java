package lse.neko.util;

// lse.neko imports:
import lse.neko.NekoObject;
import lse.neko.NekoObjectInterface;


/**
 * FIFO queue for Objects.
 * Implemented as a circular buffer.
 */
public class Queue {

    public static final int DEFAULT_SIZE = 8;
    public static final int INCREASE_FACTOR = 4;

    private Object[] data;

    /**
     * Next put should be to data[tail].
     */
    private int tail;

    /**
     * Next get should be from data[head].
     */
    private int head;

    private final NekoObjectInterface lock =
        new NekoObject() {
            public String toString() {
                return Queue.this.toString() + "-lock";
            }
        };

    public Queue() {
        empty();
    }

    /**
     * Empties the queue.
     */
    public void empty() {
        synchronized (lock) {
            data = new Object[DEFAULT_SIZE];
            head = 0;
            tail = 0;
        }
    }

    /**
     * Returns the number of messages in the queue.
     */
    public int getSize() {
        synchronized (lock) {
            int r = tail - head;
            if (r < 0) {
                r += data.length;
            }
            return r;
        }
    }

    /**
     * Puts a message into the queue.
     * The underlying array grows if necessary.
     * FIXME: problem: the size of that array never decreases.
     * This is a waste of memory.
     */
    public void put(Object o) {
        synchronized (lock) {
            data[tail] = o;
            if (head == tail) {
                lock.doNotify();
            }
            tail++;
            if (tail >= data.length) {
                tail = 0;
            }
            if (tail == head) {
                // resizing
                Object[] newData
                    = new Object[data.length * INCREASE_FACTOR];
                System.arraycopy(data, head, newData, 0, data.length - head);
                System.arraycopy(data, 0, newData, data.length - head, head);
                head = 0;
                tail = data.length;
                data = newData;
            }
        }
    }

    /**
     * Gets a message from the queue.
     * Blocks while the queue is empty.
     */
    public Object get() {
        synchronized (lock) {
            while (head == tail) {
                try {
                    lock.doWait();
                } catch (InterruptedException ex) {
                }
            }
            Object r = data[head];
            data[head] = null;
            head++;
            if (head >= data.length) {
                head = 0;
            }
            return r;
        }
    }

    /**
     * Gets a message from the queue.
     * Blocks while the queue is empty,
     * but returns null if the time spent waiting exceeds the timeout.
     * Negative and 0 timeouts make get return immediately.
     * Note that this behavior is different from
     * the behavior of Object.wait(0),
     * which is equivalent to Object.wait().
     */
    public Object get(double timeout) {
        synchronized (lock) {
            if (head == tail) {
                if (timeout > 0) {
                    try {
                        lock.doWait(timeout);
                    } catch (InterruptedException ex) {
                        return null;
                    }
                }
                if (head == tail) {
                    return null;
                }
            }
            Object r = data[head];
            data[head] = null;
            head++;
            if (head >= data.length) {
                head = 0;
            }
            return r;
        }
    }

}

