package lse.neko.comm;

// java imports:
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.AbstractId;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.Protocol;
import lse.neko.ProtocolImpl;
import lse.neko.PullNetworkInterface;
import lse.neko.PullProtocol;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.logging.NekoLogger;


public class NetworkInitLayer
    extends ProtocolImpl
    implements ReceiverInterface
{

    public NetworkInitLayer() {
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    /**
     * Maps an identifier to the pull layer that stores incoming messages
     * with that identifier.
     */
    private Map idToProtocol = new HashMap();

    private Set unregisteredIds = new HashSet();

    public synchronized PullNetworkInterface registerNetwork(Object id) {
        logger.log(Level.FINE, "register {0}", id);
        return getPullNetwork(id);
    }

    private static class PullNetworkId extends AbstractId {
        public PullNetworkId(Object name) {
            super(name);
        }
    }

    private PullNetwork getPullNetwork(final Object id) {
        PullNetwork ret;
        if (idToProtocol.containsKey(id)) {
            ret = (PullNetwork) idToProtocol.get(id);
        } else if (unregisteredIds.contains(id)) {
            ret = null;
        } else {
            ret = new PullNetwork(id);
            ret.setId(new PullNetworkId(id));
            idToProtocol.put(id, ret);
        }
        return ret;
    }

    public synchronized void unregisterNetwork(Object id) {
        logger.log(Level.FINE, "unregister {0}", id);
        if (!idToProtocol.containsKey(id)) {
            throw new IllegalArgumentException
                ("Network id " + id + " not registered!");
        }
        Protocol protocol = (Protocol) idToProtocol.remove(id);
        protocol.setId(null);
        unregisteredIds.add(id);
    }

    public static final int START = 0;
    static {
        MessageTypes.instance().register(START, "START");
    }

    public void deliver(NekoMessage m) {
        logger.log(Level.FINE, "deliver {0}", m);
        if (m.getType() == START) {

            StartContent content = (StartContent) m.getContent();
            synchronized (this) {
                PullNetwork protocol = getPullNetwork(content.getId());
                if (protocol != null) {
                    NekoMessage newM = new NekoMessage(m.getSource(),
                                                       m.getDestinations(),
                                                       content.getId(),
                                                       content.getContent(),
                                                       content.getType());
                    protocol.deliver(newM);
                }
            }

        } else {
            // must be a shutdown message
            // FIXME: check if shutdown messages should be forwarded
            // here
            throw new UnexpectedMessageException(m);
        }
    }

    private static class StartContent
        implements Serializable
    {
        private Object id;
        private int type;
        private Object content;

        public StartContent(Object id, int type, Object content) {
            this.id = id;
            this.type = type;
            this.content = content;
        }

        public Object getId() { return id; }
        public int getType() { return type; }
        public Object getContent() { return content; }

        public String toString() {
            return "{id=" + id + ",type=" + type + ",content=" + content + "}";
        }
    }

    private class PullNetwork
        extends PullProtocol
        implements PullNetworkInterface
    {
        private Object id;

        public PullNetwork(Object id) {
            this.id = id;
        }

        public void send(NekoMessage m) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "send {0} by {1}",
                           new Object[] { m, getId() });
            }
            Object newContent = new StartContent(id,
                                                 m.getType(),
                                                 m.getContent());
            NekoMessage newM = new NekoMessage(m.getSource(),
                                               m.getDestinations(),
                                               NetworkInitLayer.this.getId(),
                                               newContent,
                                               START);
            sender.send(newM);
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(NetworkInitLayer.class.getName());

}

