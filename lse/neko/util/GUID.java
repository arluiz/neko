package lse.neko.util;

// java imports:
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoThread;


/**
 * A globally unique identifier.
 * Consists of a process ID
 * and a sequence number of messages sent by a given
 * process.
 */
public class GUID
    implements Serializable, Comparable
{

    private int processId;
    private int time;

    private static Map ids = new HashMap();

    private static synchronized int getId(NekoProcess process) {
        Integer id = (Integer) ids.get(process);
        if (id == null) {
            id = new Integer(0);
            ids.put(process, id);
        }
        ids.put(process, new Integer(id.intValue() + 1));
         return id.intValue();
    }

    public GUID(NekoProcess process) {
        this.processId = process.getID();
        this.time = getId(process);
    }

    public GUID() {
        this(NekoThread.currentThread().getProcess());
    }

    public int getProcess() {
        return this.processId;
    }

    public int getTime() {
        return this.time;
    }

    public boolean equals(GUID id) {
        return (id != null)
            && (processId == id.processId)
            && (time == id.time);
    }

    public boolean equals(Object id) {
        if (!(id instanceof GUID)) {
            return false;
        }
        GUID guid = (GUID) id;
        return equals(guid);
    }

    public int hashCode() {
        return processId * 1000 + time;
    }

    public int compareTo(Object right) {
        GUID r = (GUID) right;
        if (time < r.time) {
            return -1;
        } else if (time > r.time) {
            return +1;
        } else if (processId < r.processId) {
            return -1;
        } else if (processId > r.processId) {
            return +1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return "" + processId + ";" + time;
    }

}
