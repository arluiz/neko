package lse.neko.tools.logView;

// java imports:
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

// other imports:
import net.socialchange.doctype.Doctype;
import net.socialchange.doctype.DoctypeChangerStream;
import net.socialchange.doctype.DoctypeGenerator;
import net.socialchange.doctype.DoctypeImpl;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This class creates an SAX validating parser for our xml input
 * file. The xml should specify a DTD in a tag <!Doctype...  This DTD
 * has already been made for all the information we need to deal with.
 * It is easily extensible.
 *
 * @author Jennifer Muller
 */
class XmlReader
    implements Runnable, ErrorHandler
{

    private File xmlDataFile;
    private XmlInformation xmlData = null;

    private DoctypeChangerStream changer;
    private XMLReader parser;
    private XmlHandler contentHandler = new XmlHandler();
    private int parsingErrorCounter = 0;

    private File xmlModified = null;

    /**
     * @param xmlDataFile The XML file to be parsed.
     */
    XmlReader(File xmlDataFile) {
        this.xmlDataFile = xmlDataFile;
    }

    public static final String DTD_PUBLIC_ID =
        "http://lsrwww.epfl.ch/neko/tools/logView/logView.dtd";
    private static final String DTD_RESOURCE = "logView.dtd";

    /**
     * Parses the XML file and validates it. If no parsing error
     * occurs, we retrieve all the information in the file from the
     * xml handler and make it available for others.
     * @see #getXmlData
     */
    public void run() {

        // first of all we must replace the doctype declaration to
        // validate against our DTD. Since this is still not a
        // standard feature of parsers, we have to
        // replace the doctype content ourself.
        // We use the DoctypeChanger package for this.
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(xmlDataFile);
            changer = new DoctypeChangerStream(fis);
            changer.setGenerator(new DoctypeGenerator() {
                public Doctype generate(Doctype old) {
                    return new DoctypeImpl(old.getRootElement(),
                                           DTD_PUBLIC_ID,
                                           DTD_PUBLIC_ID,
                                           null);
                }
            });

            // At this time we can assume that the doctype element
            // has been changed.
            // So we parse the modified file
            // create a SAX validating parser
            parser = XMLReaderFactory.createXMLReader();
            // set validation feature
            parser.setFeature("http://xml.org/sax/features/validation", true);
            // define content handler
            parser.setContentHandler(contentHandler);
            // define the error handler, which deals with parsing error
            parser.setErrorHandler(this);
            parser.setEntityResolver(new LocalEntityResolver(DTD_PUBLIC_ID,
                                                             DTD_RESOURCE));
        } catch (IOException e) {
            System.out.println("IO Exception occured "
                               + "while modifing the doctype element");
            System.err.println(e);
        } catch (SAXNotRecognizedException e) {
            System.out.println("Error not recognized");
            e.printStackTrace();
        } catch (SAXNotSupportedException e) {
            System.out.println("Error not supported exception");
            e.printStackTrace();
        } catch (SAXException e) {
            System.err.println("Error in setting up parser feature");
            e.printStackTrace();
        }


        try {
            // parse the file
            parser.parse(new InputSource(changer));
            // close the streams...
            changer.close();
            fis.close();
        } catch (SAXException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);

        // Here we look for parsing errors. If any happens
        // then we should not continue and warn the user.  but we
        // cannot warn him here because a thread does not display well
        // swing message dialog. (I do'nt know why but at this stage,
        // displaying a dialog make the program crash). So the user will
        // be warned just when this thread returns.
        } finally {
            if (parsingErrorCounter == 0) {
                xmlData = contentHandler.getXmlData();
            }
        }
    }


    /**
     * Warning event handler. Prints the error message to System.out.
     */
    public void warning(SAXParseException e)
        throws SAXException
    {
        handleError("warning", e);
    }

    /**
     * Error event handler. Prints the error message to System.out.
     */
    public void error(SAXParseException e)
        throws SAXException
    {
        handleError("error", e);
    }

    /**
     * Fatal error event handler. Prints error message to System.out.
     */
    public void fatalError(SAXParseException e)
        throws SAXException
    {
        handleError("FATAL error", e);
    }

    private void handleError(String kind, SAXParseException e) {
        parsingErrorCounter++;
        System.out.println("** XML parser " + kind + " **\n"
                           + "  Line:    " + e.getLineNumber() + "\n"
                           + "  URI:     " + e.getSystemId() + "\n"
                           + "  Message: " + e.getMessage());
    }

    /**
     * Get an XmlInformation object containing display properties
     * wanted by the user.
     */
    public XmlInformation getXmlData() {
        return xmlData;
    }

    /**
     * Get some error info on the parsing (default value has been used).
     */
    public boolean getDefaultValueUsed() {
        return contentHandler.getDefaultValueUsed();
    }

    /**
     * Get some error info on the parsing (incorrect value has been used).
     */
    public boolean getIncorrectValueUsed() {
        return contentHandler.getIncorrectValueUsed();
    }

    /**
     * This SAX EntityResolver allows us to validate against local
     * DTDs. Stolen from jakarta tomcat 3.2.2,
     * share/org/apache/jasper/compiler/JspUtil.java
     */
    public class LocalEntityResolver implements EntityResolver {

        String dtdId;
        String dtdResource;

        public LocalEntityResolver(String id, String resource) {
            this.dtdId = id;
            this.dtdResource = resource;
        }

        public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException
        {
            if (publicId != null && publicId.equals(dtdId)) {
                InputStream input =
                    XmlReader.this.getClass().getResourceAsStream(dtdResource);
                InputSource isrc =
                    new InputSource(input);
                return isrc;
            } else {
                return null;
            }
        }
    }

}
