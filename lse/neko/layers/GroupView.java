package lse.neko.layers;

// java imports:
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Implementation of the view of a group.
 *
 * <p>Status = working version :<br>
 * View contains: list of current members of the group, and id of this view.
 * <br>
 * This class also provides method to select leader.
 * @author Ilya Shnaiderman
 */
public class GroupView {

    /**
     *  Participants of the group.
     */
    protected Set members;

    /**
     * Identifier of the current View.
     */
    protected int viewId;

    // leader of the group
    private int leader;

    // array of processes, we keep them only for optimization reasons
    private int[] processes;

    // constructor
    public GroupView(int viewId, int[] processesParam) {
        this.members = new LinkedHashSet();
        for (int i = 0; i < processesParam.length; i++) {
            members.add(new Integer(processesParam[i]));
        }
        this.viewId = viewId;
        this.leader = -1;
        processes = null;
    } // constructor

    /**
     * Returns iterator that goes through all the process of this view.
     */
    public Iterator getProcessesIterator() {
        return members.iterator();
    }

    /**
     * Returns array of processes that are participating in the group.
     */
    public int[] getProcesses() {

        if (processes != null) {
            return processes;
        }
        processes = new int[members.size()];
        Iterator it = members.iterator();
        int i = 0;
        while (it.hasNext()) {
            processes[i++] = ((Integer) it.next()).intValue();
        }
        return processes;
    }

    /**
     * Returns number of members of the group.
     */
    public int getMembersNumber() {
        return members.size();
    }

    /**
     * Returns identifier of the View.
     */
    public int getViewId() {
        return viewId;
    }

    /**
     * Returns the Leader (Coordinator, Sequencer) of the group.
     */
    public int getLeader() {
        if (leader == -1) {
            leader = selectLeader();
        }

        return leader;
    }

    /**
     * Defines policy to select the leader for the group.
     * Default implementation selects process with minimal id
     */
    protected int selectLeader() {

        // Leader.is a member of the group with minimal id.
        if (members.size() == 0) {
            return -1;
        }

        // loop initialization
        Iterator it = members.iterator();
        Integer minId = (Integer) it.next();

        // let's look for the minimum
        while (it.hasNext()) {
            Integer t = (Integer) it.next();
            if (t.compareTo(minId) < 0) {
                minId = t;
            }
        }

        return minId.intValue();
    }

    /**
     * Returns true if this process is group member.
     */
    public boolean contains(Integer id) {
        return members.contains(id);
    }


    /**
     * Returns true if this process is group member.
     */
    public boolean contains(int id) {
        return members.contains(new Integer(id));
    }


    public String toString() {
        return "View ID: " + viewId + " processes: " + members
            + " leader " + getLeader();
    }
} // end class GroupView

