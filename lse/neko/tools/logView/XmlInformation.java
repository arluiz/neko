package lse.neko.tools.logView;

// java imports:
import java.awt.Dimension;
import java.io.File;
import java.util.List; // ambiguous with: java.awt.List
import java.util.Map;


/**
 * This class contains all the information to display the graphic
 * like the user has specified in his XML file there is timeAxis and
 * processAxis size, the printer file (the file in which we will print
 * the graphic) the screen size and specific display properties for
 * messages, process and label.
 *
 * @author Jennifer Muller
 */
class XmlInformation {

    private int timeAxisStart, timeAxisEnd, timeAxisxSize;
    private int processAxisySize;
    private Dimension screenSize;
    private File printerFile;
    private Map process;
    private List messages;
    private List logFiles;
    private DisplayLabel dLabel;

    XmlInformation(int timeAxisStart,
                   int timeAxisEnd,
                   int timeAxisxSize,
                   int processAxisySize,
                   int screenxSize,
                   int screenySize,
                   Map process,
                   List messages,
                   DisplayLabel dLabel,
                   List logFiles,
                   File printerFile)
    {
        this.timeAxisStart = timeAxisStart;
        this.timeAxisEnd = timeAxisEnd;
        this.timeAxisxSize = timeAxisxSize;
        this.processAxisySize = processAxisySize;
        this.screenSize = new Dimension(screenxSize, screenySize);
        this.printerFile = printerFile;
        this.process = process;
        this.messages = messages;
        this.dLabel = dLabel;
        this.logFiles = logFiles;
    }

    /**
     * returns the int timeAxisStart but if is is not specified could be zero!
     */
    public int getTimeAxisStart() {
        return timeAxisStart;
    }

    /**
     * returns the int timeAxisEnd but if it is not specified could be zero!
     */
    public int getTimeAxisEnd() {
        return timeAxisEnd;
    }

    /**
     * Returns the int timeAxisxSize but if it is not specified could
     * be zero.
     */
    public int getTimeAxisxSize() {
        return timeAxisxSize;
    }

    /**
     * Returns the int ProcessAxisySize but if it is not specified
     * could be zero.
     */
    public int getProcessAxisySize() {
        return processAxisySize;
    }

    /**
     * Returns the int screen xSize but if it is not specified could be zero.
     */
    public Dimension getScreenSize() {
        return screenSize;
    }

    /**
     * Returns the File in which graphic should be printed.
     */
    public File getPrinterFile() {
        return printerFile;
    }

    /**
     * Returns a map containing DisplayProcess object mapped
     * with their id.
     */
    public Map getProcessDisplay() {
        return process;
    }

    /**
     * Returns a list containing DisplayMessages.
     */
    public List getMessagesDisplay() {
        return messages;
    }

    /**
     * Returns the object displayLabel which specified different
     * useful int.
     */
    public DisplayLabel getDisplayLabel() {
        return dLabel;
    }

    /**
     * Return a list of files to be parsed (neko traces).
     */
    public List getLogFiles() {
        return logFiles;
    }
}

