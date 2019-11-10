package lse.neko;

/**
 * FIFO queue for NekoMessages.
 * Implemented as a circular buffer.
 */
public class NekoMessageQueue {
    // FIXME: rely on lse.neko.util.Queue if there is no significant
    // performance penalty. Also, move to lse.neko.util .

    public static final int DEFAULT_SIZE = 8;
    public static final int INCREASE_FACTOR = 4;

    private NekoMessage[] data;

    /**
     * Next put should be to data[tail].
     */
    private int tail;

    /**
     * Next get should be from data[head].
     */
    private int head;

    private NekoObjectInterface lock;

    public NekoMessageQueue() {
        lock = NekoSystem.instance().createObject();
        empty();
    }

    /**
     * Empties the queue.
     */
    public void empty() {
        synchronized (lock) {
            data = new NekoMessage[DEFAULT_SIZE];
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
    public void put(NekoMessage o) {
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
                NekoMessage[] newData
                    = new NekoMessage[data.length * INCREASE_FACTOR];
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
    public NekoMessage get() {
        synchronized (lock) {
            while (head == tail) {
                try {
                    lock.doWait();
                } catch (InterruptedException ex) {
                }
            }
            NekoMessage r = data[head];
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
    public NekoMessage get(double timeout) {
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
            NekoMessage r = data[head];
            data[head] = null;
            head++;
            if (head >= data.length) {
                head = 0;
            }
            return r;
        }
    }

}

