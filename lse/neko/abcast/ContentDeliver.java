package lse.neko.abcast;

// java imports:
import java.io.Serializable;

// lse.neko imports:
import lse.neko.util.GUID;


public class ContentDeliver implements Serializable {

    private GUID id;
    private int timestamp;

    public ContentDeliver(GUID id, int timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public GUID getId() {
        return id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String toString() {
        return "ContentID[" + id + ", " + timestamp + "]";
    }

}


