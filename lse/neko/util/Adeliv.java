package lse.neko.util;

// java imports:
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import lse.neko.util.logging.NekoLogger;



/**
 * Class representing a sequence of identifiers of ordered
 * messages, potentially with 'holes' in the sequence.
 */
public class Adeliv implements Iterator {

    /**
     * HashSet of all messages that are stored (optimization for
     * the contains() call).
     */
    protected Set allMessages = null;


    /**
     * List of messages that have been ordered.
     */
    protected List ordered = null;

    /**
     * SortedSet of non-contiguous parts of the sequence that have
     * been received but cannot be delivered yet. The parts are
     * sorted according to the position of their first element in the
     * global delivery sequence.
     */
    protected SortedSet pending = null;

    /**
     * Next position to be delivered.
     */
    protected int nextPos = 0;

    /**
     * Index representing start of 'new' messages (ordered messages that
     * have been added after the last call to getNewMsgs).
     */
    protected int nextNewMsg = 0;

    /**
     * Creates an empty sequence of messages.
     */
    public Adeliv() {
        ordered = new LinkedList();
        pending = new TreeSet();
        nextPos = 0;

        allMessages = new HashSet();
    }

    public boolean isEmpty() {
        return ordered.isEmpty() && pending.isEmpty();
    }

    /**
     * Class representing an element of the pending sorted set. Each element
     * is composed of (1) a number representing the position of the first
     * message in the global delivery sequence and (2) a List of messages
     * (representing the ordered messages in this non-contiguous part of
     * the globally ordered sequence of messages)
     */
    public static class Subsequence implements Comparable, Serializable {
        /**
         * Position of the first message of this part of the global
         * delivery sequence.
         */
        private int startPos;

        /**
         * Ordered messages in this part of the global delivery
         * sequence.
         */
        private List msgs = null;

        public List getMsgs() {
            return msgs;
        }

        /**
         * Creates an element of the pending parts of the global
         * sequence.
         */
        public Subsequence(int startPos, List msgs) {
            this.startPos = startPos;
            this.msgs = new LinkedList(msgs);
        }

        /**
         * Creates an element of the pending parts of the global
         * sequence, based on a previous Subsequence object.
         */
        public Subsequence(Subsequence o) {
            this.startPos = o.startPos;
            this.msgs = new LinkedList(o.msgs);
        }

        public int lastPos() {
            return startPos + msgs.size() - 1;
        }

        public int startPos() {
            return startPos;
        }

        /**
         * Compares two Subsequence objects, according to their
         * startPos variable. Note : this class has a natural ordering
         * that is inconsistent with equals.
         *
         * @return negative integer of this.startPos &lt; o.startPos,
         * 0 if this.startPos == o.startPos (and this.lastPos() == o.lastPos()),
         * and a positive integer if this.startPos &gt; o.startPos.
         */
        public int compareTo(Object o) {
            Subsequence seq = (Subsequence) o;

            if (startPos == seq.startPos) {
                return lastPos() - seq.lastPos();
            } else {
                return startPos - seq.startPos;
            }
        }

        /**
         * Returns a String representation of this object.
         */
        public String toString() {
            String result = "Subsequence[ (starting at " + startPos
                + ") : " + msgs + "]";

            return result;
        }
        /**
         * Serialization of a Subsequence : writes the start position of the
         * sequence, the length of the sequence and all objects in the sequence.
         */
        private void writeObject(ObjectOutputStream s) throws IOException {
            s.writeInt(startPos);
            s.writeInt(msgs.size());
            for (Iterator it = msgs.iterator(); it.hasNext();) {
                s.writeObject(it.next());
            }
        }

        /**
         * Deserialization of a Subsequence : reads the start position and the
         * size of the message sequence, then reads the N messages and puts
         * them into the message sequence.
         */
        private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException
        {
            startPos = s.readInt();
            msgs = new LinkedList();
            int msgsSize = s.readInt();
            for (int i = 0; i < msgsSize; i++) {
                msgs.add(s.readObject());
            }
        }


    }


    /**
     * Checks if there are any elements left in the adeliv
     * sequence.
     */
    public synchronized boolean hasNext() {
        return nextPos < ordered.size();
    }

