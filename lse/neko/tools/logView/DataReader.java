package lse.neko.tools.logView;

// java imports:
import java.io.File;
import java.util.List;


/**
 * This class treats all the reading from files (either XML or Neko
 * log files). It runs different threads.  It is in some way the
 * interface between the graphical part and the reading part of our
 * program.
 *
 *@author Jennifer Muller
 */
class DataReader {

    /**
     * boolean mode is a copy of the static variable of our
     * StartLogview class. It indicates the graphical or "console"
     * mode
     */
    private File xmlFile;
    private NekoReader neko;

    // information to be accessed with get methods
    private XmlInformation xmlData = null;
    private List arrowsList = null;
    private List lostMessages = null;
    private boolean goAhead = false;
    private List logFile = null;

    /**
     * DataReader is created with only one file: the XML file. It then
     * starts a thread to read it, and then starts threads to read the
     * log files specified in the XML file.
     */
    DataReader(File xmlFile) {
        this.xmlFile = xmlFile;

        XmlReader xmlReader = new XmlReader(xmlFile);
        xmlReader.run();
        xmlData = xmlReader.getXmlData();

        // Warn user if some values in the XML were not those expected
        if (xmlReader.getDefaultValueUsed()
            || xmlReader.getIncorrectValueUsed())
        {
            boolean one = xmlReader.getDefaultValueUsed();
            boolean two = xmlReader.getIncorrectValueUsed();
            warnUser(one, two);
        }

        /* If a parsing error occured during reading, then
         * logFile and displayInfo are null, and no further
             * reading will be performed because we do not deal with
             * corrupted XML file (as XML 1.0 recommends).
             */
        if (xmlData != null) {
            startNekoReader(xmlData);
            goAhead = true;
        } else {
            String message = "Your display configuration file is not valid.\n"
                + "Please correct it according to the DTD of LogView";
            StartLogView.displayWarning("XML Parsing Error",
                                        message, true);
        }
    }

    /**
     * This method checks the log files, given as a parameter
     * in the XmlInformation object, and reads them if everything is OK.
     * Uses a separate thread for reading.
     */
    private void startNekoReader(XmlInformation anXmlData) {
        logFile = anXmlData.getLogFiles();
        // just check the files to avoid any stupid IO Exception :)
        for (int i = 0; i < logFile.size(); i++) {
            File toCheck = (File) logFile.get(i);

            if (!toCheck.isFile()) {
                String message = "Your Data File " + toCheck
                    + " seems not to be a file.\n"
                    + "Please check the path int the configuration input File";
                StartLogView.displayWarning("Neko File not found",
                                            message, false);
                logFile.remove(toCheck);
            }
        }

        // The files that are accessible are bigger or equal to
        // one. If not, of course we cannot read anything.
        if (logFile.size() >= 1) {
            // create the nekoReader object that will read the log files
            neko = new NekoReader(anXmlData);
            neko.run();
            // retrieve the information from nekoReader
            arrowsList = neko.getListOfArrows();
            lostMessages = neko.getLostMessages();
        } else {
            // no data file is accessible, warn user.
            // Either by swing message or console.
            String message = "LogView has not any Data File to read, "
                + "add at least one to your configuration File";
            StartLogView.displayWarning("No Log File", message, true);
        }
    }

    /**
     * This method warns the user because some replacement data has
     * been used or incorrect data was not treated in the XML but the
     * program will continue to run normally. It will display the
     * result with the information it managed to extract from the
     * XML.
     */
    private void warnUser(boolean hasDefault, boolean incorrect) {
        // Here again, for each message we want to exchange with the
        // user, we check if we must user text (command Line) mode or
        // graphical mode

        if (hasDefault && !incorrect) {
            // Default values have been used
            String message = "Some value were not correct "
                + "in your configuration file, default values has been used.\n"
                + "The parser was waiting for integer "
                + "and you gave something else.";
            StartLogView.displayWarning("Default Value Used",
                                        message,
                                        false);
        } else {
            if (!hasDefault && incorrect) {
                // Incorrect values were given,
                String message = "Some value were not correct "
                    + "in your configuration file, please correct them\n"
                    + "The parser were waiting for integer "
                    + "and you gave something else. "
                    + "Information has not been treated";
                StartLogView.displayWarning("Incorrect Value Given",
                                            message, false);
            } else {
                if (hasDefault && incorrect) {
                    // default values have been used and incorrect
                    // values were given.
                    String message = "Some value were not correct "
                        + "in your configuration file, please correct them\n"
                        + "The parser were waiting for integer "
                        + "and you gave something else. "
                        + "Information has not been treated\n"
                        + "Default value has also been used!";
                    StartLogView.displayWarning("Incorrect value given "
                                                + "& default Values used",
                                                message, false);
                }
            }
        }
    }

    /**
     * Method which returns all XML information in the format of
     * Object XmlInformation.
     */
    public XmlInformation getXmlData() {
        return xmlData;
    }

    /**
     * Method which returns an arrayList containing Arrows. They are
     * ready to be printed.
     */
    public List getArrowsToDraw() {
        return arrowsList;
    }

    /**
     * Method which returns an arrayList of broken arrow, they are
     * stored as LostMessage.
     */
    public List getLostMessages() {
        return lostMessages;
    }

    /**
     * Method which returns the state of the reading. I means, if an
     * error has happened during the parsing of the XML file then we
     * must know it. And also other classes must be able to know
     * it. So we put here a boolean method to give this information to
     * other classes. (like MainFrame that is primary concern...)
     */
    public boolean readingOk() {
        return goAhead;
    }

}
