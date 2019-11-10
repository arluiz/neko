package lse.neko.util;

// java imports:
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// other imports:
import org.apache.java.util.Configurations;


/**
 * This class compares sequences and checks if certain lines
 *  (rounds of the algorithm) contain the same item.
 *  Basically, for each line, it finds the majority item,
 *  and then counts how many items are different from said majority.
 *  It also counts the number of lines where there is a mismatch.
 *  One important aspect is that if a column (one set) does not contain
 *  the item that is the majority item, it considers it has been lost
 *  and changes the line numbering accordingly.<br>
 *  For instance, if you consider the following case:<br>
 *  <table border=1>
 *  <tr><th>Node 1</th><th>Node 2</th><th>Node 3</th></tr>
 *  <tr><td>1</td><td>1</td><td>1</td></tr>
 *  <tr><td>2</td><td>2</td><td>3</td></tr>
 *  <tr><td>3</td><td>3</td><td>4</td></tr>
 *  <tr><td>4</td><td>4</td><td>5</td></tr>
 *  <tr><td>5</td><td>5</td><td>6</td></tr>
 *  </table>
 *  <br>If we do line by line comparison, all lines except line 1 are wrong.
 *  This algorithm only counts line 2 as wrong, all other are correct.
 *  This correction is only done the missing item will not come later, eg was
 *  lost. So it this case, only if Node 3 never saw sequence 2.
 *  @author Matthias Wiesmann
 */
public class TetrisSequenceComparer implements SequenceComparerInterface {

    protected List[] sequences;
    protected int maxColumns = 0;
    protected int maxLines = 0;
    protected int badLines = 0;
    protected int badItems = 0;
    protected int lostItems = 0;
    protected int lostLines = 0;

    protected Map[] posTables;

    protected boolean printBad = false;
    protected boolean printLost = false;

    private StringBuffer msgBuffer = new StringBuffer();

    protected static final String PRINT_BAD_KEY =
        "TetrisSequenceComparer.printBad";
    protected static final String PRINT_LOST_KEY =
        "TetrisSequenceComparer.printLost";

    /**
     * Configures the comparison class.
     * This method searches for two boolean keys:
     * <ul>
     * <li><code>TetrisSequenceComparer.printBad</code></li>
     * <li><code>TetrisSequenceComparer.printLost</code></li>
     * </ul>
     *
     *  @param config the configuration object
     */

    public void config(Configurations config) {
        printBad = config.getBoolean(PRINT_BAD_KEY, false);
        printLost = config.getBoolean(PRINT_LOST_KEY, false);
    } // config

    /**
     * Calculates the maximum number of lines.
     * The result is put in the <code>maxLines</code> field.
     */

    protected void maxLines() {
        maxLines = -1;
        for (int i = 0; i < sequences.length; i++) {
            if (maxLines < sequences[i].size()) {
                maxLines = sequences[i].size();
            } // if
        } // for
    } // maxLines

    /**
     * Gives a specific sequence element.
     *
     * @param c the column of the element
     * @param l the line of the element
     * @return the requested sequence
     * If the sequence is out of bounds, <code>null</code> is returned.
     */

    public Object getSequence(int c, int l) {
        if (l < sequences[c].size()) {
            return sequences[c].get(l);
        } else {
            return null;
        }
    } // getSequence

    /**
     * Calculates the majority out of an array.
     * @param array Array of objects to consider
     * @return the object found in a majority of cases
     * If there is an equality, then the first candidate is
     * returned.
     */

    protected static Object majorityLine(Object[] array) {
        Map countTable = new HashMap(array.length);
        for (int i = 0; i < array.length; i++) {
            int v;
            final Object current = array[i];
            if (current != null) {
                final Object c = countTable.get(current);
                if (c == null) {
                    v = 1;
                } else {
                    v = ((Integer) c).intValue() + 1;
                    countTable.remove(current);
                }
                countTable.put(current, new Integer(v));
            } // if current
        } // for all columns
        Iterator it = countTable.keySet().iterator();
        Object majority = null;
        int majorityNumber  = 0;
        while (it.hasNext()) {
            final Object key = it.next();
            final int i = ((Integer) countTable.get(key)).intValue();
            if (i > majorityNumber) {
                majorityNumber = i;
                majority = key;
            } // found new majority
        } // while
        return majority;
    } // majorityLine

