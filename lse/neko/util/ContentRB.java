package lse.neko.util;

// java imports:
import java.io.Serializable;

// lse.neko imports:
import lse.neko.MessageTypes;


/**
 * Content for a message with an identifier.
 */
public class ContentRB
    implements Serializable
{

    private GUID id;
    private int source;
    private Object protocolId;
    private Object content;
    private int type;

    public ContentRB(GUID id, int source, Object protocolId,
                     Object content, int type)
    {
        this.id = id;
        this.source = source;
        this.protocolId = protocolId;
        this.content = content;
        this.type = type;
    }

    public GUID getId() {
        return this.id;
    }

    public int getSource() {
        return source;
    }

    public Object getProtocolId() {
        return protocolId;
    }

    public Object getContent() {
        return content;
    }

    public int getType() {
        return type;
    }

    public String toString() {
        return "ContentRB[" + id + ", " + Util.toString(content) + ", "
            + MessageTypes.instance().getName(type) + "]";
    }

}


