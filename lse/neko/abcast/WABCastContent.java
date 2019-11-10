package lse.neko.abcast;

// java imports:
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// lse.neko imports:
import lse.neko.util.Util;


public class WABCastContent implements java.io.Serializable {

    int round;
    Object[] estimate;

    private static final int LIMIT = 500;

    public WABCastContent(int round, Object[] estimate) {
        //this.id = id;
        this.round = round;
        if (estimate == null || estimate.length <= LIMIT) {
            this.estimate = estimate;
        } else {
            this.estimate = new Object[LIMIT];
            System.arraycopy(estimate, 0, this.estimate, 0, LIMIT);
        }
    }

    public List getEstimate() {
        return new ArrayList(Arrays.asList(estimate));
    }

    public int getRound() {
        return round;
    }

    public String toString() {
        return "[round=" + round + " estimate=" + Util.toString(estimate) + "]";
    }
}
