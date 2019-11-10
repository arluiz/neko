package lse.neko.tools.logView;

/**
 * This class contains information about how to display a certain set
 * of messages like the color, the label, etc.
 *
 * @author Jennifer Muller
 */
public class DisplayMessages {

    private String type, label, color;
    private int src;
    private int[] dest;

    DisplayMessages(String type,
                    String label,
                    String color,
                    int src,
                    int[] dest)
    {
        this.type = type;
        this.label = label;
        this.color = color;
        this.src = src;
        this.dest = dest;
    }

    /**
     * Returns the type of messages to which display properties should
     * apply. Can be <code>null</code> if nothing is specified.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns label to be applied. Can be <code>null</code> if
     * nothing is specified.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Returns the color wanted for these messages. Can be
     * <code>null</code> if nothing is specified.
     */
    public String getColor() {
        return this.color;
    }

    /**
     * Returns the source id, for example to uniquely identify
     * messages. Could be 0 if nothing is specified.
     */
    public int getSource() {
        return this.src;
    }

    /**
     * Returns the destinations' ids. Can be
     * <code>null</code> if nothing is specified.
     */
    public int[] getDestination() {
        return this.dest;
    }
}
