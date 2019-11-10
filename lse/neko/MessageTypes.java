package lse.neko;

// java imports:
import java.util.HashMap;
import java.util.Map;


/**
 * Provides a mapping between message types (<code>int</code>s) and
 * their names. <code>register</code> should be called once for each
 * message type.
 */
public class MessageTypes {

    protected static MessageTypes theInstance =
        new MessageTypes();

    public static MessageTypes instance() {
        return theInstance;
    }

    private Map types = new HashMap();

    /**
     * Associates a name with a message type.
     */
    public void register(int type, String name) {
        Object previous = types.put(new Integer(type), name);
        if (previous != null) {
            throw new RuntimeException("The message type " + type
                                       + " is registered twice with names "
                                       + previous + " and " + name);
        }
    }

    /**
     * Returns the name for a message type.
     */
    public String getName(int type) {
        Object r = types.get(new Integer(type));
        if (r == null) {
            return "" + type;
        } else {
            return (String) r;
        }
    }

}
