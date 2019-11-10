package lse.neko.util;

// java imports:
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * Basic functions used in descriptive statistics.
 * Similar to the Statistics::Descriptive::Sparse Perl class.
 */
public class DescriptiveStatistics
    implements Serializable
{

    public DescriptiveStatistics() {
        clear();
    }

    public void clear() {
        num = 0;
        sum = 0;
        sum2 = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        mindex = -1;
        maxdex = -1;
    }

    private int num;
    private double sum;
    private double sum2;
    private double min;
    private double max;
    protected int mindex;
    protected int maxdex;

    /**
     * Adds data to the statistics variable. The cached
     * statistical values are updated automatically.
     */
    public void add(double item) {
        if (item < min) {
            min = item;
            mindex = num;
        }
        if (item > max) {
            max = item;
            maxdex = num;
        }
        num++;
        sum += item;
        sum2 += item * item;
    }

    /**
     * Adds several data items.
     */
    public void add(double[] item) {
        for (int i = 0; i < item.length; i++) {
            add(item[i]);
        }
    }

    /**
     * Returns the number of data items.
     */
    public int size() {
        return num;
    }

    /**
     * Returns the mean of the data.
     */
    public double mean() {
        return sum / num;
    }

    /**
     * Returns the sum of the data.
     */
    public double sum() {
        return sum;
    }

    /**
     * Returns the variance of the data.  Division by n-1 is
     * used.
     */
    public double variance() {
        return (sum2 - sum * sum / num) / (num - 1);
    }

    /**
     * Returns the standard deviation of the data. Division
     * by n-1 is used.
     */
    public double standardDeviation() {
        return Math.sqrt(variance());
    }

    private static Map tauTable = new HashMap();
    static {
        tauTable = new HashMap();
        tauTable.put(new Double(0.9), new Double(1.64485));
        tauTable.put(new Double(0.95), new Double(1.95996));
        tauTable.put(new Double(0.99), new Double(2.57583));
    }

    /**
     * Half-width of the confidence interval for estimating the mean.
     * @param percent The percentage, e.g., 0.9 = 90 %
     */
    public double confidenceInterval(double percent) {
        Double factor = (Double) tauTable.get(new Double(percent));
        if (factor == null) {
            throw new RuntimeException("Cannot compute " + percent + "% "
                                       + "confidence interval!");
        }
        return factor.doubleValue() * standardDeviation();
    }

    /**
     * Returns the minimum value of the data set.
     */
    public double min() {
        return min;
    }

    /**
     * Returns the index of the minimum value of the data
     * set.
     */
    public int mindex() {
        return mindex;
    }

    /**
     * Returns the maximum value of the data set.
     */
    public double max() {
        return max;
    }

    /**
     * Returns the index of the maximum value of the data
     * set.
     */
    public int maxdex() {
        return maxdex;
    }

    /**
     * Returns the sample range (max - min) of the data set.
     */
    public double sampleRange() {
        return max - min;
    }

}

