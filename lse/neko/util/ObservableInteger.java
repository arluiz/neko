package lse.neko.util;

// java imports:
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


public class ObservableInteger {

    public int get() {
        return above.get();
    }

    public void set(int value) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                       "{0} set to {1,number,#}",
                       new Object[] { this, new Integer(value) });
        }
        above.set(value);
        below.set(-value);
    }

    private GettingAboveObservableInteger above;
    private GettingAboveObservableInteger below;

    public ObservableInteger(int value) {
        above = new GettingAboveObservableInteger(value);
        below = new GettingAboveObservableInteger(-value);
    }

    public void addObserver(Observer observer,
                            int threshold,
                            boolean getsBelow)
    {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                       "{0} adding observer {1} updated when "
                       + "value gets {2} {3,number,#}",
                       new Object[] { this, observer,
                           (getsBelow) ? "below" : "above",
                           new Integer(threshold) });
        }
        if (getsBelow) {
            below.addObserver(observer, -threshold);
        } else {
            above.addObserver(observer, threshold);
        }
    }

    /**
     * Very simple implementation. Observers
     * cannot be removed and only get null params when
     * its update method is called.
     */
    private class GettingAboveObservableInteger {

        public GettingAboveObservableInteger(int value) {
            this.value = value;
        }

        private int value;

        public int get() {
            return value;
        }

        public void set(int newValue) {
            List observers = new ArrayList();
            synchronized (this) {
                if (newValue > value) {
                    SortedMap tail =
                        thresholdToObserver.tailMap(new Integer(value));
                    Iterator it = tail.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        int threshold = ((Integer) entry.getKey()).intValue();
                        if (threshold >= newValue) {
                            break;
                        }
                        observers.add(entry.getValue());
                    }
                }
                value = newValue;
            }
            Iterator it = observers.iterator();
            while (it.hasNext()) {
                Observer observer = (Observer) it.next();
                observer.update(null, null);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                               "{0} updating observer {1}",
                               new Object[] { this, observer });
                }
            }
        }

        private SortedMap thresholdToObserver = new TreeMap();

        synchronized void addObserver(Observer observer, int threshold) {
            // FIXME: it is not possible to have several observers
            // for the same threshold
            thresholdToObserver.put(new Integer(threshold), observer);
        }

    }

    private static final Logger logger =
        NekoLogger.getLogger(ObservableInteger.class.getName());
}
