package lse.neko.tools.logView;

// java imports:
import java.awt.Component;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List; // ambiguous with: java.awt.List

// javax imports:
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;


/**
 * This class describe the main panel of our main frame. It is in
 * fact an implementation of a JTabbedPane. It can remove tab or set
 * tab, like the xml source tab, the log files tab and the most
 * important: graphic tab
 *
 * @author Jennifer Muller
 */
public class TabbedPane
    extends JPanel
{

    JTabbedPane tabbedPane;

    /**
     * Constructor with no argument create a simple JTabbedPane with
     * only on tab and a Label in the middle saying "To display
     * blalabllba".
     */
    public TabbedPane() {

        tabbedPane = new JTabbedPane();
        Component panelInfo =
            makeTextPanel("To display something you should first open "
                          + "a configuration file");
        tabbedPane.addTab("Welcome", panelInfo);
        tabbedPane.setSelectedIndex(0);

        setLayout(new GridLayout(1, 1));
        add(tabbedPane);
    }

    /**
     * remove all tab ans associated component in the tabbedpane.
     */
    public void removeAllPanes() {
        tabbedPane.removeAll();
    }

    /**
     * set the xml source pane, it read the source file and display
     * the content in a jtextarea. And finally add it to a tab. The
     * panes is srollable, since it can be longer than the frame
     */
    public void setXmlPane(File source) {
        JPanel xmlPane = new JPanel(false);
        xmlPane.setLayout(new GridLayout(1, 1));
        JScrollPane toAdd = new JScrollPane(xmlPane);
        JTextArea xmlSource = new JTextArea();
        xmlSource.setEditable(false);
        // read the file and append each line to the textarea
        BufferedReader xmlStream = null;
        boolean loop = true;
        try {
            xmlStream = new BufferedReader(new FileReader(source));
        } catch (Exception e) {
            System.out.println("in tabbed pane, "
                               + "cannot open xml to display source");
        }
        while (loop) {
            try {
                String aLine = xmlStream.readLine();
                if (aLine != null) {
                    xmlSource.append(aLine + "\n");
                } else {
                    loop = false;
                    xmlStream.close();
                }
            } catch (EOFException k) {
                loop = false;
            } catch (IOException k) {
                loop = false;
            }
        }
        // adding instructions
        xmlPane.add(xmlSource);
        tabbedPane.addTab("Configuration file", toAdd);
    }

    /**
     * set the graphic tab using a Graphic Panel. It add it as a tab
     */
    public void setGraphicPane(JPanel gPanel) {
        // Since everything has been done in another class, this
        // method only add the method only add the panel in parameter
        // to a new tab
        Component toAdd = (Component) gPanel;
        tabbedPane.addTab("Time-Line Diagram", toAdd);
    }

    /**
     * Sets a new tab containing a JTextArea describing which file
     * have been treated, the panel is scrollable since it can be
     * longer than the frame.
     */
    public void setLogPane(List logFiles) {
        JPanel logPane = new JPanel(false);
        logPane.setLayout(new GridLayout(1, 1));

        int numberOfFile = logFiles.size();
        JTextArea logText = new JTextArea();
        logText.setEditable(false);
        logText.append("The following files of Neko Data have been treated: "
                       + "\n" + "\n");
        for (int i = 0; i < numberOfFile; i++) {
            logText.append(logFiles.get(i).toString() + "\n");
        }
        logPane.add(logText);
        JScrollPane toAdd = new JScrollPane(logPane);
        tabbedPane.addTab("Traces files", toAdd);
    }

    /**
     * This is use only one time, to create the initial tabbedPane.
     */
    protected Component makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }
}
