package lse.neko.comm;

// java imports:
import java.io.Serializable;


/**
 * XXX: remove this obsolete class.
 */
public class ShutdownContent
    implements Serializable
{

    private boolean[] processShuttingDown;
    private Object reason;

    public ShutdownContent(boolean[] processShuttingDown,
                           Object reason)
    {
        this.processShuttingDown = processShuttingDown;
        this.reason = reason;
    }

    public boolean[] getProcessShuttingDown() {
        return processShuttingDown;
    }

    public Object getReason() {
        return reason;
    }

    public String toString() {
            //StringBuffer sb = new StringBuffer(this.getClass().getName());
        StringBuffer sb = new StringBuffer("{");
        int i = 0;
        while (true) {
            sb.append(processShuttingDown[i]);
            if (++i >= processShuttingDown.length) {
                break;
            }
            sb.append(",");
        }
        sb.append("}, reason = " + reason);
        return sb.toString();
    }

}

