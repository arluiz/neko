package lse.neko.util;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Stores a set of non-negative integers.
 * The subset of type 0, 1, 2, ..., k takes up only a small constant amount
 * of memory.
 */
public class CompressedIntSet
    implements Serializable
{
    transient SortedSet set;
    int filled;

    // Does not implement the SortedSet interface.
    // Provides similar type safe methods instead.
    // XXX: provide a full set of methods.

    public CompressedIntSet() {
        set = new TreeSet();
        filled = 0;
    }

    public CompressedIntSet(CompressedIntSet right) {
        set = new TreeSet(right.set);
        filled = right.filled;
    }

    public void add(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        if (i < filled) {
            return;
        } else if (i == filled) {
            do {
                filled++;
            } while (set.remove(new Integer(filled)));
        } else {
            // assert i > filled;
            set.add(new Integer(i));
        }
    }

    public boolean contains(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return (i < filled) ? true : set.contains(new Integer(i));
    }

    /**
     * Length of full sequence starting from 0. In other words,
     * if this method returns k, then the set contains 0, 1, 2, ..., k-1.
     */
    public int getFilled() {
        return filled;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");

        boolean first = true;
        if (filled > 0) {
            if (filled == 1) {
                sb.append("0");
            } else {
                sb.append("0-" + (filled - 1));
            }
            first = false;
        }

        Iterator it = set.iterator();
        while (it.hasNext()) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(it.next());
        }

        sb.append("]");
        return sb.toString();
    }

    public int hashCode() {
        return 31 * filled + set.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof CompressedIntSet)) {
            return false;
        }
        CompressedIntSet right = (CompressedIntSet) o;
        return filled == right.filled && set.equals(right.set);
    }

    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException
    {
        s.defaultWriteObject();

        s.writeInt(set.size());

        Iterator it = set.iterator();
        while (it.hasNext()) {
            int next = ((Integer) it.next()).intValue();
            s.writeInt(next);
        }
    }

    private synchronized void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException
    {
        s.defaultReadObject();

        // XXX: add sanity checks
        int size = s.readInt();
        set = new TreeSet();
        for (int i = 0; i < size; i++) {
            int next = s.readInt();
            set.add(new Integer(next));
        }
    }

    /**
     * For testing.
     */
    public static void main(String[] args) {
        CompressedIntSet set = new CompressedIntSet();
        check(set.toString(), "[]");
        set.add(3);
        check(set.toString(), "[3]");
        set.add(0);
        check(set.toString(), "[0,3]");
        set.add(1);
        check(set.toString(), "[0-1,3]");
        set.add(2);
        check(set.toString(), "[0-3]");
        set.add(6);
        check(set.toString(), "[0-3,6]");
        check("" + set.contains(0), "true");
        check("" + set.contains(1), "true");
        check("" + set.contains(2), "true");
        check("" + set.contains(3), "true");
        check("" + set.contains(4), "false");
        check("" + set.contains(5), "false");
        check("" + set.contains(6), "true");
        check("" + set.contains(7), "false");
        System.out.println("Test successful");
    }

    private static void check(String s1, String s2) {
        if (!s1.equals(s2)) {
            System.out.println("The result of an operation should be\n  "
                               + s2 + "\nnot\n  " + s1 + " !");
            System.exit(1);
        }
    }

}
