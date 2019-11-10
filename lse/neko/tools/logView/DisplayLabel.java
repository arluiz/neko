package lse.neko.tools.logView;

/**
 * This class is used to simplified the content of an object XML
 * information. Instead off adding four more int to the constructor of
 * XmlInformation, I choose to create a new object and simply add it
 * to the contructor of XmlInformation. Therefore this class is very
 * short and seems to be useless.
 * @author Jennifer Muller
 */

class DisplayLabel {

    private int halign, valign, distance;
    private double percent;

    DisplayLabel(int halign, int valign, int distance, double percent) {
        this.halign = halign;
        this.valign = valign;
        this.distance = distance;
        this.percent = percent;
    }

    /**
     * Is use to get the horizontal alignement of the label. Can take
     * only three values; -1,0 or 1 meanings left, middle or right
     */
    public int getHAlign() {
        return this.halign;
    }

    /**
     * Is use to get the vertical alignement of the label. Can take
     * only three values; -1,0 or 1 meanings high, middle or bottom
     */
    public int getVAlign() {
        return this.valign;
    }

    /**
     * Is used to get the distance in pixel between the rectangle and
     * throws label.
     */
    public int getDistance() {
        return this.distance;
    }

    /**
     * Is used to get the percent defining the position of the label
     * along the arrow, In percent.  0 means start of the arrow, and
     * of course, 100 the end.
     */
    public double getPercent() {
        return this.percent;
    }
}
