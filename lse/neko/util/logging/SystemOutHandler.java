package lse.neko.util.logging;

// java imports:
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;


/**
 * This <tt>Handler</tt> publishes log records to <tt>System.out</tt>.
 * It is very similar to <tt>ConsoleHandler</tt> (that publishes
 * log records on <tt>System.err</tt>).
 * By default the <tt>SimpleFormatter</tt> is used to generate brief summaries.
 * <p>
 * <b>Configuration:</b>
 * By default each <tt>SystemOutHandler</tt> is initialized using the following
 * <tt>LogManager</tt> configuration properties.  If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * <ul>
 * <li>   lse.neko.util.logging.SystemOutHandler.level
 *          specifies the default level for the <tt>Handler</tt>
 *          (defaults to <tt>Level.INFO</tt>).
 * <li>   lse.neko.util.logging.SystemOutHandler.filter
 *          specifies the name of a <tt>Filter</tt> class to use
 *          (defaults to no <tt>Filter</tt>).
 * <li>   lse.neko.util.logging.SystemOutHandler.formatter
 *           specifies the name of a <tt>Formatter</tt> class to use
 *            (defaults to <tt>java.util.logging.SimpleFormatter</tt>).
 * <li>   lse.neko.util.logging.SystemOutHandler.encoding
 *          the name of the character set encoding to use (defaults to
 *          the default platform encoding).
 * </ul>
 */
public class SystemOutHandler extends StreamHandler {

    // XXX: This class is so complicated only because of the bad design of
    // java.util.logging.

    // Private method to configure a ConsoleHandler from LogManager
    // properties and/or default values as specified in the class
    // javadoc.
    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = SystemOutHandler.class.getName();

        setLevel(getLevelProperty(manager, cname + ".level", Level.INFO));
        setFilter(getFilterProperty(manager, cname + ".filter", null));
        setFormatter(getFormatterProperty(manager, cname + ".formatter",
                                          new NekoFormatter()));
        try {
            setEncoding(getStringProperty(manager, cname + ".encoding", null));
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {
                // doing a setEncoding with null should always work.
                // assert false;
            }
        }
    }

    // If the property is not defined we return the given
    // default value.
    private static String getStringProperty(LogManager manager,
                                            String name, String defaultValue)
    {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return val.trim();
    }

    // If the property is not defined or cannot be parsed
    // we return the given default value.
    private static Level getLevelProperty(LogManager manager,
                                          String name, Level defaultValue)
    {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Level.parse(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    // We return an instance of the class named by the "name"
    // property. If the property is not defined or has problems
    // we return the defaultValue.
    private static Filter getFilterProperty(LogManager manager,
                                            String name, Filter defaultValue)
    {
        String val = manager.getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Filter) clz.newInstance();
            }
        } catch (Exception ex) {
            // We got one of a variety of exceptions in creating the
            // class or creating an instance.
            // Drop through.
        }
        // We got an exception.  Return the defaultValue.
        return defaultValue;
    }


    // Package private method to get a formatter property.
    // We return an instance of the class named by the "name"
    // property. If the property is not defined or has problems
    // we return the defaultValue.
    private static Formatter getFormatterProperty(LogManager manager,
                                                  String name,
                                                  Formatter defaultValue)
    {
        String val = manager.getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Formatter) clz.newInstance();
            }
        } catch (Exception ex) {
            // We got one of a variety of exceptions in creating the
            // class or creating an instance.
            // Drop through.
        }
        // We got an exception.  Return the defaultValue.
        return defaultValue;
    }

    /**
     * Create a <tt>SystemOutHandler</tt> for <tt>System.out</tt>.
     * <p>
     * The <tt>SystemOutHandler</tt> is configured based on
     * <tt>LogManager</tt> properties (or their default values).
     *
     */
    public SystemOutHandler() {
        // FIXME: lines with sealed are commented out
        //sealed = false;
        configure();
        setOutputStream(System.out);
        //sealed = true;
    }

    /**
     * Publish a <tt>LogRecord</tt>.
     * <p>
     * The logging request was made initially to a <tt>Logger</tt> object,
     * which initialized the <tt>LogRecord</tt> and forwarded it here.
     * <p>
     * @param  record  description of the log event
     */
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    /**
     * Override <tt>StreamHandler.close</tt> to do a flush but not
     * to close the output stream.  That is, we do <b>not</b>
     * close <tt>System.out</tt>.
     */
    public void close() {
        flush();
    }
}

