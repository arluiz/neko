package lse.neko.util;

// java imports:
import java.util.List;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Interface to compare sequence number coming from executions
 *  Usefull to check things like multicasts.
 *  Data is structured in different sequences comming from different sources,
 *  and structured in lines.
 *  The sequences <em>should</em> have the same number of lines,
 *  but comparing classes should handle sequences with different lengths
 *  gracefully.
 *  @author Matthias Wiesmann
 */
public interface SequenceComparerInterface {

    /**
     * Configures the comparer.
     *  @param config the configuration object
     */

    void config(Configurations config);

    /**
     * Sets up the sequences to analyse
     *  This is supposed to be an array of sequences.
     *  Each sequence is divided in <em>n</em> lines.
     */
    void init(List[] sequences);

    /**
     * Checks if all sequences are perfectly equal.
     */
    boolean areSequenceIdentical();

    /**
     * Counts the number of sequences.
     */
    int getSequenceNumber();

    /**
     * Counts the number of lines.
     */
    int getLineNumber();

    /**
     * Counts the relative number of errors according to this metric.
     *
     * @return a number in the range 0-1 1 means all errors, 0 means no error
     */
    double relativeError();

    /**
     * Are all sequences the same length?
     *  @return <code>true</code> if some sequences have different length
     */
    boolean areSequenceMalformed();

} // SequenceComparer
