package lse.neko;

// java imports:
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This microprotocol dispatches incoming messages to the right
 * microprotocol. It is also a repository that stores microprotocols
 * of a process. Microprotocols can be looked up with their IDs.
 *
 * @see NekoProcess#getDispatcher
 */
public class Dispatcher
    extends ProtocolImpl
    implements ReceiverInterface
{

    public Dispatcher() {
    }

    // it is a LinkedHashMap to get always the same String from toString
    private Map idToProtocol = new LinkedHashMap();

    public synchronized void putProtocol(Object id, Protocol protocol) {
        if (id == null) {
            throw new NullPointerException("id should be non-null");
        }
        if (protocol == null) {
            throw new NullPointerException("protocol should be non-null");
        }
        Object oldProtocol = idToProtocol.put(id, protocol);
        if (oldProtocol != null) {
            throw new RuntimeException("id already in use"
                                       + " / id = " + id
                                       + " / old protocol = " + oldProtocol
                                       + " / new protocol = " + protocol
                                       + " in " + toString());
        }
    }

    public synchronized void removeProtocol(Object id) {
        if (id == null) {
            throw new NullPointerException("id should be non-null");
        }
        Object protocol = idToProtocol.remove(id);
        if (protocol == null) {
            throw new RuntimeException("no protocol registered for id "
                                       + id + " in " + toString());
        }
    }

    public synchronized Protocol getProtocol(Object id) {
        if (id == null) {
            throw new NullPointerException("id should be non-null");
        }
        Object idLoop = id;
        while (true) {
            Protocol protocol = (Protocol) idToProtocol.get(idLoop);
            if (protocol != null) {
                return protocol;
            }
            if (!(idLoop instanceof HierarchicalId)) {
                throw new RuntimeException("no protocol registered for id "
                                           + id + " in " + toString());
            }
            // if the protocol ID is hierarchical,
            // try the lookup again with the parent ID
            idLoop = ((HierarchicalId) idLoop).getParent();
        }
    }

    public void deliver(NekoMessage m) {
        try {
            Object id = m.getProtocolId();
            ReceiverInterface protocol = (ReceiverInterface) getProtocol(id);
            protocol.deliver(m);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Error when delivering " + m, ex);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Dispatcher-");
        sb.append(getId());
        // printing the idToProtocol map cannot be done with toString()
        // because this object is also in the map (and should not be printed)
        Iterator it = idToProtocol.entrySet().iterator();
        boolean first = true;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            // "==" is appropriate here, not equals
            String value = (entry.getKey() == getId())
                ? "Dispatcher" : entry.getValue().toString();
            if (first) {
                sb.append(": {\n  ");
                first = false;
            } else {
                sb.append(",\n  ");
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(value);
        }
        sb.append("\n}");
        return sb.toString();
    }

}


