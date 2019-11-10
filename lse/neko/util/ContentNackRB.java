package lse.neko.util;

// java imports:
import java.util.HashMap;
import java.util.Map;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;


/**
 * Content for a message with an integer identifier; the integer identifier
 * is incremented at each ContentNackRB creation and it is therefore guaranteed
 * that the ContentNackRB objects will have consecutive, monotonically
 * increasing identifiers.
 */
public class ContentNackRB extends NekoMessage {

    // map of identifiers for the different sending processes
    private static Map nextId = null;

    static {
        nextId = new HashMap();
    }

    private int id = -1;

    public ContentNackRB(int from, int[] to, Object protocolId,
                         Object content, int type)
    {
        super(from, to, protocolId, content, type);

        if (from < 0) {
            throw new IllegalArgumentException("Source of ContentNackRB"
                                               + " must be "
                                               + "a real process. Found : "
                                               + from);
        }

        synchronized (this.getClass()) {
            Integer theId = (Integer) nextId.get(new Integer(from));
            if (theId == null) {
                id = 0;
            } else {
                id = theId.intValue();
            }
            nextId.put(new Integer(from), new Integer(id + 1));
        }
    }

    public ContentNackRB(NekoMessage m) {
        this(m.getSource(), m.getDestinations(), m.getProtocolId(),
             m.getContent(), m.getType());

    }

    public int getId() {
        return id;
    }

    public String toString() {
        return "ContentNackRB[id=" + id + ", src=" + getSource() + ", dests="
            + Util.toString(getDestinations())
            + ", protocolId=" + getProtocolId()
            + ", content=" + Util.toString(getContent()) + ", "
            + MessageTypes.instance().getName(getType()) + "]";
    }
}


