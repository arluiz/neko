package lse.neko.util;
// java imports:
import java.util.List;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Only checks if all sequences are the same or not.
 * If they are the same, prints one sequence.
 * If they are not, prints all the sequences.
 * Most query methods are not implemented.
 */
public class BasicSequenceComparer
    implements SequenceComparerInterface
{

    protected List[] sequences;
    protected boolean areIdentical;

    public void config(Configurations config) {
    }

    public void init(List[] newSequences) {

        this.sequences = newSequences;

        areIdentical = true;
        List left = sequences[0];
        for (int i = 1; i < sequences.length; i++) {
            List right = sequences[i];
            if (!left.equals(right)) {
                areIdentical = false;
                break;
            }
        }
    } // init

    public boolean areSequenceIdentical() {
        return areIdentical;
    } // areSequenceIdentit

    public boolean areSequenceMalformed() {
        throw new RuntimeException("Not implemented");
    } // areSequenceMalformed

    public int countDifferences() {
        throw new RuntimeException("Not implemented");
    } // countDifferences

    // Implemented

    public int getSequenceNumber() {
        return sequences.length;
    } // getSequenceNumber

    // Stupid implementation, might be incorrect

    public int getLineNumber() {
        return sequences[0].size();
    } //getLineNumber

    /**
     * Comparison is boolean.
     *  @return <ul>
     *  <li>0 if all sequences are identical
     *  <li>1 if they are somehow different
     *  </ul>
     */

    public double relativeError() {
        if (areIdentical) {
            return 0.0;
        } else {
            return 1.0;
        }
    } // relativeError

    public String toString() {

        StringBuffer buf = new StringBuffer();
        if (areIdentical) {
            buf.append("The sequences are identical:\n"
                       + sequenceToString(sequences[0])
                       + "\n");
        } else {
            buf.append("The sequences differ.\n");
            for (int i = 0; i < sequences.length; i++) {
                buf.append("Sequence #" + i + ":\n"
                           + sequenceToString(sequences[i])
                           + "\n");
            }
        }
        return buf.toString();
    } // toString

    protected String sequenceToString(List sequence) {
        StringBuffer buf = new StringBuffer();
        buf.append("number of items = " + sequence.size() + "\n");
        for (int i = 0; i < sequence.size(); i++) {
            buf.append("  " + sequence.get(i));
        }
        return buf.toString();
    }

} // BasicSequenceComparer
