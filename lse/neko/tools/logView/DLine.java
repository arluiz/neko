package lse.neko.tools.logView;

// java imports:
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;


/**
 * DLine implements an arrow to be displayed in GarphicPanel. X1, x2,
 * y1,y2 are the coordinates of the line, and xpoints, ypoints are
 * containing points in order to draw the arrowhead.  the arrowhead is
 * automatically oriented int the axis of the arrow, using the
 * transformHead method.
 * <p>DLine is also used to draw the process
 * lines. In this case, the arrowhead and the label are not
 * calculated.
 */
class DLine extends Line2D.Double {

    int x1, x2, y1, y2;
    double tangent = 0;
    Point labellocation;
    int[] xpoints = new int[5];
    int[] ypoints = new int[5];
    int[] labelx = new int[5];
    int[] labely = new int[5];

    Point[] connection = new Point[2];

    int halign = 0;
    int valign = 0;
    int distance = 0;
    double percent = 0;

    String label = null;
    Color color = Color.blue;

    /**
     * Constructs a DLine object with the specified coordinates.
     */
    public DLine(int x1, int y1, int x2, int y2) {

        super(x1, y1, x2, y2);
        this.x1 = x1;
        this.x2 = x2;
        this.y2 = y2;
        this.y1 = y1;
        int[] xpts = {0, 18, 9, 9, 0};
        int[] ypts = {0, 9, 9, 18, 0};
        if (y1 != y2) {
            transformHead(xpts, ypts);
        }
    }

    /**
     * This method puts the given generic arrowhead to the end of the
     * arrow line, oriented in the good way.
     */
    private void transformHead(int[] newXPoints, int[] newYPoints) {

        AffineTransform at = new AffineTransform(1.0, 0.0, 0.0, 1.0, x2, y2);
        double atang = 0;

        if (x2 > x1) {
            tangent = ((double) y2 - (double) y1) / ((double) x2 - (double) x1);
            atang = Math.atan(tangent);
        } else {
            tangent = ((double) y2 - (double) y1) / ((double) x2 - (double) x1);
            atang = Math.PI + Math.atan(tangent);
        }
        at.rotate(3 * Math.PI / 4 + atang);

        for (int i = 0; i < newXPoints.length; i++) {

            Point p = new Point(newXPoints[i], newYPoints[i]);
            Point p2 = new Point(0, 0);

            at.transform(p, p2);
            this.xpoints[i] = (int) p2.getX();
            this.ypoints[i] = (int) p2.getY();
        }
    }

    /**
     * Method to transform the generic label coordinates in order to
     * draw them at the correct place with the correct dimensions.
     */
    private void transformLabel(int[] newLabelX, int[] newLabelY) {

        if (label != null) {

            int a = 1;
            if (x2 > x1 && y2 > y1) {
                a = -1;
            }

            int distancex =
                (int) (-a * distance * Math.sin(Math.atan(tangent)));
            int distancey =
                (int) (a * distance * Math.cos(Math.atan(tangent)));
            AffineTransform at =
                new AffineTransform(1.0,
                                    0.0,
                                    0.0,
                                    1.0,
                                    distancex
                                    + halign * (10 + label.length() * 3)
                                    + x1 + (x2 - x1) * percent,
                                    distancey + valign * 10
                                    + y1 + (y2 - y1) * percent);
            for (int i = 0; i < 5; i++) {

                Point p = new Point(newLabelX[i], newLabelY[i]);
                Point p2 = new Point(0, 0);

                at.transform(p, p2);

                this.labelx[i] = (int) p2.getX();
                this.labely[i] = (int) p2.getY();

            }
            labellocation = new Point(this.labelx[0] + 5, this.labely[0] + 15);

            // defines the coordinates of the connecting line between
            // the label ans its arrow

            connection[0] =
                new Point((int) (distancex + x1 + (x2 - x1) * percent),
                          (int) (distancey + y1 + (y2 - y1) * percent));
            connection[1] = new Point((int) (x1 + (x2 - x1) * percent),
                                      (int) (y1 + (y2 - y1) * percent));
        }
    }

    /**
     * Sets the label's content.
     */
    public void setLabel(String l) {

        if (l != null) {
            this.label = l;

            int[] newLabelX = {
                -10 - label.length() * 3,
                10 + label.length() * 3,
                10 + label.length() * 3,
                -10 - label.length() * 3,
                -10 - label.length() * 3
            };
            int[] newLabelY = {
                -10, -10, 10, 10, -10
            };
            transformLabel(newLabelX, newLabelY);
        }
    }

    /**
     * Method that is called to define display parameters.  these two
     * parameters define where the connection must be on the
     * rectangle.  horizontalAlignment : 1=left, 0=center, -1=right ;
     * verticalAlignment : 1=up , 0=center, -1=down DistanceFromLine:
     * Distance of the connecting point of the rectangle to the
     * message line.  percentOfTheLine : Position of the label (in
     * percents of the length of the arrow) beside the arrow:
     * 0=beginning, 100=end
     */
    void setLabelPreferences(int horizontalAlignment,
                             int verticalAlignment,
                             int distanceFromLine,
                             double percentOfTheLine)
    {
        valign = verticalAlignment;
        halign = horizontalAlignment;
        distance = distanceFromLine;
        percent = percentOfTheLine;
    }

    /**
     * Sets the color of the arrow to draw.
     */
    public void setColor(Color c) {
        this.color = c;
    }

    /**
     * Sets the terminaison of the LostMessage which have to have a
     * different "head" from the real messages' terminaison.
     */
    public void setLostTerminaison() {
        int[] xpts = {0, 10, -10, 0, 0};
        int[] ypts = {0, -10, 10, 0, 0};
        transformHead(xpts, ypts);
    }
}
