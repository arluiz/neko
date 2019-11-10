package lse.neko.tools.logView;

// java imports:
import java.awt.Color;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List; // ambiguous with: java.awt.List
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Class NekoReader read traces files of Neko. It reads all files
 * first and then tries to create arrow object from all the
 * information it has stored in memory. After having do all this work,
 * this class is also able to sort the arrows and the inherent lost
 * messages.  So it create two principal list of Lost Message and
 * Arrow that are destined to be display (All filter about the user
 * options are done here, ie like process = "211" display = "false".
 * <p>
 * This class should be executed by a thread and it must finish when
 * the two lists are created and filtered according to the user
 * options.
 * <p>
 * Because the amount of data to read can be huge we use a lot of
 * HashMap and HashSet to optimize the time the thread work, so we try
 * to avoid any loop that could slow the execution of LogView.
 *
 * @author Jennifer Muller [parsing, storage, matching of event
 * together (creation of arrows)]
 * @author Marton Galanthay     [filters, colors, hex]
 */
class NekoReader implements Runnable {

    /**
     * List of files to be read (Neko traces files).
     */
    private List nekoDataFile;

    private XmlInformation xmlData;

    /**
     * Display info on process.
     */
    private Map xmlDisplayProcess;

    /**
     * Display info on messages.
     */
    private List xmlDisplayMessages;

    private static final char SENDER = 's';
    private static final char RECEIVER = 'r';
    private static final int SENDER_ACTION = 0;
    private static final int RECEIVER_ACTION = 1;
    private static final int OTHER_ACTION = 2;

    // The following variables are the main part of the data strucutre
    // we create by reading log files

    private Map mapOnEvent = new HashMap();

    private List lostMessages = new ArrayList();
    private List listOfArrows = new ArrayList();

    /**
     * The constructor takes only one argument, the XML Data. It
     * initializes three variables for the following work.
     */
    NekoReader(XmlInformation xmlData) {
        this.xmlData = xmlData;
        nekoDataFile = xmlData.getLogFiles();
        // information to filter messages and process that have to be
        // drawn or not
        xmlDisplayProcess = xmlData.getProcessDisplay();
        xmlDisplayMessages = xmlData.getMessagesDisplay();
    }

    /**
     * Method run, it manages all the work of the thread. It first
     * read all files, store data in memory, match event together and
     * finally filter them. And before going back to the caller of the
     * run it make the two lists of arrows and of lost message
     * available.
     */
    public void run() {

        BufferedReader nekoStream = null;
        boolean loop = true;

        for (int i = 0; i < nekoDataFile.size(); i++) {
            File toRead = (File) nekoDataFile.get(i);
            // Trying to open a stream on the file. Since the file
            // exists, we have checked before, it should minimize IO
            // Exception, but Exception are always possible...
            //
            try {
                nekoStream = new BufferedReader(new FileReader(toRead));
            } catch (Exception e) {
                System.out.println("Stream problems in NekoTreatment");
                // FIXME
            }
            // Once the stream to the file is open we read the file
            // line by line and check each one. This manner to read
            // line by line is convenient because in neko traces
            // files, element of data are line. I mean each line of
            // data is independant of the other. So we loop and the
            // line till meeting the end of the document
            while (loop) {
                try {
                    String aLine = nekoStream.readLine();
                    if (aLine != null) {
                        // Ok, we have a line of data, but it is
                        // possible that a white line! Here we first
                        // check before extracting data
                        if (!aLine.equals("")) {
                            parseLine(aLine);
                        }
                    } else {
                        // If the line is null, it means that we have met
                        // the end of the file. So we stop looping and
                        // fetch another file if present. If not we leave
                        // reading and go ahead filtering data and
                        // matching events together
                        loop = false;
                        nekoStream.close();
                    }
                } catch (EOFException k) {
                    loop = false;
                } catch (IOException k) {
                    loop = false;
                }
            }
            loop = true;
        }

        // After having read all the file, some action are taken on
        // the data stored in memory. We first filter process which
        // are not asked to be displayed, to avoid useless work.  Then
        // we match event beetween them to create arrow or lost
        // messages. And then take care about the displaying
        // requirements specified in the XML input file
        matchEvents();
        processOptionHandler();
        arrowOptionHandler();
    }

    /**
     * MatchEvent is the method behaving the algorithm to find pairs
     * and twins events that will be joined to create arrow. It calls
     * a method find the pairs in its algorithm to lighten the code
     * and make it readable and also to seperate difficult parts from
     * others.
     */
    private void matchEvents() {
        // We first create an iterator to walk through the set of
        // Event we have in memory. In way that we then can go into
        // the hashtable to exactly which Key we want and values we
        // will get.
        Iterator it = mapOnEvent.entrySet().iterator();

        final List emptyList = new ArrayList();

        while (it.hasNext()) {

            Map.Entry entry = (Map.Entry) it.next();
            it.remove();
            NekoEvent event = (NekoEvent) entry.getKey();
            List[] timeLists = (List[]) entry.getValue();

            // replace null elements by empty lists
            for (int i = 0; i < timeLists.length; i++) {
                if (timeLists[i] == null) {
                    timeLists[i] = emptyList;
                }
            }

            matchEvent(event, timeLists);
        }
    }

    private void matchEvent(NekoEvent event, List[] timeLists) {

        int minSize = timeLists[0].size();
        for (int i = 1; i < timeLists.length; i++) {
            int size = timeLists[i].size();
            if (size < minSize) {
                minSize = size;
            }
        }

        // create arrows
        for (int i = 0; i < minSize; i++) {
            float sendTime = ((Float) timeLists[0].get(i)).floatValue();
            for (int r = 0; r < timeLists.length - 1; r++) {
                float receiveTime =
                    ((Float) timeLists[r + 1].get(i)).floatValue();
                Arrow arrow = new Arrow(sendTime,
                                        receiveTime,
                                        event,
                                        event.getToProcess()[r]);
                listOfArrows.add(arrow);
            }
        }

        // FIXME: some more arrows could be created.

        // create lost messages out of the events not used for the
        // construction of arrows
        for (int i = 0; i < timeLists.length; i++) {
            List timeList = timeLists[i];
            for (int j = minSize; j < timeList.size(); j++) {
                float time = ((Float) timeList.get(j)).floatValue();
                LostMessage m =
                    new LostMessage(time,
                                    (i == 0) ? SENDER : RECEIVER,
                                    event,
                                    (i == 0)
                                    ? event.getFromProcess()
                                    : event.getToProcess()[i - 1]);
                lostMessages.add(m);
            }
        }
    }

    /**
     * Reads a line and extracts tokens from it.
     */
    private void parseLine(String aLine) {

        // Extraction of necessary informations.
        // Tokenize the line to parse the token between spaces.
        StringTokenizer st = new StringTokenizer(aLine, " ");

        // Parsing the first token, should be a time...
        Float time = new Float(st.nextToken());

        int pInUse = processToId(st.nextToken());

        // Handle only events related to messages
        if (!st.nextToken().equals("messages")) {
            return;
        }
        if (!st.nextToken().equals("e")) {
            return;
        }

        // I have decomposed the action of time sn snf in a sender
        // action "s" and the rest, which I name cAction, and s is
        // called identifier
        String action = st.nextToken();
        int actionType;
        String cAction;
        switch (action.charAt(0)) {
        case SENDER:
            actionType = SENDER_ACTION;
            cAction = action.substring(1);
            break;
        case RECEIVER:
            actionType = RECEIVER_ACTION;
            cAction = action.substring(1);
            break;
        default:
            actionType = OTHER_ACTION;
            cAction = action;
            break;
        }

        int fromProcess = processToId(st.nextToken());

        String toProcessList = st.nextToken();
        StringTokenizer stProcess = new StringTokenizer(toProcessList, ",");
        int[] toProcess = new int[stProcess.countTokens()];
        int i = 0;
        while (stProcess.hasMoreTokens()) {
            toProcess[i] = processToId(stProcess.nextToken());
            i++;
        }

        String typeOfMessage = st.nextToken();

        // Read the rest of the line as the content of the
        // message. But it into a String.
        String contentOfMessage;
        if (st.hasMoreTokens()) {
            contentOfMessage = (String) st.nextToken();
            // the end of the line is the content of the message
            while (st.hasMoreTokens()) {
                contentOfMessage.concat(" ");
                contentOfMessage = contentOfMessage.concat(st.nextToken());
            }
        } else {
            contentOfMessage = null;
        }

        // ask create Event to work now..
        createEvent(time,
                    cAction,
                    pInUse,
                    actionType,
                    fromProcess,
                    toProcess,
                    contentOfMessage,
                    typeOfMessage);
    }

    /**
     * This method create an event for our further use. It select
     * information, and decode it. It takes care of different case
     * between sending event and receiving event It also treats the
     * sending of multicast message as several point to point
     * messages.
     */
    private void createEvent(Float time,
                             String action,
                             int processInUse,
                             int actionType,
                             int fromProcess,
                             int[] toProcess,
                             String content,
                             String type)
    {
        NekoEvent eventKey = new NekoEvent(action,
                                           fromProcess,
                                           toProcess,
                                           content,
                                           type);

        // IdentifierKey will be useful to call storeEvent. We just
        // transform the char into a String. But it has the same
        // meaning. We do it because we will have to use the Hashcode
        // of the String, and it is not possible to use hashcode for a
        // char.
        int identifierKey;
        switch (actionType) {
        case SENDER_ACTION:
        case OTHER_ACTION:
            if (fromProcess != processInUse) {
                throw new IllegalArgumentException();
            }
            identifierKey = 0;
            break;
        case RECEIVER_ACTION:
            identifierKey = Arrays.binarySearch(toProcess, processInUse);
            if (identifierKey < 0) {
                throw new IllegalArgumentException();
            }
            identifierKey++;
            break;
        default:
            throw new RuntimeException();
        }

        storeEvent(time, eventKey, identifierKey);
    }


    /**
     * This method store two information in two different hash. An
     * HashSet containing object NekoEvent and an HashMap contains
     * NekoEvent and value is the associated Float Object, the time
     * associated with this event.  (Again an HashMap with: identifier
     * -> ArrayOfTime)
     * <p>
     * Identifier can be s or r (cooresponding to SENDER or RECEIVER)
     * nothing else.
     * <p>
     * This method store the information it receives in parameter into
     * the hashmaps and the hashset.  Here is a representation of the
     * structure of data we used to store data.  (K== key and V==
     * value)
     * <pre>
     *                   HashMap(mapOnEvent)   HashSet ---> NekoEvent (Object)
     *                       /     \
     *               K:NekoEvent   V:HashMap(SubHashMap)
     *                 Object        /             \
     *                              /               \
     *                       K:identifier           V:ArrayListOfTime
     *                    SENDER Or RECEIVER         Float (the Object)
     *                       String Object
     * </pre>
     */
    private void storeEvent(Float time,
                            NekoEvent eventKey,
                            int identifierKey)
    {

        List[] timeLists = (List[]) mapOnEvent.get(eventKey);
        if (timeLists == null) {
            timeLists = new List[eventKey.getToProcess().length + 1];
            mapOnEvent.put(eventKey, timeLists);
        }

        List timeList = timeLists[identifierKey];
        if (timeList == null) {
            timeList = new ArrayList();
            timeLists[identifierKey] = timeList;
        }

        timeList.add(time);
    }

    /**
     * This method gets rid of the non-wanted processes' corresponding
     * arrows. In the XML file one can specify whether a process will
     * be displayed or not.  If not, every arrow that comes from this
     * process or goes through this process is to be eliminated.  Same
     * for the lost messages.
     */
    private void processOptionHandler() {

        DisplayProcess dm = null;

        for (int j = 0; j < listOfArrows.size(); j++) {
            Arrow toTest = (Arrow) listOfArrows.get(j);
            int to = toTest.getToProcess();
            int from = toTest.getFromProcess();
            if (xmlDisplayProcess.containsKey(String.valueOf(from))
                || xmlDisplayProcess.containsKey(String.valueOf(to)))
            {

                DisplayProcess dp1 =
                    (DisplayProcess)
                    xmlDisplayProcess.get(String.valueOf(from));
                DisplayProcess dp2 =
                    (DisplayProcess)
                    xmlDisplayProcess.get(String.valueOf(to));

                boolean toDisplay = true;
                try {
                    toDisplay = dp1.getDisplayBoolean();
                } catch (Exception e) {
                }
                if (toDisplay) {
                    try {
                        toDisplay = dp2.getDisplayBoolean();
                    } catch (Exception e) {
                    }
                }
                if (!toDisplay) {
                    listOfArrows.remove(j);
                    j--;
                }
            }
        }
        LostMessage lm = null;

        for (int j = 0; j < lostMessages.size(); j++) {

            lm = (LostMessage) lostMessages.get(j);
            NekoEvent ne = lm.getEvent();
            int from = ne.getFromProcess();
            // FIXME: bogus code. It used to be
            // int to = ne.getToProcess();
            // but NekoEvent changed.
            int to = ne.getToProcess()[0];

            if (xmlDisplayProcess.containsKey(String.valueOf(from))
                || xmlDisplayProcess.containsKey(String.valueOf(from)))
            {

                DisplayProcess dp1 =
                    (DisplayProcess)
                    xmlDisplayProcess.get(String.valueOf(from));
                DisplayProcess dp2 =
                    (DisplayProcess)
                    xmlDisplayProcess.get(String.valueOf(to));

                boolean toDisplay = true;
                try {
                    toDisplay = dp1.getDisplayBoolean();
                } catch (Exception e) {
                }

                if (toDisplay) {
                    try {
                        toDisplay = dp2.getDisplayBoolean();
                    } catch (Exception e) {
                    }
                }
                if (!toDisplay) {
                    lostMessages.remove(j);
                    j--;
                }
            }
        }
    }

    /**
     * This method sets the color and the label text of each arrow
     * according to the specifications in the XML file.
     */
    private void arrowOptionHandler() {

        DisplayMessages dm = null;
        Arrow arrow = null;
        for (int i = 0; i < xmlDisplayMessages.size(); i++) {
            dm = (DisplayMessages) xmlDisplayMessages.get(i);

            for (int j = 0; j < listOfArrows.size(); j++) {
                arrow = (Arrow) listOfArrows.get(j);
                if (arrowCorresponding(arrow , dm)) {
                    if (dm.getLabel() != null) {
                        arrow.setLabel(dm.getLabel());
                    } else {
                        String contentLabel = arrow.getContent();
                        arrow.setLabel(contentLabel);
                    }
                    arrow.setColor(stringToColor(dm.getColor()));
                }
            }
        }
    }

    /**
     * This method check if the arrow sent as argument corresponds to
     * the specifications described in th XML file.
     */
    private boolean arrowCorresponding(Arrow arrow, DisplayMessages display) {

        int displaysrc = display.getSource();
        int [] displaydest = display.getDestination();
        String displaytype = display.getType();

        if (!arrow.getType().equals(displaytype)) {
            return false;
        }
        if (arrow.getFromProcess() == displaysrc || displaysrc == -1) {
            if (displaydest != null) {
                for (int k = 0; k < displaydest.length; k++) {
                    if (displaydest[k] == arrow.getToProcess()) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * This method transforms a String object (color name) to the
     * corresponding Color object. There are two ways to specify
     * thecolor name: <ul><li>Full name (blue, green...) these are
     * pre-determined colors.</li> <li>Hexadecimal notation: standard
     * hex notation(#123456)</li></ul>
     */
    private Color stringToColor(String colorName) {
        Color ret = Color.black;
        if (colorName.charAt(0) == '#' && colorName.length() == 7) {

            String colorHex = colorName.substring(1);

            int r = hexToInt2Char(colorHex.substring(0, 2));
            int g = hexToInt2Char(colorHex.substring(2, 4));
            int b = hexToInt2Char(colorHex.substring(4, 6));

            ret = new Color(r, g, b);
        } else {
            if (colorName.equals("blue")) {
                ret = Color.blue;
            }
            if (colorName.equals("magenta")) {
                ret = Color.magenta;
            }
            if (colorName.equals("cyan")) {
                ret = Color.cyan;
            }
            if (colorName.equals("gray")) {
                ret = Color.gray;
            }
            if (colorName.equals("green")) {
                ret = Color.green;
            }
            if (colorName.equals("orange")) {
                ret = Color.orange;
            }
            if (colorName.equals("pink")) {
                ret = Color.pink;
            }
            if (colorName.equals("red")) {
                ret = Color.red;
            }
            if (colorName.equals("white")) {
                ret = Color.white;
            }
            if (colorName.equals("yellow")) {
                ret = Color.yellow;
            }
        }
        return ret;
    }

    /**
     * This method returns an integer which is the value of the
     * 2-character length hexadecimal number.
     */
    private int hexToInt2Char(String hex) {
        int ret = 0;
        hex.toLowerCase();
        char first = hex.charAt(0);
        char second = hex.charAt(1);

        int firstInt = charToIntHex(first);
        int secondInt = charToIntHex(second);

        ret = 16 * firstInt + secondInt;

        return ret;
    }

    /**
     * This method returns the integer value of the received char,
     * according to the hexadecimal notation.
     */
    private int charToIntHex(char c) {
        int ret = 0;

        if (0 <= c && c < 10) {
            ret = c;
        } else {
            switch (c) {
            case 'a':
                ret = 10;
                break;
            case 'b':
                ret = 11;
                break;
            case 'c':
                ret = 12;
                break;
            case 'd':
                ret = 13;
                break;
            case 'e':
                ret = 14;
                break;
            case 'f':
                ret = 15;
                break;
            default:
                // do not change ret
                break;
            }
        }
        return ret;
    }

    /**
     * Returns the list of arrows computed in these class.
     */
    public List getListOfArrows() {
        return listOfArrows;
    }

    /**
     * Returns the list of lost messages computed in this class.
     */
    public List getLostMessages() {
        return this.lostMessages;
    }

    /**
     * Converts the string given as parameter to the int
     * corresponding, it takes the int from the char at (1). Because the
     * string it receive is in the format pXXX.
     */
    private int processToId(String s) {
        if (!s.startsWith("p")) {
            throw new IllegalArgumentException("Expected p<number>, e.g., p13");
        }
        return Integer.parseInt(s.substring(1));
    }
}