    /**
     * Finds if an item is present in an array
     *  Does a linear search, so it's quite slow.
     *  @deprecated Use findItem(item) instead.
     */

    protected static int findItem(Object item, Object[] array) {
        if (item == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (item.equals(array[i])) {
                return i;
            }
        } // for
        return -1;
    } // findItem

    /**
     * Builds a hashtable to make look-up of positions faster
     *  This table is used by the <code>findItem</code>.
     */

    protected void buildPosTable() {
        posTables = new Map[sequences.length];
        for (int column = 0; column < sequences.length; column++) {
            posTables[column] = new HashMap(sequences[column].size());
            for (int line = 0; line < sequences[column].size(); line++) {
                final Object o = sequences[column].get(line);
                posTables[column].put(o, new Integer(line));
            } // for
        } // for
    } //

    /**
     * Finds the position of an item in a given column.
     * Needs the structures set up by <code>buildPosTable</code>
     * @param item the item to search
     * @param column the column to search the item in
     */

    protected int findItem(Object item, int column) {
        final Integer i = (Integer) posTables[column].get(item);
        if (i == null) {
            return -1;
        }
        return i.intValue();
    } // findItem

    /**
     * Format used to print out bad items.
     */
    protected static final String ACCOUNT_BAD_FORMAT =
        "BAD - delta={0} column={1} <{2}>-<{3}>";

    /**
     * Method called for each out of order item.
     * @param line the line of the item
     * @param column the column of the item
     * @param bad the item that was present
     * @param majority the item that was expected
     */
    protected void accountBad(int line, int badLine, int column,
                              Object bad, Object majority)
    {
        badItems++;
        final int lineDelta = line - badLine;
        if (printBad) {
            final Object[] array = {
                new Integer(lineDelta),
                new Integer(column),
                bad,
                majority };
            String msg = MessageFormat.format(ACCOUNT_BAD_FORMAT, array);
            msgBuffer.append(msg);
            msgBuffer.append("\n");
        } // if printBad
    } // accountBad

    /**
     * Format used to print lost items.
     */
    protected static final String ACCOUNT_LOST_FORMAT =
        "LOST - column={1}  majority=<{3}>";

     /**
      * Method called for each lost item.
      * @param line the line of the item
      * @param column the column of the item
      * @param bad the item that was present
      * @param majority the item that was expected
      */
    protected void accountLost(int line, int column,
                               Object bad, Object majority)
    {
        lostItems++;
        if (printLost) {
            final Object[] array = {
                new Integer(line),
                new Integer(column),
                bad ,
                majority
            };
            String msg = MessageFormat.format(ACCOUNT_LOST_FORMAT, array);
            msgBuffer.append(msg);
            msgBuffer.append("\n");
        } // if printLost
    } // accountLost




    /**
     * Does all the calculations.
     */
    protected void calc() {
        int[] counter = new int[sequences.length];
        for (int i = 0; i < counter.length; i++) {
            counter[i] = 0;
        }
        for (int line = 0; line < maxLines; line++) {
            boolean lineBad = false;
            boolean lineLost = false;
            final Object[] array = new Object[sequences.length];
            for (int column = 0; column < sequences.length; column++) {
                array[column] = getSequence(column, counter[column]);
            }
            final Object majority = majorityLine(array);

            for (int column = 0; column < sequences.length; column++) {
                final int localLine = counter[column];
                final Object item = getSequence(column, localLine);
                if (item == null) {
                    lineBad = true;
                } else if (!item.equals(majority)) {
                    final int position = findItem(majority, column);
                    if (position >= 0) {
                        // if item exists -> wrong order -> consume
                        accountBad(localLine, position, column,
                                   item, majority);
                        counter[column]++;
                        lineBad = true;
                    } else {
                        // item does not exist, lost item
                        accountLost(localLine, column, item, majority);
                        lineLost = true;
                    }  // else item does not exist
                } else { // item is like majority, we go on
                    counter[column]++;
                } // if else
            } // for column
            if (lineBad) {
                badLines++;
            }
            if (lineLost) {
                lostLines++;
            }
        } // for line
    } // calc

