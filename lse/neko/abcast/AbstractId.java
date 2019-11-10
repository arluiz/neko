package lse.neko;

// java imports:
import java.io.Serializable;


/**
 * A base class for identifiers.  They implement <code>hashCode</code>
 * and <code>equals</code> (if the embedded object implements them
 * correctly).  To have a private namespace for IDs used by your
 * microprotocol, subclass this class as a nested class of your
 * microprotocol class, and use instances of the nested class.
 */
public abstract class AbstractId
    implements Serializable
{
    private Object name;

    /**
     * Constructs a new ID.
     *
     * @param name the name that distinguishes IDs of a given subclass
     * of this class. It is often a <code>String</code>.
     */
    public AbstractId(Object name) {
        this.name = name;
    }

    private transient int hash;

    public int hashCode() {
        if (hash == 0) {
            hash = getClass().getName().hashCode() + name.hashCode();
        }
        return hash;
    }

    public boolean equals(Object o) {
        return getClass() == o.getClass()
            && getName().equals(((AbstractId) o).getName());
    }

    public Object getName() {
        return name;
    }

    public String toString() {
        return getClass().getName() + "-" + name.toString();
    }
}

