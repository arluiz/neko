package lse.neko.util;

// java imports:
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoObjectInterface;
import lse.neko.NekoSystem;
import lse.neko.util.logging.NekoLogger;


/**
 * Fixed size circular buffer.
 */
public class ObjectBuffer {

    private Object[] data;
    private int tail;
    private int head;
    private boolean empty;
    private boolean closed;
    private NekoObjectInterface lock =
        NekoSystem.instance().createObject();

    public ObjectBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        data = new Object[capacity];
        head = 0;
        tail = 0;
        empty = true;
        closed = false;
    }

    public boolean add(Object o) {
        synchronized (lock) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("ENTRY add(" + o + ")\n" + this);
            }
            if (closed) {
                throw new IllegalStateException("Already closed");
            }
            while (head == tail && !empty) {
                try {
                    lock.doWait();
                } catch (InterruptedException ex) {
                }
            }
            data[tail] = o;
            tail++;
            if (tail >= data.length) {
                tail = 0;
            }
            if (empty) {
                lock.doNotifyAll();
            }
            empty = false;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("RETURN add(" + o + ")\n" + this);
            }
        }
        return true;
    }

    public boolean addAll(Collection c) {
        Iterator it = c.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
        return true;
    }

    public void close() {
        synchronized (lock) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("ENTRY close\n" + this);
            }
            if (closed) {
                throw new IllegalStateException("Already closed");
            }
            closed = true;
            lock.doNotifyAll();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("RETURN close\n" + this);
            }
        }
    }

    private boolean calledIterator = false;

    public Iterator iterator() {
        synchronized (lock) {
            if (calledIterator) {
                throw new IllegalStateException("Already called iterator");
            }
            calledIterator = true;
            return new MyIterator();
        }
    }

    private class MyIterator implements Iterator {

        public boolean hasNext() {
            synchronized (lock) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("ENTRY hasNext\n" + ObjectBuffer.this);
                }
                while (empty && !closed) {
                    try {
                        lock.doWait();
                    } catch (InterruptedException ex) {
                    }
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("RETURN hasNext: " + (!empty) + "\n"
                                + ObjectBuffer.this);
                }
                return !empty;
            }
        }

        // also removes the object
        public Object next() {
            synchronized (lock) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("ENTRY next\n" + ObjectBuffer.this);
                }
                while (empty && !closed) {
                    try {
                        lock.doWait();
                    } catch (InterruptedException ex) {
                    }
                }
                if (empty) {
                    throw new NoSuchElementException();
                }
                if (head == tail) {
                    lock.doNotifyAll();
                }
                Object r = data[head];
                head++;
                if (head >= data.length) {
                    head = 0;
                }
                if (head == tail) {
                    empty = true;
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("RETURN next: " + r + "\n" + ObjectBuffer.this);
                }
                return r;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private static final Logger logger =
        NekoLogger.getLogger(ObjectBuffer.class.getName());
}
