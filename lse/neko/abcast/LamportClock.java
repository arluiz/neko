package lse.neko.abcast;


/**
 * An implementation of Lamport's logical clock.
 */
public class LamportClock {

    private int clock;

    public LamportClock() {
        clock = 0;
    }

    /**
     * Call upon a send event.
     */
    public void update() {
        clock++;
    }

    /**
     * Call upon a receive event.
     */
    public void update(int timestamp) {
        if (clock < timestamp) {
            clock = timestamp;
        }
        clock++;
    }

    public int getValue() {
        return clock;
    }

}

