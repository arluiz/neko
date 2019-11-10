package lse.neko.tools.logView;

// java imports:
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List; // ambiguous with: java.awt.List

// javax imports:
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;


/**
 * This class implements the GUI of our program. It contains a
 * MenuBar and a tabbedPane as contentpane. We have three tabs, two
 * with information about the xml file and the files that have been
 * opened. And a third one with the time line graphic.
 * @author Jennifer Muller
 */
class MainFrame
    extends JFrame
    implements ActionListener
{

    private JMenuItem openXml, print, exit;
    private JMenu fileMenu, quitMenu;
    private JMenuBar menuBar;
    private JPanel firstView;
    private JLabel filler;
    private TabbedPane principalPane;
    private JFrame frame;
    private GraphicPanel graphicPane;

    /**
     * Constructor of main frame come with no argument. It loads a
     * default window with a menu bar and a tabbedPane. Only one tab
     * is present at this time and it tells the user to open an xml
     * file.  Default size of the window is a quarter of the
     * screen. It is then changed in the xml.
     */
    MainFrame() {
        // Setting up the intial Frame, size, title etc.. (menubar)
        frame = new JFrame("Visualisation of Neko Simulations Traces "
                           + "with LogView");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        createJMenuBar(frame);
        principalPane = new TabbedPane();
        frame.getContentPane().add(principalPane, BorderLayout.CENTER);
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        frame.setSize(d.width / 2, d.height / 2);
        frame.setVisible(true);
    }

    /**
     * implementation of actionperformed from interface
     * actionlistener. It actually manages two menu item. Quit and
     * open xml. MenuItem openxml starts several method, in fact it
     * really starts all treatment in our programm
     */
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        if (o == exit) {
            System.exit(0);
        }

        if (o == openXml) {
            File xml = openXmlFile();
            if (xml != null) {
                DataReader dataReader = new DataReader(xml);
                if (dataReader.readingOk()) {
                    continueAfterXmlReading(dataReader, xml);
                }
            }
        }
        if (o == print) {
            graphicPane.print();
        }
    }

    /**
     * This method load the content of the frame after having read
     * the XML file, it loads the tabs that shows the graphic, the xml
     * and the logfile that have been opened.
     */
    public void continueAfterXmlReading(DataReader dataReader, File xml) {
        // remove all tabs to create three new tabs
        XmlInformation xmlData = dataReader.getXmlData();
        principalPane.removeAllPanes();
        principalPane.setGraphicPane(createGraphicPanel(dataReader));
        principalPane.setXmlPane(xml);
        principalPane.setLogPane(xmlData.getLogFiles());
        // check to enable the generate postscript button, if no file
        // is specified in the config file then the button will stay
        // disabled...
        if (xmlData.getPrinterFile() != null) {
            print.setEnabled(true);
        }
        // resize the frame to the xml wanted dimension
        frame.setSize(xmlData.getScreenSize());
        frame.validate();
    }

    /**
     * Creates a graphicPane by creating an instance of GraphicPanel
     * and then send it to the tabbed pane to be added.
     */
    private JPanel createGraphicPanel(DataReader dataReader) {
        XmlInformation xmlData = dataReader.getXmlData();
        List arrows = dataReader.getArrowsToDraw();
        List lostMessages = dataReader.getLostMessages();
        graphicPane = new GraphicPanel(xmlData, arrows, lostMessages);
        return (JPanel) graphicPane;
    }

    /**
     * Creates a JMenuBar and adds it to the frame.
     */
    private void createJMenuBar(JFrame superFrame) {
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        quitMenu = new JMenu("Quit");

        openXml = new JMenuItem("Open Configuration File");
        openXml.addActionListener(this);
        openXml.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                                                      ActionEvent.CTRL_MASK));
        print = new JMenuItem("Generate PostScript");
        print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                                                    ActionEvent.CTRL_MASK));
        print.setEnabled(false);
        print.addActionListener(this);
        exit = new JMenuItem("Exit");
        exit.addActionListener(this);
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,
                                                   ActionEvent.ALT_MASK));

        fileMenu.add(openXml);
        fileMenu.add(print);
        quitMenu.add(exit);
        menuBar.add(fileMenu);
        menuBar.add(quitMenu);
        superFrame.setJMenuBar(menuBar);
    }

    /**
     * Open a JFile chooser to select an input file. In XML format.
     */
    private File openXmlFile() {
        File xmlFile = null;
        JFileChooser chooser = new JFileChooser();

        // Filter the file to view only XML files
        chooser.setFileFilter(new XMLFilter());
        int option = chooser.showOpenDialog(this);
        // get the selected file and return it (or null)
        if (option == JFileChooser.APPROVE_OPTION) {
            xmlFile = chooser.getSelectedFile();
        }
        return xmlFile;
    }

    /**
     * Implementation of a filter that select only XML files. It
     * applies to JFileChooser.
     */
    class XMLFilter extends javax.swing.filechooser.FileFilter {
        private String getExtension(File f) {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 &&  i < s.length() - 1) {
                ext = s.substring(i + 1).toLowerCase();
            }
            return ext;
        }

        public boolean accept(File f) {
            String extension = getExtension(f);
            if (f.isDirectory()) {
                return true;
            }
            if (extension != null) {
                return extension.equals("xml");
            }
            return false;
        }

        // The description of this filter
        public String getDescription() {
            return "XML Files";
        }
    }
}
