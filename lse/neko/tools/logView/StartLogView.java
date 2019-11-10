package lse.neko.tools.logView;

// java imports:
import java.io.File;
import java.util.List;

// javax imports:
import javax.swing.JOptionPane;


/**
 * This class contains the main method of the logView tool. If no
 * command line argument is specified, the GUI is loaded. If one is
 * given, it is supposed to be the xml input file.
 *
 * If the program is started with one valid argument and the xml
 * specify a printer file then logView will stay in command line mode
 * and simply create the postcript file. When those conditions are not
 * met, the GUI will be loaded as soon as input is necessary.
 *
 * @author Jennifer Muller
 */
public class StartLogView {

    private static boolean commandLineMode = false;

    /**
     * The main method of our project, it can accepts argument. Only
     * one which specify the xml file location/name. If no arguments,
     * it runs and starts the GUI.
     */
    public static void main(String[] args) {

        // process command line arguments

        // process options
        int i;
        for (i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--")) {
                i++;
                break;
            } else if (arg.equals("-h") || arg.equals("-h")) {
                System.out.print(HELP_MESSAGE);
                System.exit(0);
            } else if (arg.startsWith("-")) {
                usage();
            } else {
                break;
            }
        }

        // process the rest of the command line
        if (i > args.length || i < args.length - 1) {
            usage();
        }
        String configFileName = (i < args.length) ? args[i] : null;

        if (configFileName == null) {

            commandLineMode = false;
            MainFrame mainFrame = new MainFrame();

        } else {

            File configFile = new File(configFileName);
            if (!configFile.isFile()) {
                error("Cannot open config file " + configFileName);
            }
            commandLineMode = true;
            DataReader dataReader = new DataReader(configFile);
            // check for parsing errors
            if (!dataReader.readingOk()) {
                error("Syntax error in the config file!");
            }
            XmlInformation info = dataReader.getXmlData();
            File printerFile = info.getPrinterFile();

            if (printerFile == null) {
                commandLineMode = false;
                // No output postscript file is defined so
                // we will start a GUI to show result
                MainFrame lateLoadedFrame = new MainFrame();
                lateLoadedFrame.continueAfterXmlReading(dataReader, configFile);
            } else {
                commandLineMode = true;
                // An output file is given. So we wont
                // load a graphic interface and directly
                // compute the output file only and stay
                // in the command line mode.  Computing the
                // POSTSCRIPT HERE...
                System.out.println("The file " + printerFile.toString()
                                   + " is being created... Please wait");
                // Here is the code to output the
                // postcript file in command line mode
                List arrows = dataReader.getArrowsToDraw();
                List losts = dataReader.getLostMessages();
                GraphicPanel graphicPane =
                    new GraphicPanel(info, arrows, losts);
                graphicPane.print();
                System.out.println(printerFile.toString()
                                   + " is available. Good Bye !");
                System.exit(0);
            }
        }
    }

    public static void displayWarning(String shortMessage,
                                      String longMessage,
                                      boolean error)
    {
        if (commandLineMode) {
            System.out.println("Warning: " + shortMessage);
            System.out.println(longMessage);
            if (error) {
                System.exit(1);
            }
        } else {
            JOptionPane.showMessageDialog(null,
                                          longMessage,
                                          shortMessage,
                                          error
                                          ? JOptionPane.ERROR_MESSAGE
                                          : JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * This method is use to know the mode in which the program is
     * running. Either commmand line mode or graphical mode It is used
     * as static because every interaction should change if it is
     * command line or swing interaction. So we need to know exactly
     * which mode is wanted. If true it indicates commande line mode,
     * false -> normal mode, (graphical)
     */
    public static boolean getMode() {
        // if false the normal mode, if true command line mode (text mode)
        return commandLineMode;
    }

    private static final String HELP_MESSAGE =
        "LogView: Visualization of Neko message logs\n"
        + "Usage: java lse.neko.tools.logView.StartLogView"
        + " [options] [option files]\n"
        + "Options:\n"
        + "  - h, --help  Displays this help message.\n"
        + "Displays or prints Neko message logs.\n"
        + "Display options are specified by the option files.\n"
        + "No GUI appears if the option files only specify printing.\n";

    private static void usage() {
        System.err.print(HELP_MESSAGE);
        System.exit(2);
    }

    private static void error(String s) {
        System.err.println("Error: " + s);
        System.exit(1);
    }

}
