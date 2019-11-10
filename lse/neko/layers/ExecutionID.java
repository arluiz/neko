package lse.neko.layers;


/**
 * All messages of an algorithm derived from
 * MultipleExecutions use this class or a descendant
 * as message content.
 * @see MultipleExecutions
 */
public class ExecutionID
    implements java.io.Serializable
{
    /**
     * Serial number of the algorithm execution.
     */
    private int number;

    public ExecutionID(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

}