    public boolean areSequenceIdentical() {
        return badItems == 0;
    } // areSequenceIdentical

    /**
     * @return the number of lines in the data
     */

    public int getLineNumber() {
        return maxLines;
    } // getLineNumber

    /**
     * @return the number of sequences in the data
     */

    public int getSequenceNumber() {
        return sequences.length;
    } // getSequenceNumber

    /**
     * @return the number of lines that are
     *  different between the sequences
     */

    public int getLineDifferences() {
        return badLines;
    } // countLineDifferences

    public int getLineLost() {
        return lostLines;
    } // getLineLost

    /**
     * @return the relative error, eg.
     * the proportional number of lines that are bad.
     */

    public double relativeError() {
        return (double) badLines / (double) getLineNumber();
    } // relativeError

    /**
     * @return the relative number of lost lines.
     */

    public double relativeLosses() {
        return (double) lostLines / (double) getLineNumber();
    } // relativeLosses

    /**
     * Checks if all sequences have the same length.
     * @return <code>true</code> if the sequences have different lengths
     */
    public boolean areSequenceMalformed() {
        for (int i = 0; i < maxColumns; i++) {
            if (sequences[i].size() != maxLines) {
                return true;
            }
        } //
        return false;
    } // areSequenceMalformed

    protected static final String TO_STRING_FORMAT =
        "{0} [Sequences={1} Lines={2} Different Lines={3} Lost Lines={4} "
        + "Relative Difference={5}% Relative Losses={6}% "
        + "Bad Items={7} Lost Item={8}";

    /**
     * Pretty prints information about the sequences.
     */
    public String toString() {
        Object[] array = { getClass().getName(),
                           new Integer(getSequenceNumber()),
                           new Integer(getLineNumber()),
                           new Integer(getLineDifferences()),
                           new Integer(getLineLost()),
                           new Double(relativeError() * 100),
                           new Double(relativeLosses() * 100),
                           new Integer(badItems),
                           new Integer(lostItems)
        };
        String msg = MessageFormat.format(TO_STRING_FORMAT, array);
        msgBuffer.append(msg);
        String ret = msgBuffer.toString();
        msgBuffer = null;
        return ret;
    } // toString

    /**
     * Sets up the sequence comparison.
     *  Most of the calculation is done in the <code>calc</code>
     *  method.
     *  @param newSequences the different sequences to compare
     */
    public void init(List[] newSequences) {
        this.sequences = newSequences;
        maxColumns = sequences.length;
        maxLines();
        buildPosTable();
        calc();
    }  // init

    /**
     * Simple test program. Builds some test sequences
     * and prints the information for it.
     */
    public static void main(String[] args) {
        List[] array = {
            Arrays.asList(new String[] { "1", "2", "3", "4", "5", "7" }),
            Arrays.asList(new String[] { "1", "2", "3", "4", "5", "7" }),
            Arrays.asList(new String[] { "1", "3", "4", "5", "6", "7" })};
        TetrisSequenceComparer c = new TetrisSequenceComparer();
        c.init(array);
        String msg = c.toString();
        Object[] a = { TetrisSequenceComparer.class.getName(),
                       new Integer(3),
                       new Integer(6),
                       new Integer(1),
                       new Integer(1),
                       new Double(100.0 / 6.0),
                       new Double(100.0 / 6.0),
                       new Integer(1),
                       new Integer(1)
        };
        String msg0 = MessageFormat.format(TO_STRING_FORMAT, a);
        if (!(msg.equals(msg0))) {
            System.out.println("Test failed!"
                               + "\nreference: " + msg0
                               + "\nactual:    " + msg);
            System.exit(1);
        }
        System.out.println("Test successful");
    } // main

} // TetrisSequenceComparer


