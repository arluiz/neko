package lse.neko.tools.logView;

// java imports:
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List; // ambiguous with: java.awt.List
import java.util.Map;

// javax imports:
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.Sides;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;


/**
 * The GraphicPanel class handles the display and the printing
 * functions. Its parameters are an object XmlInformation, and two
 * ArrayList objects containing Arrows objects for the first, and Lost
 * message objects for the other. All of these messages will be
 * displayed, with their corresponding process line. Arrow objects and
 * LostMessage objects are first transformed to DLine objects, what
 * can be displayed.
 */
class GraphicPanel
    extends JPanel
    implements AdjustmentListener
{

    // FIXME: this variable was called shift, but it never changed.
    // Probably there is a bug.
    private int theShift;

    /**
     * Size of the displaying area.
     */
    private Dimension size;

    /**
     * Panel on which the messages are drawn.
     */
    private  JPanel drawingArea;

    /**
     * Panel to display the name of the processes.
     */
    private  JPanel labelPanel;

    /**
     * Panel to display the time axis.
     */
    private JPanel axePanel;

    /**
     * List containing DLines to draw the process lines.
     */
    private List process;

    /**
     * List containing the messages to be drawn.
     */
    private List messages;

    /**
     * Number of processes to display.
     */
    private int nbproc = 0;

    /**
     * Time interval to display.
     */
    private int time = 0;

    /**
     * Pixels per milliseconds (x-scale).
     */
    private int ppm;

    /**
     * ppp = pixels per process(y-scale).
     */
    private int ppp;

    /**
     * ScrollPane for drawing area.
     */
    private JScrollPane scroller;

    /**
     * ScrollPane for labelPanel.
     */
    private JScrollPane scroller2;

    /**
     * ScrollPane for axePanel.
     */
    private JScrollPane scroller3;

    /**
     * Messages to display (Containing Arrow objects).
     */
    private List msg;

    /**
     * Lost messages to display (Objects DLine).
     */
    private List lost;

    /**
     * Lost messages to display as received (Objects Arrow).
     */
    private List lostMessages;

    /**
     * Panel to display the name of the processes.
     */
    private List processList;

    /**
     * Viewport.
     */
    private JViewport myViewport;

    /**
     * The earliest time to be displayed.
     */
    private int timeStart;

    /**
     * The latest time to be displayed.
     */
    private int timeEnd;

    /**
     * The DisplayLabel object.
     */
    private DisplayLabel dl;

    /**
     * The information container.
     */
    private XmlInformation displayInfo;

    /**
     * File to print into.
     */
    private File printfile;

    /**
     * Constructs a GraphicPanel object with the specified messages
     * (msg), lost messages and displaying specifications.
     */
    public GraphicPanel(XmlInformation displayInfo,
                        List msg,
                        List lostMessages)
    {
        super();

        this.displayInfo = displayInfo;
        this.msg = msg;
        this.lostMessages = lostMessages;

        Dimension screenSize = displayInfo.getScreenSize();
        int w = (int) screenSize.width;
        int h = (int) screenSize.height;

        timeEnd = displayInfo.getTimeAxisEnd();
        timeStart = displayInfo.getTimeAxisStart();

        if (timeEnd == -1 || timeStart == -1) {
            setDefaultTimes();
        }

        time = timeEnd - timeStart;

        ppm = displayInfo.getTimeAxisxSize();
        ppp = displayInfo.getProcessAxisySize();

        processList = new ArrayList();
        process = new ArrayList();
        messages = new ArrayList();

        if (ppm == -1 || ppp == -1) {
            setDefaultSizes(w, h);
        }

        setOpaque(true);
        size = new Dimension(0, 0);

        dl = displayInfo.getDisplayLabel();
        lost = new ArrayList();

        printfile = displayInfo.getPrinterFile();

        // Set up the drawing area.
        // This JPanel draws on the screen all the DLines, labels.
        drawingArea =
            new JPanel() {
                protected void paintComponent(Graphics g1D)  {

                    Graphics2D g = (Graphics2D) g1D;

                    super.paintComponent(g);
                    BasicStroke lineStroke =
                        new BasicStroke(2.0f,
                                        BasicStroke.CAP_BUTT,
                                        BasicStroke.JOIN_ROUND);
                    g.setStroke(lineStroke);
                    DLine l;
                    // Drawing the process line
                    for (int i = 0; i < process.size(); i++) {
                        l = (DLine) process.get(i);
                        g.setColor(Color.black);
                        g.drawLine(l.x1, l.y1, l.x2, l.y2);
                    }
                    Rectangle view = myViewport.getViewRect();
                    view.grow(200, 0);
                    if (lost != null) {
                        for (int h = 0; h < lost.size(); h++) {
                            DLine lm = (DLine) lost.get(h);
                            g.setColor(Color.black);
                            g.drawLine(lm.x1, lm.y1, lm.x2, lm.y2);
                            Polygon p = new Polygon(lm.xpoints, lm.ypoints, 5);
                            g.drawPolygon(p);
                        }
                    }
                    //Drawing the arrow's line
                    for (int k = 0; k < messages.size(); k++) {

                        l = (DLine) messages.get(k);

                        if (l.intersects(view)) {

                            // Drawing the label of the message.
                            if (l.label != null) {
                                Polygon lab =
                                    new Polygon(l.labelx, l.labely, 5);
                                g.setColor(Color.blue);
                                g.drawPolygon(lab);
                                g.drawString(l.label,
                                             l.labellocation.x,
                                             l.labellocation.y);
                                g.drawLine((int) l.connection[0].getX(),
                                           (int) l.connection[0].getY(),
                                           (int) l.connection[1].getX(),
                                           (int) l.connection[1].getY());
                            }
                            //Drawing the line
                            g.setColor(l.color);
                            g.drawLine(l.x1, l.y1, l.x2, l.y2);

                            // Drawing the arrow's edge contained in
                            // instance variables of DLine.
                            if (view.contains(l.xpoints[0], l.ypoints[0])) {
                                Polygon p = new Polygon(l.xpoints,
                                                        l.ypoints, 5);
                                g.fillPolygon(p);
                            }
                        }
                    }
                }
            };

        drawingArea.setBackground(Color.white);
        // Put the drawing area in a scroll pane.
        scroller = new JScrollPane(drawingArea);
        myViewport = scroller.getViewport();

        // Left panel for labels
        // JPanel on the left side for the process names.
        labelPanel =
            new JPanel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    g.setColor(Color.black);
                    g.drawString("Process", 20, ppp / 2);

                    // Drawing the process names.
                    for (int h = 0; h < nbproc; h++) {
                        if (processList.get(h) instanceof String) {
                            g.drawString((String) processList.get(h),
                                         20,
                                         ppp * (h + 1) + 2);
                        } else {
                            g.drawString(("Process "
                                          + (Integer) processList.get(h)),
                                         20, ppp * (h + 1) + 2);
                        }
                    }
                }
            };

        labelPanel.setBackground(Color.white);
        scroller2 = new JScrollPane(labelPanel);

        // North panel for the axis
        // This JPanel will contain the time axis.
        axePanel =
            new JPanel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    // Drawing the time axis
                    g.setColor(Color.black);
                    g.drawLine(100, 10, ppm * time + 100, 10);

                    // Drawing the numbers, and sticks on it.

                    for (int i = 0; i < time + 1; i++) {
                        g.drawLine(i * ppm + 100, 8, i * ppm + 100, 12);
                        g.drawString("" + (timeStart + i), i * ppm + 100, 23);
                    }
                }
            };

        axePanel.setBackground(Color.white);
        scroller3 = new JScrollPane(axePanel);

        // Layout this panel
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        add(scroller2, BorderLayout.WEST);
        add(scroller3, BorderLayout.NORTH);

        // Properties of scrollBars.
        scroller.setVerticalScrollBarPolicy(JScrollPane
                                                .VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(JScrollPane
                                                  .HORIZONTAL_SCROLLBAR_ALWAYS);
        scroller3.setHorizontalScrollBarPolicy(JScrollPane
                                                   .HORIZONTAL_SCROLLBAR_NEVER);
        scroller2.setVerticalScrollBarPolicy(JScrollPane
                                                 .VERTICAL_SCROLLBAR_NEVER);
        scroller2
            .setHorizontalScrollBarPolicy(JScrollPane
                                              .HORIZONTAL_SCROLLBAR_ALWAYS);

        JScrollBar s1 = scroller.getVerticalScrollBar();
        s1.addAdjustmentListener(this);

        JScrollBar s3 = scroller.getHorizontalScrollBar();
        s3.addAdjustmentListener(this);
        s3.setUnitIncrement(20);

        JScrollBar s2 = scroller2.getVerticalScrollBar();
        s2.addAdjustmentListener(this);

        JScrollBar s4 = scroller3.getHorizontalScrollBar();
        s4.addAdjustmentListener(this);

        // Calling the methods to enable the transformation to DLine objects
        drawMessages();
        drawProcess();

        //Drawing accessories
        drawLabels();
        drawAxe();

        //     SimpleBook sb = new SimpleBook(messages,process,lost);
    }

    /**
     * This method is called if one has not specified start and end
     * parameters in the configuration file. In this case, start =
     * first event to display, end = last event to display.
     */
    private void setDefaultTimes() {

        Arrow arrow = null;

        Collections.sort(msg);

        arrow = (Arrow) msg.get(0);
        float min = arrow.getDepartureTime();

        arrow = (Arrow) msg.get(msg.size() - 1);
        float max = arrow.getArrivalTime();

        if (timeEnd == -1) {
            timeEnd = (int) max + 1;
        }
        if (timeStart == -1) {
            timeStart = (int) min;
        }
    }

    /**
     * This method is called if one has not specified xSize and ySite
     * parameters in the configuration file. In this case, we try to
     * put every message on the screen.
     */
    private void setDefaultSizes(int w, int h) {

        Arrow arrow = null;

        if (ppm == -1) {
            ppm = w / time + 1;
        }

        if (ppp == -1) {

            for (int i = 0; i < msg.size(); i++) {
                arrow = (Arrow) msg.get(i);
                int s = arrow.getFromProcess();
                int r = arrow.getToProcess();
                setProcessID(s);
                setProcessID(r);
            }

            ppp = h / nbproc - 200 / nbproc;

            if (ppp > 600) {
                ppp = 600;
            }
        }
    }

    /**
     * Constructs a Dline object for every process to display.
     */
    public void drawProcess() {

        // tmp given in milliseconds

        DLine li;
        for (int i = 0; i < nbproc; i++) {

            li = new DLine(0, ppp + ppp * i, ppm * time, ppp + ppp * i);
            process.add(li);

            size.height = 80 + ppp * i;
            size.width = ppm * time;
        }

        // Update client's preferred size because the area taken up by
        // the graphics has gotten larger or smaller (if cleared).
        drawingArea.setPreferredSize(size);
    }

    /**
     * For every Arrow object in ArrayList msg this method checks the
     * process names (in order to determine nbproc) and constructs a
     * DLine object to be displayed in the drawing area, which is put
     * in ArrayList messages.
     */
    void drawMessages() {

        DLine line = null;
        Arrow m;

        // this parses the arrows in order to collect all the
        // processes to display, and then sort them.

        for (int i = 0; i < msg.size(); i++) {
            m = (Arrow) msg.get(i);
            int s = m.getFromProcess();
            int r = m.getToProcess();
            setProcessID(s);
            setProcessID(r);
        }

        // Now the process list will be sorted, to let them displayed
        // ordered.
        Collections.sort(processList);

        // This constructs the DLine objects (what could be displayed)
        for (int i = 0; i < msg.size(); i++) {

            m = (Arrow) msg.get(i);

            int s = m.getFromProcess();
            int r = m.getToProcess();
            int spi = processList.indexOf(new Integer(s));
            int rpi = processList.indexOf(new Integer(r));

            int mst = (int) (ppm * (m.getDepartureTime()));
            int mrt = (int) (ppm * (m.getArrivalTime()));

            // We create the DLine object with the informations we
            // obtained before.

            line = new DLine(mst - timeStart * ppm,
                             ppp + ppp * spi,
                             mrt - timeStart * ppm,
                             ppp + ppp * rpi);

            if (dl != null) {
                line.setLabelPreferences(dl.getHAlign(),
                                         dl.getVAlign(),
                                         dl.getDistance(),
                                         dl.getPercent());
            } else {
                line.setLabelPreferences(1, 1, 50, 0.5);
            }

            line.setColor(m.getColor());

            line.setLabel(m.getLabel());
            messages.add(line);
        }

        if (lostMessages != null) {

            for (int i = 0; i < lostMessages.size(); i++) {

                LostMessage lm = (LostMessage) lostMessages.get(i);

                NekoEvent ne = lm.getEvent();

                Float g = lm.getTime();
                char id = lm.getIdentifier();

                if (g == null) {
                    continue;
                }

                float f = g.floatValue();
                int eventTime = (int) (ppm * f);

                // FIXME: bogus code. It used to be
                // int toProcess = ne.getToProcess();
                // but NekoEvent changed.
                int toProcess = ne.getToProcess()[0];
                int fromProcess = ne.getFromProcess();

                int spi = processList.indexOf(new Integer(fromProcess));
                int rpi = processList.indexOf(new Integer(toProcess));

                if (id == 's') {

                    int dir = 1;
                    if (toProcess < fromProcess) {
                        dir = -1;
                    }

                    line = new DLine(eventTime - timeStart * ppm,
                                     ppp + ppp * spi,
                                     (int) (eventTime - timeStart * ppm
                                            + ppp * 0.7),
                                     (int) (ppp + ppp * spi + (ppp * 0.7 * dir))
                                     );
                    line.setLostTerminaison();
                    line.setColor(Color.black);
                }
                if (id == 'r') {
                    int dir = -1;
                    if (toProcess < fromProcess) {
                        dir = 1;
                    }
                    line = new DLine(eventTime - timeStart * ppm,
                                     ppp + ppp * spi,
                                     (int) (eventTime - timeStart * ppm
                                            - ppp * 0.7),
                                     (int) (ppp + ppp * spi
                                            + (ppp * 0.7 * dir)));
                    line.setLostTerminaison();
                    line.setColor(Color.black);
                }

                lost.add(line);
            }
        }

        drawingArea.revalidate();
        // Let the scroll pane know to update itself and its
        // scrollbars.
        drawingArea.repaint();
    }

    /**
     * In order to determine nbproc, every identifier is put in
     * processList, if processlist does not contain it also.  The
     * number of processes to display will be nbproc.
     */
    void setProcessID(int procnum) {

        Integer ii = new Integer(procnum);
        if (!processList.contains(ii)) {
            processList.add(ii);
        }

        nbproc = processList.size();
    }

    /**
     * Paints the labelPanel.
     */
    public void drawLabels() {

        // Update panel's preferred size because
        Map hm = displayInfo.getProcessDisplay();

        for (int h = 0; h < processList.size(); h++) {

            String key = "" + ((Integer) processList.get(h)).intValue();
            if (hm.containsKey(key)) {
                DisplayProcess dp = (DisplayProcess) hm.get(key);
                String name = dp.getName();
                Object o = processList.set(h, name);
            }
        }

        Dimension dlp = new Dimension(100, size.height);
        labelPanel.setPreferredSize(dlp);
        // Let the scroll pane know to update itself and its
        // scrollbars.
        labelPanel.revalidate();
        labelPanel.repaint();
    }

    /**
     * Paints the time axis.
     */
    public void drawAxe() {

        // Update client's preferred size because the area taken up by
        // the graphics has gotten larger or smaller (if cleared).
        Dimension dap = new Dimension(ppm * time + 120, 30);
        axePanel.setPreferredSize(dap);

        // Let the scroll pane know to update itself and its
        // scrollbars.
        axePanel.revalidate();
        axePanel.repaint();
    }

    /**
     * Listener of the scrollBars. Adjustement policy is defined by
     * display constraints
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {

        Object o = e.getSource();

        JScrollBar s1, s2, s3, s4;
        s1 = scroller.getVerticalScrollBar();
        s2 = scroller2.getVerticalScrollBar();
        s3 = scroller.getHorizontalScrollBar();
        s4 = scroller3.getHorizontalScrollBar();

        if (o == s1) {
            s2.setValue(s1.getValue());
        }
        if (o == s2) {
            s1.setValue(s2.getValue());
        }
        if (o == s3) {
            s4.setValue(s3.getValue());
        }
        if (o == s4) {
            s3.setValue(s4.getValue());
        }
    }

    /**
     * Handles the printing job. Constructs a Book object which sheets
     * are done by calling the drawShapes method with the appropriate
     * shift(to shift the "picture").  Then this Book is printed to an
     * output stream.
     */
    public void print() {

        // SimpleBook sb = new SimpleBook(messages, process, lost, processList);
        PrinterJob job = PrinterJob.getPrinterJob();
        String psMimeType = "application/postscript";

        FileOutputStream outstream = null;
        StreamPrintService psPrinter = null;

        // Create a landscape page format
        PageFormat landscape = job.defaultPage();
        landscape.setOrientation(PageFormat.LANDSCAPE);

        // Set up a book
        Book bk = new Book();
        double w = landscape.getImageableWidth();
        double h = landscape.getImageableHeight();

        // resizedppp =(int)(h-100)/nbproc;

        // The cover page
        bk.append(new PaintCover(), job.defaultPage());
        int nofpages = time * ppm / (int) w + 1;

        for (int i = 0; i < nofpages; i++) {
            // Contents
            bk.append(new PaintContent((int) (i * w), this), landscape);
        }
        job.setPageable(bk);

        try {
            StreamPrintServiceFactory[] spsFactories =
                PrinterJob.lookupStreamPrintServices(psMimeType);

            if (spsFactories.length > 0) {
                try {
                    if (printfile != null) {
                        // Create the output stream to the printer job.
                        // File to be printed
                        outstream = new FileOutputStream(printfile);
                        psPrinter = spsFactories[0].getPrintService(outstream);
                    } else {
                        System.out.println("invalid file");
                        // FIXME
                    }

                    // psPrinter can now be set as the service on a PrinterJob
                } catch (FileNotFoundException exx) { }
            }
            // if app wants to specify this printer.
            job.setPrintService(psPrinter);
            PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            aset.add(Sides.ONE_SIDED);
            aset.add(PrintQuality.HIGH);

            // aset.add(new Copies(2));

            job.print(aset);

            try {
                outstream.close();
            } catch (Exception ex) {
                // FIXME: ignoring exception
            }

        } catch (Exception exxx) {
            System.out.println("something is wrong");
            // FIXME
        }
    }

    /**
     * Paint the printed document's contents.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        drawShapes(g2, theShift);
    }

    /**
     * This method draws what will be printed. The shift attribute is
     * necessary to have different draqings on each page. (It takes
     * the "screenshot" of what is displayed on the screen between
     * x-index shift to the end of the page.)
     */
    void drawShapes(Graphics2D g, int shiftParam) {

        int shift = shiftParam;
        int begin = 80;
        int yadj = 30;
        yadj = yadj + 80;
        DLine l;
        if (shift == 0) {
            g.setColor(Color.black);
            // g.drawString("Process", begin,50);

            for (int h = 0; h < processList.size(); h++) {
                if (processList.get(h) instanceof String) {
                    g.drawString((String) processList.get(h),
                                 begin,
                                 ppp * (h + 1) + 2 + yadj);
                } else {
                    g.drawString(("Process " + (Integer) processList.get(h)),
                                 begin,
                                 ppp * (h + 1) + 2 + yadj);
                }
            }
        }

        l = (DLine) process.get(1);
        g.setColor(Color.red);
        g.drawLine(l.x1 - shift + begin + 80, 80,
                   l.x2 - shift + begin + 80, 80);

        for (int i = 0; i < time + 1; i++) {
            g.drawLine(i * ppm - shift + begin + 80, 78,
                       i * ppm - shift + begin + 80, 82);
            g.drawString("" + (timeStart + i),
                         i * ppm - shift + begin + 80, 93);
        }

        if (true) {
            shift = shift - begin - 80;

            // Graphics2D g= (Graphics2D)g1d;

            int[] xp = new int[5];
            int[] yp = new int[5];

            BasicStroke lineStroke =
                new BasicStroke(2.0f,
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_ROUND);
            g.setStroke(lineStroke);

            // Printing the process line
            for (int i = 0; i < process.size(); i++) {
                l = (DLine) process.get(i);
                g.setColor(Color.black);
                g.drawLine(l.x1 - shift, l.y1 + yadj,
                           l.x2 - shift, l.y2 + yadj);
            }
            // Printing the lost messages
            if (lost != null) {
                for (int h = 0; h < lost.size(); h++) {
                    DLine lm = (DLine) lost.get(h);
                    g.setColor(Color.black);
                    g.drawLine(lm.x1 - shift,
                               lm.y1 + yadj,
                               lm.x2 - shift,
                               lm.y2 + yadj);

                    for (int n = 0; n < 5; n++) {
                        xp[n] = lm.xpoints[n] - shift;
                        yp[n] = lm.ypoints[n] + yadj;
                    }

                    Polygon p = new Polygon(xp, yp, 5);
                    g.drawPolygon(p);
                }
            }

            // Drawing the arrow
            for (int k = 0; k < messages.size(); k++) {

                l = (DLine) messages.get(k);

                // Drawing the label of the message.
                if (l.label != null) {

                    for (int n = 0; n < 5; n++) {
                        xp[n] = l.labelx[n] - shift;
                        yp[n] = l.labely[n] + yadj;
                    }
                    Polygon lab = new Polygon(xp, yp, 5);

                    g.setColor(Color.blue);
                    g.drawPolygon(lab);
                    g.drawString(l.label,
                                 l.labellocation.x - shift,
                                 l.labellocation.y + yadj);

                    g.drawLine((int) l.connection[0].getX() - shift,
                               (int) l.connection[0].getY() + yadj,
                               (int) l.connection[1].getX() - shift,
                               (int) l.connection[1].getY() + yadj);
                }
                //Drawing the line
                g.setColor(l.color);
                g.drawLine(l.x1 - shift, l.y1 + yadj,
                           l.x2 - shift, l.y2 + yadj);

                for (int n = 0; n < 5; n++) {
                    xp[n] = l.xpoints[n] - shift;
                    yp[n] = l.ypoints[n] + yadj;
                }

                Polygon p = new Polygon(xp, yp, 5);
                g.fillPolygon(p);
            }
        }
    }

    /**
     * Class which will draw the cover page.
     */
    static class PaintCover implements Printable {

        Font fnt = new Font("Helvetica-Bold", Font.PLAIN, 48);

        public int print(Graphics g, PageFormat pf, int pageIndex)
            throws PrinterException
        {
            g.setFont(fnt);
            g.setColor(Color.black);
            g.drawString("Neko Log", 100, 200);
            return Printable.PAGE_EXISTS;
        }
    }

    /**
     * A Printable object that contains a sheet of the document to print.
     */
    static class PaintContent implements Printable {

        int i;
        GraphicPanel gp;
        int resizedppp;

        PaintContent(int i, GraphicPanel gp) {
            this.i = i;
            this.gp = gp;
        }

        public int print(Graphics g, PageFormat pf, int pageIndex)
            throws PrinterException
        {
            gp.drawShapes((Graphics2D) g, i);
            return Printable.PAGE_EXISTS;
        }
    }

}
