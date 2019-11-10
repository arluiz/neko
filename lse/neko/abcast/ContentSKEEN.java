package lse.neko.abcast;

// java imports:
import java.io.Serializable;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.util.GUID;


/**
 * Content of the source message during the
    execution of the Skeen algorithm.
*/
public class ContentSKEEN implements Serializable {

    private Object protocolId;
    private Object content;
    private int type;
    private GUID id;

    public ContentSKEEN(Object protocolId, Object content, int type, GUID id) {

        this.protocolId = protocolId;
        this.content = content;
        this.type = type;
        this.id = id;

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

    public GUID getId() {
        return id;
    }

    public String toString() {
        return "ContentSKEEN[" + protocolId + ", " + content + ", "
            + MessageTypes.instance().getName(type) + ", "
            + id + "]";
    }

}
