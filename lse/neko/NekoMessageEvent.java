package lse.neko;

// lse.neko imports:
import lse.neko.util.Util;


/**
 * Stores a message with an event (e.g., sending).
 * It is used by the logging framework to store and
 * format the message event.
 * The format is the following: (outdated documentation)
 * <ul><li>Lexical definition<ul>
 * <li>ASCII format, with only printable characters, tabs and newlines.</li>
 * <li>Each entry is one line (if the message content does not contain
 *    newlines).</li>
 * <li>Whitespace separates fields</li>
 * <li>Comment lines start with #, possibly prepended with whitespace.
 *     Empty lines (containing whitespace) are possible.</li>
 * <li>Entries containing spaces / non-printable characters should be
 *    possible. Escaping mechanism: use quotes (").
 *    Within the quotes, space and backslash
 *    sequences are allowed: \\, \", \n, \t, \r, etc. with the usual meanings.
 *    (not implemented)</li>
 * </ul></li>
 * <li>Syntax<ul>
 * <li>The first field is the time of occurence.
 *     This time is given in milliseconds (and can be any floating point
 *        value).</li>
 * <li>The second field is the entry type.
 *   Parsers should ignore unknown entry types. The entry types follow.</li>
 * <li>process id name
 *     <br>Communicating process
 *     <br>Example: process p2 server_1</li>
 * <li>e logging_entity event_type message<br>
 *     logging_entity is most often a process, e.g., p1
 *     Event of type event_type that occurs on message.
 *     Predefined event types are
 *     <ul>
 *       <li>s sending</li>
 *       <li>ns message appears on the network (makes sense for some
 *         simulated networks)</li>
 *       <li>nr message disappears from the network</li>
 *       <li>r receiving</li>
 *     </ul></li>
 * <li>message has the following format:<br>
 *     source destinations type content
 *     <br>
 *         source is a process id.
 *     <br>
 *         destinations is a comma separated list of process ids.
 *     <br>
 *         type is the type of the message for the simulation.
 *     <br>
 *         content can be anything, even 0 or several fields.
 *         It's a string to be displayed. If the content is the
 *         null reference, "null" is printed.
 *     <br>
 *         Examples for events:
 *     <br>
 *        e p0 s p0 p1,p2,p3,p4 INITIAL_BROADCAST value=15
 *     <br>
 *        e p0 r p3 p0 REPLY null
 * </ul></li>
 * </ul>
 */
public class NekoMessageEvent {

    private final String eventName;
    private final NekoMessage message;

    public NekoMessageEvent(String eventName, NekoMessage message) {
        this.eventName = eventName;
        this.message = message;
    }

/*
    public String getEventName() {
        return eventName;
    }

    public NekoMessage getMessage() {
        return message;
    }
*/

    private static final int STRING_BUFFER_SIZE = 500;

    public String toString() {
        StringBuffer sb = new StringBuffer(STRING_BUFFER_SIZE);

        sb.append("e ");
        sb.append(eventName);
        sb.append(" p");
        sb.append(Integer.toString(message.getSource()));

        final int[] dest = message.getDestinations();
        for (int i = 0; i < dest.length; i++) {
            if (i > 0) {
                sb.append(",p");
            } else {
                sb.append(" p");
            }
            sb.append(Integer.toString(dest[i]));
        }

        // FIXME: put these lines back
        // once the reference log files in the unit tests are updated
        //sb.append(" ");
        //sb.append(message.getProtocolId());

        sb.append(" ");
        sb.append(MessageTypes.instance().getName(message.getType()));
        sb.append(" ");
        sb.append(Util.toString(message.getContent()));

        return sb.toString();
    }

}
