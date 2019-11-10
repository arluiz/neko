package lse.neko.util.logging;

// java imports:
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

// other imports:
import org.apache.java.util.Configurations;


public class NekoLogManagerInitializer {

    private static boolean initialized = false;

    private static final String DEFAULT_CONFIG =
        "handlers = java.util.logging.ConsoleHandler\n"
        + ".level = INFO\n"
        + "java.util.logging.ConsoleHandler.formatter = "
        + "lse.neko.util.logging.NekoFormatter\n"
        + "java.util.logging.FileHandler.formatter = "
        + "lse.neko.util.logging.NekoLongFormatter\n"
        + "java.util.logging.MemoryHandler.formatter = "
        + "lse.neko.util.logging.NekoLongFormatter\n"
        + "java.util.logging.SocketHandler.formatter = "
        + "lse.neko.util.logging.NekoLongFormatter\n"
        + "java.util.logging.StreamHandler.formatter = "
        + "lse.neko.util.logging.NekoLongFormatter\n";

    public static synchronized void init(Configurations config) {

        if (config == null) {
            throw new IllegalArgumentException();
        }
        if (initialized) {
            throw new IllegalStateException("Only call init once!");
        }

        Map repository = config.getRepository();
        Set entries = repository.entrySet();
        Iterator it = entries.iterator();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Writer writer;
        try {
            writer = new OutputStreamWriter(stream, "8859_1");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected exception " + ex
                                       + ", the encoding "
                                       + "8859_1 must be known!");
        }

        try {

            // The directives in the default configuration can be
            // overridden with directives in the Neko config file.
            writer.write(DEFAULT_CONFIG);

            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                Object v = entry.getValue();
                if (!(v instanceof Collection)) {
                    writer.write(key + " " + v + "\n");
                } else {
                    // list of values
                    StringBuffer sb = new StringBuffer(key);
                    Collection c = (Collection) v;
                    Iterator it2 = c.iterator();
                    while (it2.hasNext()) {
                        sb.append(" ");
                        sb.append(it2.next().toString());
                    }
                    sb.append("\n");
                    writer.write(sb.toString());
                }
            }

            writer.close();

        } catch (IOException ex) {
            throw new RuntimeException("Unexpected exception " + ex);
        }

        //InputStream ins2 = new ByteArrayInputStream(stream.toByteArray());
        //Properties prop = new Properties();
        //try {
        //    prop.load(ins2);
        //} catch (IOException ex) {
        //    logger.fine(""+ex);
        //}
        //logger.fine("prop "+prop);

        InputStream ins = new ByteArrayInputStream(stream.toByteArray());

        try {
            LogManager.getLogManager().readConfiguration(ins);
        } catch (IOException ex) {
            throw new RuntimeException("Unexpected IOException: " + ex);
        }

        initialized = true;
    }

    // A hack to have some static initializers executed
    // XXX: remove when found a solution for initializing
    // names for message types.
    static {
        new lse.neko.MessageTypeConst();
        new lse.neko.EventTypeConst();
    }

}

