package lse.neko.util.logging;

// java imports:
import java.text.MessageFormat;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public class NekoLongFormatter
    extends NekoFormatter
{

    private static final Logger logger =
        NekoLogger.getLogger(NekoLongFormatter.class.getName());

    private static final String UNKNOWN = "???";
    private static final String FORMAT = "{0,number,0.000} {1} {2} {3}\n";

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

        // type check
        NekoLogRecord r2;
        if (record instanceof NekoLogRecord) {
            r2 = (NekoLogRecord) record;
        } else {
            // FIXME: uncomment
            // logger.warning("The log record with message\n  "
            //                + record.getMessage()
            //                + "\nis not a NekoLogRecord.");
            r2 = null;
        }

        // Get the time
        double time =
            (r2 != null)
            ? r2.getTime()
            : new Long(record.getMillis()).doubleValue();

        // Get the process
        int processId = (r2 != null) ? r2.getProcessId() : Integer.MIN_VALUE;
        String process =
            (processId != Integer.MIN_VALUE) ? "p" + processId : UNKNOWN;

        // Get the source logger
        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = UNKNOWN;
        }

        // Format the message
        String message = formatMessage(record);

        // Format the whole log entry
        MessageFormat format = new MessageFormat(FORMAT);
        StringBuffer sb = new StringBuffer(STRING_BUFFER_SIZE);
        format.format(
                      new Object[] {
                          new Double(time),
                          process,
                          loggerName,
                          message
                      }, sb, null);
        printThrown(sb, record);
        return sb.toString();
    }

}
