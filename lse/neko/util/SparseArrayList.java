package lse.neko.util;

// java imports:
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


/**
 * A special kind of list that stores null values efficiently.
 * Long sublists of null values take up almost no memory.
 * Also, adding elements at the end of the list is more efficient
 * than with ArrayList when the list has to grow: this list
 * does not copy all its data into a bigger array.
 * If the list is huge, this saves a lot of
 * (temporarily used) memory when the list grows.
 * <p>
 * Because of these properties, the list can be used to process huge
 * streams of data: if the producer adds elements at the end and the
 * consumer zeroes out elements from the beginning at about the same
 * rate, then the list will never use much memory.
 * <p>
 * This list is backed by a two-dimensional array (array of refs to array).
 * Element #k is stored as array[k/C][k%C] with C = BOTTOM_ARRAY_SIZE.
 */
public class SparseArrayList
    extends AbstractList
    implements Serializable, RandomAccess
{

    /**
     * List holding arrays (ArrayCountingNonNull instances).
     */
    private transient List topArray;

    /**
     * The number of elements in this list.
     */
    private int size;

    /**
     * The number of non-null elements in the list.
     * Updated by the nested class ArrayCountingNonNull.
     */
    private transient int numNonNullAll;

    /**
     * The size of the bottom-level arrays holding elements.
     */
    private static final int BOTTOM_ARRAY_SIZE = 100;

    /**
     * This array maintains the number of non-null
     * elements in itself. Hence it is efficient
     * to ask whether the array has only null elements.
     *
     * It also maintains the number of non-null elements
     * in all the instances of this class.
     */
    private class ArrayCountingNonNull {

        /**
         * The number of non-null elements in the array.
         */
        private int numNonNull;

        /**
         * The elements of this array.
         */
        private Object[] array;

        /**
         * Creates an array of the specified size.
         *
         * @param size the size of the array.
         */
        public ArrayCountingNonNull(int size) {
            array = new Object[size];
            numNonNull = 0;
        }

        /**
         * Returns the element at the specified position in this array.
         *
         * @param index index of element to return.
         * @return the element at the specified position in this array.
         * @throws IndexOutOfBoundsException if index is out of range
         * (<code>index < 0 || index >= size</code>)
         */
        public Object get(int index) {
            return array[index];
        }

        /**
         * Replaces the element at the specified position in this array
         * with the specified element.
         *
         * @param index index of element to replace.
         * @param element element to be stored at the specified position.
         * @return the element previously at the specified position.
         * @throws IndexOutOfBoundsException if index out of range
         * (<code>index < 0 || index >= size</code>)
         */
        public Object set(int index, Object element) {
            Object ret = array[index];
            if (ret != null) {
                numNonNull--;
                numNonNullAll--;
            }
            if (element != null) {
                numNonNull++;
                numNonNullAll++;
            }
            array[index] = element;
            return ret;
        }

        /**
         * Returns <code>true</code>
         * if the array has only <code>null</code> elements.
         */
        public boolean isEmpty() {
            return numNonNull == 0;
        }

    }

    /**
     * Constructs an empty list.
     */
    public SparseArrayList() {
        init();
    }

    /**
     * The real constructor for an empty list.
     */
    private void init() {
        topArray = new ArrayList();
        size = 0;
        numNonNullAll = 0;
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the
     * collection's iterator.
     *
     * @param collection the collection whose elements are to be placed
     * into this list.
     */
    public SparseArrayList(Collection collection) {
        init();
        addAll(0, collection);
    }

    /**
     * Returns the element at the specified position in this array.
     *
     * @param index index of element to return.
     * @return the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if index is out of range
     * (<code>index < 0 || index >= size</code>)
     */
    public Object get(int index) {
        if (index < 0 || index >= size()) {
            // XXX: index >= size() should block
            // if we want to make a list that behaves like a pipe
            throw new IndexOutOfBoundsException();
        }
        int bottomIndex = index % BOTTOM_ARRAY_SIZE;
        int topIndex = index / BOTTOM_ARRAY_SIZE;
        ArrayCountingNonNull bottomArray =
            (ArrayCountingNonNull) topArray.get(topIndex);
        if (bottomArray != null) {
            return bottomArray.get(bottomIndex);
        } else {
            return null;
        }
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements currently in the list.
     */
    public int size() {
        return size;
    }

    /**
     * Replaces the element at the specified position in this array
     * with the specified element.
     *
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if index out of range
     * (<code>index < 0 || index >= size</code>)
     */
    public Object set(int index, Object element) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException();
        }
        int bottomIndex = index % BOTTOM_ARRAY_SIZE;
        int topIndex = index / BOTTOM_ARRAY_SIZE;
        ArrayCountingNonNull bottomArray =
            (ArrayCountingNonNull) topArray.get(topIndex);
        if (bottomArray != null) {
            Object ret = bottomArray.set(bottomIndex, element);
            // empty bottom-level arrays are discarded
            if (bottomArray.isEmpty()) {
                topArray.set(topIndex, null);
            }
            return ret;
        } else {
            if (element != null) {
                bottomArray = new ArrayCountingNonNull(BOTTOM_ARRAY_SIZE);
                bottomArray.set(bottomIndex, element);
                topArray.set(topIndex, bottomArray);
            }
            return null;
        }
    }

    /**
     * Inserts the specified element at the specified position in this
     * list (optional operation). Shifts the element currently at that
     * position (if any) and any subsequent elements to the right
     * (adds one to their indices).
     *
     * @param index index at which the specified element is to be
     * inserted.
     * @param element element to be inserted.
     * @throws IndexOutOfBoundsException
     * if index out of range (index < 0 || index > size()).
     */
    public void add(int index, Object element) {
        makeSpace(index, 1);
        set(index, element);
    }

    /**
     * We need to provide an implementation because of a Java bug:
     * AbstractList.addAll(Collection) is inefficient,
     * as it calls add(index, Object).
     */
    public boolean addAll(Collection c) {
        return addAll(size(), c);
    }

    /**
     * Inserts all of the elements in the specified collection into
     * this list at the specified position (optional
     * operation). Shifts the element currently at that position (if
     * any) and any subsequent elements to the right (increases their
     * indices). The new elements will appear in the list in the order
     * that they are returned by the specified collection's
     * iterator.
     *
     * @param index index at which to insert the first element from
     * the specified collection.
     * @param c elements to be inserted into this List.
     * @return true if this list changed as a result of the call.
     * @throws IndexOutOfBoundsException
     * if index out of range (index < 0 || index > size()).
     */
    public boolean addAll(int index,
                          Collection c)
    {
        int cSize = c.size();
        if (cSize < 0) {
            throw new RuntimeException("The collection has negative size!");
        }
        if (cSize == 0) {
            return false;
        }
        makeSpace(index, cSize);
        Iterator it = c.iterator();
        for (int i = index; i < index + cSize; i++) {
            set(i, it.next());
        }
        if (it.hasNext()) {
            throw new ConcurrentModificationException("The collection grew "
                + "while addAll was executing!");
        }
        return true;
    }

    /**
     * Shifts elements of the array starting at index by shift.
     *
     * @param index index of the first element to be shifted.
     * @param shift the elements are shifted this much.
     */
    private void makeSpace(int index, int shift) {
        int oldSize = size;
        size += shift;
        int topSize = (size + BOTTOM_ARRAY_SIZE - 1) / BOTTOM_ARRAY_SIZE;
        // increases the size of topArray if necessary
        // because set does not do this
        if (topArray.size() < topSize) {
            for (int i = topArray.size(); i < topSize; i++) {
                topArray.add(null);
            }
        }
        for (int i = oldSize - 1; i >= index; i--) {
            set(i + shift, get(i));
        }
    }

    /**
     * Removes the element at the specified position in this list
     * (optional operation). Shifts any subsequent elements to the
     * left (subtracts one from their indices). Returns the element
     * that was removed from the list.
     *
     * @param index the index of the element to remove.
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if the specified index is out
     * of range (index < 0 || index >= size()).
     */
    public Object remove(int index) {
        Object ret = get(index);
        removeRange(index, index + 1);
        return ret;
    }

    /**
     * Removes from this list all of the elements whose index is
     * between fromIndex, inclusive, and toIndex, exclusive. Shifts
     * any succeeding elements to the left (reduces their index). This
     * call shortens the ArrayList by (toIndex - fromIndex)
     * elements. (If toIndex==fromIndex, this operation has no
     * effect.)
     *
     * This method is called by the clear operation on this list and
     * its subLists.
     * @param fromIndex index of first element to be removed.
     * @param toIndex index after last element to be removed.
     */
    protected void removeRange(int fromIndex,
                               int toIndex)
    {
        if (fromIndex < 0 || toIndex < fromIndex || toIndex > size) {
            throw new IllegalArgumentException("fromIndex: " + fromIndex
                                               + " toIndex: " + toIndex
                                               + " size: " + size);
        }
        if (toIndex == fromIndex) {
            return;
        }
        // shift elements left
        int shift = toIndex - fromIndex;
        for (int i = toIndex; i < size; i++) {
            set(i - shift, get(i));
        }
        // zero out leftover elements
        for (int i = size - shift; i < size; i++) {
            set(i, null);
        }
        size -= shift;
        // clear part of topArray if necessary
        int topSize = (size + BOTTOM_ARRAY_SIZE - 1) / BOTTOM_ARRAY_SIZE;
        if (topSize < topArray.size()) {
            for (int i = topSize; i < topArray.size(); i++) {
                topArray.subList(topSize, topArray.size()).clear();
            }
        }
    }

    /**
     * Returns the number of non-null elements in this list.
     *
     * @return the number of non-null elements.
     */
    protected int getNonNullCount() {
        return numNonNullAll;
    }

    /**
     * Save the state of the <tt>SparseArrayList</tt> instance to a
     * stream (that is, serialize it).
     *
     * @serialData All elements are emitted in the proper order.
     *             A run-length encoding scheme is used for nulls:
     *             n consecutive null objects are written as null
     *             followed by n.
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException
    {
        // Write out element count
        s.defaultWriteObject();

        // Write out all elements in the proper order.
        // Use run-length encoding for nulls:
        // n consecutive null objects are written as null followed by n.
        int nullCount = 0;
        for (int i = 0; i < size; i++) {
            Object element = get(i);
            if (element != null || nullCount == Integer.MAX_VALUE) {
                if (nullCount > 0) {
                    logger.log(Level.FINE, "Writing null {0,number,#}",
                               new Integer(nullCount));
                    s.writeObject(null);
                    s.writeInt(nullCount);
                    nullCount = 0;
                }
            }
            if (element != null) {
                logger.log(Level.FINE, "Writing {0}", element);
                s.writeObject(element);
            } else {
                nullCount++;
            }
        }
        // write all nulls not yet written
        if (nullCount != 0) {
            logger.log(Level.FINE, "Writing null {0,number,#}",
                       new Integer(nullCount));
            s.writeObject(null);
            s.writeInt(nullCount);
        }
    }

    /**
     * Reconstitute the <tt>SparseArrayList</tt> instance from a
     * stream (that is, deserialize it).
     */
    private synchronized void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException
    {
        // read size
        s.defaultReadObject();
        logger.log(Level.FINE, "Read size {0,number,#}", new Integer(size));
        int sizeCopy = size;

        // make space for the elements
        init();
        makeSpace(0, sizeCopy);

        // read all elements in the proper order
        int i = 0;
        while (i < size) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "i {0,number,#} size {0,number,#}",
                           new Object[] { new Integer(i), new Integer(size) });
            }
            Object element = s.readObject();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Read element #{0,number,#}: {1}",
                           new Object[] { new Integer(i), element });
            }
            if (element != null) {
                set(i, element);
                i++;
            } else {
                int nullCount = s.readInt();
                // XXX: should produce a meaningful StreamCorruptedException
                // if s.readInt fails
                logger.log(Level.FINE, "Read null count: {0,number,#}",
                           new Integer(nullCount));
                i += nullCount;
                // there is no need to write the nulls
                // skipped elements are null by default
            }
        }
        if (i != size) {
            throw new java.io.StreamCorruptedException(
                    "Read " + i + " elements of a SparseArrayList "
                    + "of size " + size);
        }
    }

    /**
     * Logger instance for messages of this class.
     */
    private static final Logger logger =
        NekoLogger.getLogger(SparseArrayList.class.getName());
}
