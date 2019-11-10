package lse.neko.util;

// java imports:
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * Basic functions used in descriptive statistics.
 * Similar to the Statistics::Descriptive::Full Perl class.
 */
public class FullDescriptiveStatistics
    extends DescriptiveStatistics
{

    /**
     * This is the real constructor.
     * Be careful: member initializers are called too late.
     * Do all initialization here.
     */
    public void clear() {
        super.clear();
        // FIXME: add stuff
        isSorted = true;
        if (data == null) {
            data = new ArrayList();
        } else {
            data.clear();
        }
    }

    private boolean isSorted;
    private List data;

    /**
     * Adds data to the statistics variable. The cached
     * statistical values are updated automatically.
     */
    public void add(double item) {
        if (isSorted && size() > 0 && item < max()) {
            isSorted = false;
        }
        super.add(item);
        data.add(new Double(item));
    }

    /**
     * Returns an iterator to the data array. Corresponds to get_data()
     * in the Perl class.
     */
    public Iterator iterator() {
        return data.iterator();
    }

    public void sort() {
        Collections.sort(data);
        mindex = 1;
        maxdex = size();
        isSorted = true;
    }

    // the rest is not yet implemented

}

