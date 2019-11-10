package lse.neko.tools.logView;
// java imports:
import java.awt.Color;
import java.util.Arrays;


/**
 * This class implements a very useful object for our project. It
 * reprensents an arrow, a line with a start and an end, containing an
 * event we construct as a nekovent (see its doc).
 * @author Jennifer Muller
 */
class Arrow implements Comparable {

    private float departureTime;
    private float arrivalTime;
    private NekoEvent event;
    private int toProcess;

    private String label = null;
    private Color color = Color.red;

    /**
     * Constructor takes only three fields, two times and an
     * associated event.
     */
    Arrow(float departureTime,
          float arrivalTime,
          NekoEvent event,
          int toProcess)
    {
        if (event == null
            || 0 > Arrays.binarySearch(event.getToProcess(), toProcess))
        {
            throw new IllegalArgumentException();
        }
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.event = event;
        this.toProcess = toProcess;
        label = event.getContent();
    }

    /**
     * getDepartureTime() returns the departure time of this arrow.
     */
    public float getDepartureTime() {
        return this.departureTime;
    }

    /**
     * This method returns the arrival time of this arrow.
     */
    public float getArrivalTime() {
        return this.arrivalTime;
    }

    /**
     * This method sets the arrival time of this arrow to the
     * specified float value.
     */
    public void setArrivalTime(float f) {
        this.arrivalTime = f;
    }

    /**
     * This method sets the departure time of this arrow to the
     * specified float value.
     */
    public void setDepartureTime(float f) {
        this.departureTime = f;
    }
    /**
     * Returns the origin process of the neko event associated with this arrow.
     */
    public int getFromProcess() {
        return event.getFromProcess();
    }

    /**
     * Returns the destination process of the neko event associated
     * with this arrow.
     */
    public int getToProcess() {
        return toProcess;
    }

    public int[] getToProcesses() {
        return event.getToProcess();
    }

    /**
     * This method returns the content of the neko event associated
     * with this arrow.
     */
    public String getContent() {
        return event.getContent();
    }

    /**
     * Returns the type of the neko event associated with this arrow.
     */
    public String getType() {
        return event.getType();
    }

    /**
     * This method returns the action of the neko event associated
     * with this arrow.
     */
    public String getAction() {
        return event.getAction();
    }

    /**
     * Sets the color of the arrow to the specified one.
     */
    public void setColor(Color c) {
        this.color = c;
    }

    /**
     * Sets the label of this arrow to the specified string.
     */
    public void setLabel(String l) {
        this.label = l;
    }

    /**
     * Returns the color of this arrow.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the label of this arrow.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Method inherited from the interface Comparable. It compares
     * based on time, both departure and arrival times.
     */
    public int compareTo(Object obj) {

        Arrow toCompare = (Arrow) obj;
        float oneArrival = this.getArrivalTime();
        float oneDeparture = this.getDepartureTime();
        float twoArrival = toCompare.getArrivalTime();
        float twoDeparture = toCompare.getDepartureTime();

        float oneMin, oneMax, twoMin, twoMax;
        if (oneArrival < oneDeparture) {
            oneMin = oneArrival;
            oneMax = oneDeparture;
        } else {
            oneMin = oneDeparture;
            oneMax = oneArrival;
        }
        if (twoArrival < twoDeparture) {
            twoMin = twoArrival;
            twoMax = twoDeparture;
        } else {
            twoMin = twoDeparture;
            twoMax = twoArrival;
        }

        if (oneMin < twoMin) {
            return -1;
        } else if (oneMax > twoMax) {
            return 1;
        } else {
            return 0;
        }
    }
}
