package lse.neko;

// java imports:
import java.io.Serializable;
import java.util.NoSuchElementException;

// lse.neko imports:
import lse.neko.util.Util;


/**
 * A message, sent from one process to another.  It contains
 * addressing information.  It can be a multicast message, that is, a
 * message sent to several processes.
 *
 * <p>
 *
 * For communication within one process, use method calls rather than
 * NekoMessages (this is a change from previous versions of Neko).
 */
public class NekoMessage
    implements Serializable
{
    /**
     * The ID of the sending process.
     */
    private int from;

    /**
     * The IDs of the receiving processes.
     */
    private int[] to;

    /**
     * The ID of the destination microprotocol.
     */
    private Object protocolId;

    /**
     * Indication of the type of the content.
     */
    private int type;

    /**
     * The message body. The information in the type
     * field might help in casting it to the right type.
     */
    private Object content;

    // FIXME: leave only one constructor
    /**
     * Constructs a new message.
     *
     * @param to the IDs of the destination processes.
     * @param protocolId the ID of the destination microprotocol.
     * @param content the content of the message. Can be any Java object.
     * @param type the type of the message. Types should be integer
     * constants and they should be associated with a string using
     * {@link MessageTypes#register}.
     */
    public NekoMessage(int[] to, Object protocolId, Object content, int type) {
        this(-1, to, protocolId, content, type);
    }

    /**
     * Constructs a new message, specifying the ID of the sender
     * process as well. This is usually not necessary: the ID of the
     * sender process is filled in automatically.
     *
     * @param from the ID of the sender process.
     * @param to the IDs of the destination processes.
     * @param protocolId the ID of the destination microprotocol.
     * @param content the content of the message. Can be any Java object.
     * @param type the type of the message. Types should be integer
     * constants and they should be associated with a string using
     * {@link MessageTypes#register}.
     */
    public NekoMessage(int from,
                       int[] to, Object protocolId, Object content, int type)
    {
        this.content  = content;
        this.protocolId = protocolId;
        this.type     = type;
        this.from     = from;
        this.to       = to;
    }

    // FIXME: make NekoMessage immutable
    // use the process context to fill this field
    /**
     * Sets the ID of the sender process.
     */
    public void setSource(int newFrom) {
        this.from = newFrom;
    }

    /**
     * Returns the ID of the sender process.
     */
    public int getSource() {
        return from;
    }

    /**
     * Returns the content of the message.
     */
    public Object getContent() {
        return content;
    }

    /**
     * Returns the message type.
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the list of destination process IDs.
     */
    public int[] getDestinations() {
        return to;
    }

    /**
     * Returns the ID of the destination microprotocol.
     */
    public Object getProtocolId() {
        return protocolId;
    }

    public String toString() {
        //StringBuffer sb = new StringBuffer(this.getClass().getName());
        StringBuffer sb = new StringBuffer("NekoMessage");
        sb.append("[from=");
        sb.append(this.from);
        sb.append(", to={");
        if ((this.to != null) && (this.to.length > 0)) {
            int i = 0;
            while (true) {
                sb.append(this.to[i]);
                if (++i >= this.to.length) {
                    break;
                }
                sb.append(",");
            }
        }
        sb.append("}, ");
        // FIXME: put back:
        /*
        if (protocolId != null) {
            sb.append(protocolId.toString());
        } else {
            sb.append("null");
        }
        sb.append(", ");
        */
        try {
            sb.append(MessageTypes.instance().getName(this.type));
        } catch (NoSuchElementException ex) {
            sb.append(this.type);
        }
        sb.append(", ");
        if (this.content != null) {
            sb.append(Util.toString(this.content));
        } else {
            sb.append("null");
        }
        sb.append("]");
        return sb.toString();
    }
}
