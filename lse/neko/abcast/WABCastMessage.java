package lse.neko.abcast;

// java imports:
import java.io.Serializable;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.util.GUID;


public class WABCastMessage
    implements Serializable, Comparable
{

    NekoMessage m;
    GUID id;

    public WABCastMessage(NekoMessage m, GUID id) {
        this.m = m;
        this.id = id;
    }

    public boolean equals(Object o) {
        if (o instanceof WABCastMessage) {
            return id.equals(((WABCastMessage) o).id);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return id.hashCode();
    }

    public int compareTo(Object o) {
        WABCastMessage right = (WABCastMessage) o;
        return id.compareTo(right.id);
    }

    public NekoMessage getMessage() {
        return m;
    }

    public String toString() {
        return id.toString();
    }

}
