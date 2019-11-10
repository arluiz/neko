package lse.neko.tools.logView;

// java imports:
import java.util.Arrays;


/**
 * Class NekoEvent represent an event for our programme. It contains
 * the destination process, the original process and all information
 * related to the event, the action, the content of the message.
 *
 * @author Jennifer Muller
 */
class NekoEvent {

    private String action;
    private int fromProcess;
    private int[] toProcess;
    private String content;
    private String type;

    NekoEvent(String action,
              int fromProcess,
              int[] toProcess,
              String content,
              String type)
    {
        if (action == null
            || fromProcess < 0
            || toProcess == null
            || toProcess.length == 0
            || toProcess[0] < 0
            || content == null
            || type == null)
        {
            throw new IllegalArgumentException();
        }
        for (int i = 1; i < toProcess.length; i++) {
            if (toProcess[i - 1] >= toProcess[i]) {
                throw new IllegalArgumentException();
            }
        }

        this.action = action;
        this.fromProcess = fromProcess;
        this.toProcess = toProcess;
        this.content = content;
        this.type = type;
    }

    public boolean equals(Object o) {
        NekoEvent right = (NekoEvent) o;
        return action.equals(right.action)
            && fromProcess == right.fromProcess
            && Arrays.equals(toProcess, right.toProcess)
            && content.equals(right.content)
            && type.equals(right.type);
    }

    private int hash = 0;

    public int hashCode() {
        if (hash == 0) {
            hash = toString().hashCode();
        }
        return hash;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(action);
        sb.append(" ");
        sb.append(fromProcess);
        for (int i = 0; i < toProcess.length; i++) {
            sb.append((i == 0) ? " " : ",");
            sb.append(toProcess[i]);
        }
        sb.append(" ");
        sb.append(type);
        sb.append(" ");
        sb.append(content);
        return sb.toString();
    }

    /**
     * Returns the sender process.
     */
    public int getFromProcess() {
        return fromProcess;
    }

    /**
     * Returns the destination process.
     */
    public int[] getToProcess() {
        return toProcess;
    }

    /**
     * Returns the content of the message of the NekoEvent object.
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the type of this object.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the action of this object.
     */
    public String getAction() {
        return action;
    }
}
