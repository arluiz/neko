package lse.neko.layers;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.failureDetectors.FailureDetector;


public class SimulatedFailureDetector
    extends FailureDetector
    implements ReceiverInterface
{

    public static final int START_FAILURE = 2236;
    public static final int STOP_FAILURE = 2237;

    static {
        MessageTypes.instance().register(START_FAILURE, "START_FAILURE");
        MessageTypes.instance().register(STOP_FAILURE, "STOP_FAILURE");
    }

    private int[] group;

    /**
     * Failure detector for the given group.
     */
    public SimulatedFailureDetector(NekoProcess process, int[] group) {
        super(process);

        setGroup(group);
    }

    /**
     * Part of the constructor.
     * Computes the set of processes which may be suspected
     */
    private void setGroup(int[] newGroup) {

        if (newGroup == null) {
            group = new int[process.getN() - 1];
            for (int i = 0; i < process.getID(); i++) {
                group[i] = i;
            }
            for (int i = process.getID() + 1; i < process.getN(); i++) {
                group[i - 1] = i;
            }
        } else {
            group = (int[]) newGroup.clone();
        }
    }


    public void deliver(NekoMessage m) {

        if (m.getType() == START_FAILURE) {
            Integer pid = (Integer) (m.getContent());
            if (pid.intValue() == process.getID()) {
                return;
            }
            suspect(pid.intValue());
            return;
        } else if (m.getType() == STOP_FAILURE) {
            Integer pid = (Integer) (m.getContent());
            unsuspect(pid.intValue());
            return;
        }
        throw new UnexpectedMessageException(m);
    } // deliver
}