    /**
     * Returns the element at the next position and moves
     * forward in the sequence of elements.
     *
     * @return The next message in the sequence of ordered messages
     */
    public synchronized Object next() {
        if (!hasNext()) {
            throw new java.util.NoSuchElementException();
        }

        return ordered.get(nextPos++);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds an entry to the sequence of ordered messages.
     *
     * @param msgID GUID to be added at the end of the sequence
     * @return true (o isn't already present in the sequence)
     */
    public synchronized boolean add(GUID msgID) {
        logger.log(Level.FINE, "Adding message {0} to adeliv", msgID);

        // first : check if msgID is already in the ordered sequence...
        if (ordered.contains(msgID)) {
            return false;
        }
        // ... or in one of the pending sequences
        for (Iterator it = pending.iterator(); it.hasNext();) {
            if (((Subsequence) it.next()).msgs.contains(msgID)) {
                return false;
            }
        }

        // add the messages to the HashSet for the contains calls...
        allMessages.add(msgID);

        // if we're here (msgID not in any sequence), add it to ordered
        // (if there is no pending sequence) or to the last pending
        // sequence
        if (pending.isEmpty()) {
            ordered.add(msgID);
        } else {
            ((Subsequence) pending.last()).msgs.add(msgID);
        }

        return true;
    }

    /**
     * Adds an entry to the sequence of ordered messages.
     *
     * @param o Object to be added at the end of the sequence
     * @return true (o isn't already present in the sequence)
     */
    public synchronized boolean add(Object o) {
        return add((GUID) o);
    }


    /**
     * Clears the sequence of adelivered messages.
     */
    public void clear() {
        ordered.clear();
        nextPos = 0;
        pending.clear();

        allMessages.clear();
    }


    /**
     * Returns a view of the portion of this set whose elements range
     * from fromPosition, inclusive, to toPosition, exclusive.
     *
     * @param fromPosition low endpoint (inclusive) of the subSet
     * @param toPosition0 high endpoint (exclusive) of the subSet
     *
     * @see java.util.TreeSet#subSet(java.lang.Object,java.lang.Object)
     */
    public synchronized Subsequence subList(int fromPosition,
                                            int toPosition0)
    {
        int toPosition = toPosition0;
        if (toPosition > ordered.size()) {
            toPosition = ordered.size();
        }

        if (fromPosition < 0 || fromPosition >= toPosition) {
            throw new IllegalArgumentException("The start "
                    + "position (" + fromPosition + ") must be greater "
                    + "than zero and smaller than the end position "
                    + "(" + toPosition + ")");
        }

        return new Subsequence(fromPosition,
                    ordered.subList(fromPosition, toPosition));
    }

    /**
     * Returns the last position that has been ordered in this sequence.
     * This method returns the last known ordered message, but there
     * can be previously ordered messages that have not been received
     * yet. There is no guarantee that all messages with IDs between
     * 0 and the return value of lastPos() have already been received and
     * delivered.
     *
     * @return the position of the last entry in the
     * sequence of the Adeliv, or -1 if no entry is present
     */
    public synchronized int lastPos() {
        if (pending.isEmpty()) {
            return ordered.size() - 1;
        } else {
            Subsequence last = (Subsequence) pending.last();
            return last.startPos + last.msgs.size() - 1;
        }
    }

    /**
     * Returns the highest message identifier among the contiguous
     * ordered messages. This method guarantees that all message IDs
     * between 0 and the return value of lastContiguousPos have been
     * ordered and are in this.
     *
     * @return the position of the last contiguous message or -1 if
     * no message has been ordered yet.
     */
    public synchronized int lastContiguousPos() {
        return ordered.size() - 1;
    }

    /**
     * Returns an Adeliv.Subsequence of consecutive messages, starting at
     * position fromElement.
     *
     * @param fromElement minimum position of the lowest element in the
     * returned sorted set
     *
     * @return Adeliv.Subsequence starting at element fromElement (or at an
     * element greater than fromElement if there are holes between fromElement
     * and this.lastPos()
     *
     * @see java.util.TreeSet#tailSet(java.lang.Object)
     */
    public synchronized Subsequence tailList(int fromElement) {
        if (fromElement < 0) {
            throw new IllegalArgumentException("The start "
                    + "position of the tail list must be greater than 0");
        }

        if (pending.isEmpty()) {
            // first case : no pending elements => return a subList
            // of ordered
            return new Subsequence(fromElement,
                    ordered.subList(fromElement, ordered.size()));
        } else {
            // second case : check that fromElement is part of the
            // last pending sequence. If it is not, we can't return
            // a contiguous sequence of messages => modify fromElement
            // so that it starts at the last pending sequence
            Subsequence last = (Subsequence) pending.last();
            // if fromElement is not part of last, set fromElement
            // to the first element of the last pending subsequence
            int fromElement0 = fromElement;
            if (last.startPos > fromElement) {
                fromElement0 = last.startPos;
            }
            return new Subsequence(fromElement0,
                    last.msgs.subList(fromElement0 - last.startPos,
                        last.msgs.size()));
        }
    }

    public synchronized boolean contains(GUID mid) {
        return allMessages.contains(mid);
    }


    /**
     * Adds the (as yet unordered) messages in toBeAdded to this.
     *
     * @param toBeAdded List to be added to this. The elements of the
     * Collection have to be of type GUID
     *
     * @return The position at which toBeAdded was added in the list (i.e.
     * the position in the global sequence of the first element of
     * toBeAdded).
     */
    public synchronized int addAll(List toBeAdded) {
        logger.log(Level.FINE, "addAll(List) Adding {0} to adelivI", toBeAdded);

        if (toBeAdded == null || toBeAdded.isEmpty()) {
            throw new IllegalArgumentException("addAll(List) "
                    + "expects a non-null, non-empty List of messages "
                    + "to be added to this");
        }

        // keep a reference of the messages in the allMessages HashSet
        allMessages.addAll(toBeAdded);
        int insPos = -1;
        if (pending.isEmpty()) {
            insPos = ordered.size();
            ordered.addAll(toBeAdded);
        } else {
            Subsequence last = (Subsequence) pending.last();
            insPos = last.lastPos() + 1;
            last.msgs.addAll(toBeAdded);
        }
        logger.log(Level.FINE, "adelivI.lastPos() = {0} after addAll(List)",
                new Integer(lastPos()));

        return insPos;

    }

    /**
     * Adds the (as yet unordered) messages in toBeAdded to this.
     *
     * @param toBeAdded List to be added to this. The elements of the
     * Collection have to be of type GUID
     *
     * @return false if toBeAdded was already entirely in ordered, true
     * otherwise
     */
    public synchronized boolean addAll(List toBeAdded, int position) {
        return addAll(new Subsequence(position, toBeAdded));
    }

    public synchronized boolean addAll(Subsequence toBeAdded) {
        logger.log(Level.FINE, "addAll(Subsequence) : {0}", toBeAdded);

        if (toBeAdded.msgs.isEmpty()) {
            return false;
        }

        // keep a reference of the added messages in the allMessages
        // HashSet
        allMessages.addAll(toBeAdded.msgs);

        // if there are no pending messages, check if toBeAdded should
        // really be added to the ordered sequence
        if (pending.isEmpty()) {
            // check if the messages in toBeAdded are already in
            // the ordered sequence
            if (toBeAdded.startPos + toBeAdded.msgs.size() <= ordered.size()) {
                // the messages to be added are already part of the sequence
                return false;
            }
            // check if the messages in toBeAdded can be directly added to
            // ordered
            if (toBeAdded.startPos <= ordered.size()) {
                int skippable = ordered.size() - toBeAdded.startPos;
                ordered.addAll(toBeAdded.msgs.subList(skippable,
                            toBeAdded.msgs.size()));

                logger.log(Level.INFO,
                           "adelivI.lastPos() = {0} after addAll(Sub)",
                           new Integer(lastPos()));

                return true;
            }
        }

        // add a new pending sequence
        pending.add(new Subsequence(toBeAdded));

        // check if some of the pending sequences can be merged
        mergePending();
        logger.log(Level.FINE, "adelivI.lastPos() = {0} after addAll(Sub)",
                new Integer(lastPos()));
        return true;
    }

    protected void mergePending() {
        if (pending.isEmpty()) {
            return;
        }

        if (ordered.isEmpty()) {
            Subsequence first = (Subsequence) pending.first();

            // if the first message of the first pending sequence is
            // the next message to be delivered, then this pending
            // sequence can be copied into the ordered list.
            if (first.startPos == nextPos) {
                ordered.addAll(first.msgs);
                pending.remove(first);
                if (pending.isEmpty()) {
                    return;
                } else {
                    mergePending();
                }
            }
            return;
        }

        // get an iterator for the pending elements
        Iterator pendingIt = pending.iterator();

        // check if the first element of pending can be merged with the
        // ordered sequence
        Subsequence current = (Subsequence) pendingIt.next();

        // if the first element is already part of the ordered sequence, delete
        // it and call mergePending() recursively
        if (current.startPos + current.msgs.size() <= ordered.size()) {
            logger.log(Level.FINE,
                       "All elements of {0} already in ordered (lastPos = {1}",
                       new Object[]{current, new Integer(ordered.size() - 1)});
            pending.remove(current);
            mergePending();
            return;
        }

        // now check if the first element can be merged with the ordered
        // sequence
        if (current.startPos <= ordered.size()) {
            // add messages in current to ordered
            assert (current.startPos + current.msgs.size() >= ordered.size());
            int skippable = ordered.size() - current.startPos;
            ordered.addAll(current.msgs.subList(skippable,
                        current.msgs.size()));

            // remove current from the pending sequence
            pending.remove(current);
            mergePending();
            return;
        }

        // finally, check if several pending sequences can be merged together
        while (pendingIt.hasNext()) {
            Subsequence next = (Subsequence) pendingIt.next();

            // check if the elements are mergeable
            if (next.startPos <= current.startPos + current.msgs.size()) {
                // merge the two mergeable lists
                concatenateLists(current, next);

                pending.remove(next);
                mergePending();
                return;
            } else {
                current = next;
            }
        }

    }

    protected void concatenateLists(Subsequence first, Subsequence second) {
        // calculate how many messages in second are already in first
        int skippableMsgs = first.startPos
            + first.msgs.size()
            - second.startPos;
        // if we're going to skip all messages in second, simply return now
        if (skippableMsgs >= second.msgs.size()) {
            return;
        }
        // create the list of messages to be added to first and add them
        List newMsgs = second.msgs.subList(skippableMsgs, second.msgs.size());
        first.msgs.addAll(newMsgs);
    }

    /**
     * Returns a list of new messages added since the last call to getNewMsgs.
     *
     * @return A list containing (at least) all the new messages added to this
     * since the last call to getNewMsgs.
     */
    public synchronized List getNewMsgs() {
        List newMsgs = new LinkedList();

        newMsgs.addAll(ordered.subList(nextNewMsg, ordered.size()));

        for (Iterator it = pending.iterator(); it.hasNext();) {
            newMsgs.addAll(((Subsequence) it.next()).msgs);
        }

        nextNewMsg = ordered.size();
        return newMsgs;
    }

    public synchronized boolean hasHoles() {
        return !pending.isEmpty();
    }

    public synchronized String toString() {
        String result = "Adeliv : [ ";
        for (Iterator it = ordered.iterator(); it.hasNext();) {
            result += it.next() + " ";
        }
        result += "]";
        int counter = 0;
        for (Iterator it = pending.iterator(); it.hasNext(); counter++) {
            Subsequence curr = (Subsequence) it.next();
            result += ", Pending " + counter + " starting at "
                + curr.startPos + " : [";
            for (Iterator msgIt = curr.msgs.iterator(); msgIt.hasNext();) {
                result += msgIt.next() + " ";
            }
            result += "]";
        }
        return result;
    }

    public boolean sanityTest() {
        if (hasHoles()) {
            logger.log(Level.WARNING, "SanityTest of Adeliv : Adeliv has "
                       + "holes, which is unexpected");
        }

        Subsequence previous = new Subsequence(0, ordered);

        for (Iterator it = pending.iterator(); it.hasNext();/*nothing*/) {
            Subsequence current = (Subsequence) it.next();

            if (previous.startPos + previous.msgs.size() >= current.startPos) {
                throw new IllegalStateException("Adeliv : Two adjacent "
                        + "sequences have not been merged. First seq : "
                        + previous + ", second seq : " + current);
            }
            if (current.msgs.isEmpty()) {
                throw new IllegalStateException("Adeliv : An empty sequence "
                        + "was found in the pending sorted set : " + current);
            }

            previous = current;
        }

        return true;
    }

    private static final Logger logger =
        NekoLogger.getLogger(Adeliv.class.getName());

}
