package lse.neko.util;

// java imports:
import java.text.MessageFormat;
import java.util.List;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Simple comparison object
 *  Method of comparing is not very subtle.
 *  @author Matthias Wiesmann
 */
public class SimpleSequenceComparer implements SequenceComparerInterface {

    protected List[] sequences;
    protected int numberDifferences;
    protected int lineDifferences;
    protected boolean malformed;
    protected int maxLine;

    protected static final String TO_STRING_FORMAT =
        "{0} [Sequences={1} Lines={2} Different Lines={3} "
        + "Differences={4} Malformed={5}]";

    public SimpleSequenceComparer() {
    }

    public void config(Configurations config) {
    }

    public void init(List[] newSequences) {
        if (sequences.length < 2) {
            throw new RuntimeException("Cannot compare "
                                       + "less than two sequences!");
        }
        this.sequences = newSequences;
        numberDifferences = 0;
        lineDifferences = 0;
        malformed = false;
        maxLine = -1;
        for (int i = 0; i < sequences.length; i++) {
            if (sequences[i].size() > maxLine) {
                maxLine = sequences[i].size();
            }
        } // for
        for (int line = 0; line < maxLine; line++) { // We do all lines
            boolean lineOk = true;
            for (int j = 0; j < sequences.length; j++) {
                for (int k = j + 1; k < sequences.length; k++) {
                    if ((line >= sequences[j].size())
                        || (line >= sequences[k].size()))
                    {
                        numberDifferences++;
                        lineOk = false;
                        malformed = true;
                    } else if (!sequences[j].get(line)
                               .equals(sequences[k].get(line)))
                    {
                        numberDifferences++;
                        lineOk = false;
                    } // if found difference
                } // for k
            } // for j
            if (!lineOk) {
                lineDifferences++;
            }
        } // for all lines
    } // init

    public boolean areSequenceIdentical() {
        return numberDifferences == 0;
    } // areSequenceIdentit

    public boolean areSequenceMalformed() {
        return malformed;
    } // areSequenceMalformed

    public int countDifferences() {
        return numberDifferences;
    } // countDifferences

    public int getSequenceNumber() {
        return sequences.length;
    } // getSequenceNumber

    public int getLineNumber() {
        return maxLine;
    } //getLineNumber

    public int countLineDifferences() {
        return lineDifferences;
    } // countLineDifferences

    public String toString() {
        Object[] array = { getClass().getName(),
                           new Integer(getSequenceNumber()),
                           new Integer(getLineNumber()),
                           new Integer(countLineDifferences()),
                           new Integer(countDifferences()),
                           Boolean.valueOf(areSequenceMalformed())};
        return MessageFormat.format(TO_STRING_FORMAT, array);
    } // toString

    public double relativeError() {
        return (double) countLineDifferences() / (double) getLineNumber();
    } // relativeError

} // SimpleSequenceComparer
