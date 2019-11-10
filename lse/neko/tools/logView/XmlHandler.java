package lse.neko.tools.logView;

// java imports:
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

// other imports:
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Class XmlHandler is my handler for this application. It handles
 * the special type of xml file we have discussed with peter urban as
 * input for our programm.
 *
 * @author Jennifer Muller
 */
class XmlHandler
    extends DefaultHandler
{

    private List logFiles = new ArrayList();
    private Map process = new HashMap();
    private List messages = new ArrayList();
    private int timeAxisStart = -1;
    private int timeAxisend = -1;
    private int timeAxisxSize = -1;
    private int processAxisySize = -1;
    private int screenxSize = 600;
    private int screenySize = 400;
    private File printerFile = null;
    private DisplayLabel dLabel = null;
    private XmlInformation data = null;

    private boolean defaultValueUsed = false;
    private boolean incorrectValue = false;

    XmlHandler() {
        super();
    }

    /**
     * method that indicates if default values from the DTD have been
     * used to replace values in XML Data.
     */
    public boolean getDefaultValueUsed() {
        return defaultValueUsed;
    }

    /**
     * method that indicates if incorrect values were read. So it
     * alors indicates that some informations in the XML were not
     * undersood -> were not treated
     */
    public boolean getIncorrectValueUsed() {
        return this.incorrectValue;
    }

    /**
     * Method which should be called to obtain the display information.
     */
    public XmlInformation getXmlData() {
        return this.data;
    }

    /**
     * Methode inherited from DefaultHandler. I override it so, when
     * document is finished, I can create an object full of the
     * information I have found in the xml, I construct a
     * xmlInformation object.
     */
    public void endDocument() {
        data = new XmlInformation(timeAxisStart,
                                  timeAxisend,
                                  timeAxisxSize,
                                  processAxisySize,
                                  screenxSize,
                                  screenySize,
                                  process,
                                  messages,
                                  dLabel,
                                  logFiles,
                                  printerFile);
    }

    /**
     * methode heritates from DefaultHandler, it extract information
     * of the XML. By calling it every time an element is
     * started. (the reading of it starts..)
     */
    public void startElement(String namespaceURI,
                             String lName,
                             String qName,
                             Attributes attrs)
    {
        // In this method, i test every name and call a method
        // appropriate to extract the information
        String eName = lName;
        if ("".equals(eName)) {
            eName = qName; //nameSpace aware == false
        }
        if (eName.equals("timeAxis")) {
            timeAxisExtract(attrs);
        } else if (eName.equals("processAxis")) {
            processAxisExtract(attrs);
        } else if (eName.equals("log")) {
            logFileExtract(attrs);
        } else if (eName.equals("window")) {
            windowExtract(attrs);
        } else if (eName.equals("printer")) {
            printerExtract(attrs);
        } else if (eName.equals("process")) {
            processExtract(attrs);
        } else if (eName.equals("messages")) {
            messagesExtract(attrs);
        } else if (eName.equals("label")) {
            labelExtract(attrs);
        }
    }

    /**
     * Extract the information of the first tag, time Axis. Get the
     * values and check them. If values are not specified default
     * values are set, -1 for int and null for String specifies that
     * not values has been specified.
     */
    private void timeAxisExtract(Attributes attrs) {
        String start = attrs.getValue("start");
        String end = attrs.getValue("end");
        String xSize = attrs.getValue("xSize");

        try {
            if (start != null) {
                Integer startInt = new Integer(start);
                timeAxisStart = startInt.intValue();
            } else {
                timeAxisStart = -1;
            }
        } catch (NumberFormatException nfe) {
            defaultValueUsed = true;
            timeAxisStart = -1;
        }
        try {
            if (end != null) {
                Integer endInt = new Integer(end);
                timeAxisend = endInt.intValue();
            } else {
                timeAxisend = -1;
            }
        } catch (NumberFormatException nfe) {
            defaultValueUsed = true;
            timeAxisend = -1;
        }
        try {
            if (xSize != null) {
                Integer xSizeInt = new Integer(xSize);
                timeAxisxSize = xSizeInt.intValue();
            } else {
                timeAxisxSize = -1;
            }
        } catch (NumberFormatException nfe) {
            defaultValueUsed = true;
            timeAxisxSize = -1;
        }
    }

    /**
     * Extracts the information about processAxis, y axis.
     */
    private void processAxisExtract(Attributes attrs) {
        String ySize = attrs.getValue("ySize");

        try {
            if (ySize != null) {
                Integer ySizeInt = new Integer(ySize);
                processAxisySize = ySizeInt.intValue();
            } else {
                processAxisySize = -1;
            }
        } catch (NumberFormatException nfe) {
            defaultValueUsed = true;
            processAxisySize = -1;
        }
    }


    /**
     * Extracts information about the path of neko data files. Add
     * each one to a unique ArrayList of String containg paths
     */
    private void logFileExtract(Attributes attrs) {
        String log = attrs.getValue("filename");
        File logFile = new File(log);
        logFiles.add(logFile);
    }

    /**
     * Extract other information for default display. Will later be
     * part of XmlInformation object.
     */
    private void windowExtract(Attributes attrs) {
        String ySize2 = attrs.getValue("ySize");
        String xSize2 = attrs.getValue("xSize");

        try {
            Integer xSize2Int = new Integer(xSize2);
            screenxSize = xSize2Int.intValue();
            Integer ySize2Int = new Integer(ySize2);
            screenySize = ySize2Int.intValue();
        } catch (NumberFormatException nfe) {
            defaultValueUsed = true;
            screenxSize = 600;
            screenySize = 400;
        }
    }

    /**
     * Extract the name of the file in which we should print graphic
     * if it exists. (the file name...:-))
     */
    private void printerExtract(Attributes attrs) {
        String print = attrs.getValue("filename");
        if (print != null) {
            printerFile = new File(print);
        }
    }

    /**
     * Extract information about process special display. It creates
     * an object DisplayProcess with the values it reads
     */
    private void processExtract(Attributes attrs) {
        String id = attrs.getValue("id");
        String name = attrs.getValue("name");
        String display = attrs.getValue("display");
        boolean bdisplay;

        Boolean displayBo = Boolean.valueOf(display);
        bdisplay = displayBo.booleanValue();

        try {
            Integer idInt = new Integer(id);
            int iid = idInt.intValue();
            DisplayProcess dp = new DisplayProcess(iid, name, bdisplay);
            process.put(id, dp);
        } catch (NumberFormatException nfe) {
            incorrectValue = true;
        }
    }

    /**
     * Extract the information about displaying a message. Create an
     * object DisplayMessage and add it to an arraylist
     */
    private void messagesExtract(Attributes attrs) {

        String type = attrs.getValue("type");
        String label = attrs.getValue("label");
        String src = attrs.getValue("src");
        String dest = attrs.getValue("dest");
        String color = attrs.getValue("color");
        int[] destinations;
        int isrc;
        DisplayMessages disMessage = null;

        // It may be possible that different tags are not present, so
        // we check for possible null value of attributes.
        if (src != null && dest != null) {
            try {
                Integer srcInt = new Integer(src);
                isrc = srcInt.intValue();
                StringTokenizer tokenizer = new StringTokenizer(dest, ",");
                int size = tokenizer.countTokens();
                destinations = new int[size];
                for (int i = 0; i < size; i++) {
                    String iti = tokenizer.nextToken();
                    Integer integerOfDest = new Integer(iti);
                    int idest = integerOfDest.intValue();
                    destinations[i] = idest;
                }
                disMessage = new DisplayMessages(type, label, color,
                                                 isrc, destinations);
            } catch (NumberFormatException nfe) {
                incorrectValue = true;
            }
        } else {
            if (src != null && dest == null) {
                try {
                    Integer srcInt = new Integer(src);
                    isrc = srcInt.intValue();
                    disMessage = new DisplayMessages(type, label, color,
                                                     isrc, null);
                } catch (NumberFormatException nfe) {
                    incorrectValue = true;
                }
            } else {
                if (src == null && dest != null) {
                    StringTokenizer tokenizer = new StringTokenizer(dest, ",");
                    int size = tokenizer.countTokens();
                    destinations = new int[size];
                    try {
                        for (int i = 0; i < size; i++) {
                            String iti = tokenizer.nextToken();
                            Integer integerOfDest = new Integer(iti);
                            int idest = integerOfDest.intValue();
                            destinations[i] = idest;
                        }
                        disMessage = new DisplayMessages(type, label, color,
                                                         -1, destinations);
                    } catch (NumberFormatException nfe) {
                        incorrectValue = true;
                    }
                } else {
                    disMessage = new DisplayMessages(type, label, color,
                                                     -1, null);
                }
            }
        }
        //Add the object to the arraylist messages
        if (disMessage != null) {
            messages.add(disMessage);
        }
    }

    /**
     * This method is called when the element label is
     * encountered. It extract the attributes values and create a
     * DisplayLabel with them
     */
    private void labelExtract(Attributes attrs) {
        String h = attrs.getValue("halign");
        String v = attrs.getValue("valign");
        String dist = attrs.getValue("distance");
        String per = attrs.getValue("percent");
        int halign, valign;

        if (h.equals("left")) {
            halign = 1;
        } else if (h.equals("center")) {
            halign = 0;
        } else {
            halign = -1;
        }

        if (v.equals("top")) {
            valign = 1;
        } else if (v.equals("center")) {
            valign = 0;
        } else {
            valign = -1;
        }

        try {
            Integer distance = new Integer(dist);
            Double percent = new Double(per);
            dLabel = new DisplayLabel(halign, valign,
                                      distance.intValue(),
                                      percent.doubleValue());
        } catch (NumberFormatException nfe) {
            defaultValueUsed = true;
            dLabel = new DisplayLabel(halign, valign, 50, 0.5);
        }
    }
}
