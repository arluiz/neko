package lse.neko.util.logging;

// java imports:
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.Util;


public class NekoFormatter
    extends Formatter
{

    private static final Logger logger =
        NekoLogger.getLogger(NekoFormatter.class.getName());

    protected static final int STRING_BUFFER_SIZE = 500;

    /**
     * Format the given log record and return the formatted string.
     * <p>
     * The resulting formatted String will normally include a
     * localized and formated version of the LogRecord's message field.
     * The Formatter.formatMessage convenience method can (optionally)
     * be used to localize and format the message field.
     *
     * @param record the log record to be formatted.
     * @return the formatted log record
     */
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        sb.append(formatMessage(record));
        sb.append("\n");
        printThrown(sb, record);
        return sb.toString();
    }

    public void printThrown(StringBuffer sb, LogRecord record) {
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
    }

    public String formatMessage(LogRecord record) {

        // Localize the message
        String format = record.getMessage();
        java.util.ResourceBundle catalog = record.getResourceBundle();
        if (catalog != null) {
            try {
                format = catalog.getString(record.getMessage());
            } catch (java.util.MissingResourceException ex) {
                // Drop through.  Use record message as format
                format = record.getMessage();
            }
        }

          // Format the message
        try {
            Object[] parameters = record.getParameters();
            if (parameters == null || parameters.length == 0) {
                // No parameters. Just return the format string.
                return format;
            } else if (format.indexOf("{0") >= 0) {
                for (int i = 0; i < parameters.length; i++) {
                    Object o = parameters[i];
                    if (o != null && o.getClass().isArray()) {
                        parameters[i] = Util.toString(o);
                    }
                }
                // Format according to a java.text style format.
                return MessageFormat.format(format, parameters);
            } else {
                StringBuffer sb = new StringBuffer(STRING_BUFFER_SIZE);
                // Append the format
                // and the string representation of all parameters.
                sb.append(format);
                for (int i = 0; i < parameters.length; i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }
                    sb.append(Util.toString(parameters[i]));
                }
                return sb.toString();
            }
        } catch (Exception ex) {
            // Formatting failed: use localized format string.
            // FIXME: uncomment
            // logger.warning("Formatting failed for the format\n  "
            //     + format + "\nin the message at time " + time);
            return format;
        }
    }
}




