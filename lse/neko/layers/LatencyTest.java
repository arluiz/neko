package lse.neko.layers;

// java imports:
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// lse.neko imports:
import lse.neko.ActiveReceiver;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.Util;


/**
 * A class that helps implementing performance tests that measure
 * latency. A test consists of runs, numbered from 0, and these
 * runs are independent measurements. Put an object of
 * this class on the protocol stack of process #0.
 * Before the test, all processes should create an EventCollector.
 * During the test, they should register events with
 * EventCollector.register(); the time of occurence is registered
 * along with the event. After the test, they should call
 * EventCollector.finish(). This will send all events collected
 * to the LatencyTest on process #0. The events are grouped by runs
 * and can be processed by the application.
 */
public class LatencyTest
    extends ActiveReceiver
{

    public void run() {
        // does not do anything
    }

    /**
     * An event with its name, the number of the run it belongs to
     * and its occurence time.
     */
    public static class Event
        implements Serializable
    {

        private String name;
        private int run;
        private double time;

        public void set(String newName, int newRun, double newTime) {
            this.name = newName;
            this.run = newRun;
            this.time = newTime;
        }

        public String getName() {
            return name;
        }

        public int getRun() {
            return run;
        }

        public double getTime() {
            return time;
        }

        public String toString() {
            return "Event(" + name + "," + run + ","
                + Util.timeToString(time) + ")";
        }

    }

    /**
     * Events on the wire. The content of EVENTS messages has this type.
     */
    private static class EventsOnWire
        implements Serializable
    {
        /**
         * The time when this object was created (and sent).
         */
        private double creationTime;
        /**
         * The events.
         */
        private Event[] events;

        public String toString() {
            String r = "EventsOnWire(time=" + Util.timeToString(creationTime);
            for (int i = 0; i < events.length; i++) {
                r += ", " + events[i];
            }
            r += ")";
            return r;
        }

    }

    /**
     * Message type.
     */
    public static final int EVENTS = 134;
    static {
        MessageTypes.instance().register(EVENTS, "EVENTS");
    }

    public void deliver(NekoMessage m) {
        if (m.getType() == EVENTS) {
            super.deliver(m);
        } else {
            throw new UnexpectedMessageException(m);
        }
    }

    /**
     * Register the occurence of events important for your measurement
     * with this class.
     * Once all events are registered, call finish().
     * You can put objects of this class
     * on the protocol stack. If you don't, it will send messages using
     * the NekoProcess.
     *
     * XXX: the following classes probably need to be modified to get
     * this EventCollector to work :
     * benchmarks.MulticastLatencyTestCoordinator
     * benchmarks.MulticastLatencyTestSlave
     * consensus.tests.LatencyTestClient
     */
    public static class EventCollector
        extends ProtocolImpl
    {

        /**
         * Identifier of the LatencyTest object that this EventCollector
         * should send information to when finish() is called.
         */
        private Object latencyTestId = null;

        private Event[] events;
        private int size;

        private NekoProcess process;

        /**
         * @param capacity The maximum number of events
         * that can be registered.
         * We need this limitation to avoid the
         * creation of new objects in register().
         */
        public EventCollector(NekoProcess process, int capacity) {
            this.process = process;
            events = new Event[capacity];
            for (int i = 0; i < capacity; i++) {
                events[i] = new Event();
            }
            size = 0;
        }

        /**
         * Sets the identifier of the LatencyTest object that
         * receives the EventCollector information when the
         * finish method is called.
         *
         * @param latencyTestId Identifier of the LatencyTest object
         *
         * @see #finish()
         */
        public void setLatencyTestId(Object latencyTestId) {
            this.latencyTestId = latencyTestId;
        }

        private SenderInterface sender;

        public void setSender(SenderInterface sender) {
            this.sender = sender;
        }

        /**
         * Call whenever there is an event worth registering.
         * This method should run fast - this is why it does
         * not allocate anything on the heap and
         * simply fails (index out of bounds)
         * if there are too many events.
         */
        public void register(String name, int run) {
            events[size].set(name, run, process.clock());
            size++;
        }

        /**
         * Like the other register(), but one can specify
         * the time of occurence for the event.
         */
        public void register(String name, int run, double clock) {
            events[size].set(name, run, clock);
            size++;
        }

        public void finish() {
            EventsOnWire eventsOnWire = new EventsOnWire();
            eventsOnWire.creationTime = process.clock();
            eventsOnWire.events = new Event[size];
            System.arraycopy(events, 0, eventsOnWire.events, 0, size);

            // to generate an error if register is called again
            events = null;

            if (latencyTestId == null) {
                throw new RuntimeException("Call setLatencyTestId() before "
                        + "calling finish() in LatencyTest.EventCollector");
            }
            int[] dest = { 0 };
            NekoMessage m = new NekoMessage(process.getID(),
                                            dest,
                                            latencyTestId,
                                            eventsOnWire,
                                            EVENTS);
            sender.send(m);

            // unregister from the Dispatcher
            // TODO: temporary solution; all classes that use EventCollector
            // should use unique IDs instead.
            process.getDispatcher().removeProtocol(getId());
        }

    }

    public LatencyTest(NekoProcess process) {
        super(process, "LatencyTest");

        defaultGroup = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            defaultGroup[i] = i;
        }
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private int[] defaultGroup;

    public EventsIterator getEvents() {
        return getEvents(defaultGroup);
    }

    public EventsIterator getEvents(int[] group) {

        Arrays.sort(group);

        // holds all events on all processes
        EventsOnWire[] allEventsOnWire = new EventsOnWire[group.length];

        // collect the events
        for (int i = 0; i < group.length; i++) {
            NekoMessage m = receive();
            if (m.getType() != EVENTS) {
                throw new UnexpectedMessageException(m);
            }
            EventsOnWire eventsOnWire = (EventsOnWire) m.getContent();
            int source = m.getSource();
            int index = Arrays.binarySearch(group, source);
            if (index < 0) {
                throw new RuntimeException("Received events message from"
                                           + " process #" + source
                                           + ", not part of"
                                           + " group " + Util.toString(group)
                                           + ": " + m);
            }
            if (allEventsOnWire[index] != null) {
                throw new RuntimeException("Received events twice from"
                                           + " process #" + source + ": " + m);
            }
            allEventsOnWire[index] = eventsOnWire;
        }

        // group the events by runs and compute
        // the time when the test was finished
        List eventsByRun = new ArrayList();
        double endTime = Double.MAX_VALUE;

        for (int i = 0; i < allEventsOnWire.length; i++) {
            EventsOnWire eventsOnWire = allEventsOnWire[i];

            if (eventsOnWire.creationTime < endTime) {
                endTime = eventsOnWire.creationTime;
            }

            Event[] eventArray = eventsOnWire.events;
            for (int j = 0; j < eventArray.length; j++) {
                Event event = eventArray[j];

                int run = event.getRun();
                if (eventsByRun.size() <= run) {
                    int n = run + 1 - eventsByRun.size();
                    eventsByRun.addAll(Collections.nCopies(n, null));
                    // was: eventsByRun.setSize(run + 1);
                }
                Events events = (Events) eventsByRun.get(run);
                if (events == null) {
                    events = new Events();
                    eventsByRun.set(run, events);
                }
                events.add(event);

            }
        }

        // consistency checks:
        // the first event of run r+1 must have occurred after
        // the last event of run r
        // and the collected events should only be sent once
        // all processes finished executing the test
        if (eventsByRun.size() >= 1) {
            double latestSoFar = ((Events) eventsByRun.get(0)).latestTime;
            for (int i = 1; i < eventsByRun.size(); i++) {
                Events events = (Events) eventsByRun.get(i);
                if (events == null) {
                    continue;
                }
                if (events.earliestTime < latestSoFar) {
                    // throw new RuntimeException("The events of run #" + i
                    //                            + "overlap"
                    //                            + " with the events of run #"
                    //                            + (i-1));
                }
                latestSoFar = events.latestTime;
            }
            if (latestSoFar > endTime) {
                // throw new RuntimeException("A process sent its "
                //                            + "collected data"
                //                            + " before the end of the test");
            }
        }

        Iterator it = eventsByRun.iterator();
        return new EventsIterator(it);

    }

    /**
     * Typed iterator returned by getEvents().
     */
    public static class EventsIterator {

        private Iterator it;

        private EventsIterator(Iterator it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public Events next() {
            return (Events) it.next();
        }

    }

    /**
     * Stores events for a given run.
     */
    public static class Events {

        private Map map = new HashMap();
        private double earliestTime = Double.MAX_VALUE;
        private double latestTime = Double.MIN_VALUE;
        private int run = -1;

        private void add(Event event) {
            map.put(event.getName(), event);
            double time = event.getTime();
            if (time < earliestTime) {
                earliestTime = time;
            } else if (time > latestTime) {
                latestTime = time;
            }
            if (run == -1) {
                run = event.getRun();
            } else if (run != event.getRun()) {
                throw new RuntimeException("Only insert events "
                                           + "of the same run!");
            }
        }

        public Event get(String name) {
            return (Event) map.get(name);
        }

        public int getRun() {
            return run;
        }

    }

}


