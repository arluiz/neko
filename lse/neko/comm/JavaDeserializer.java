package lse.neko.comm;

// java imports:
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.util.logging.NekoLogger;


public class JavaDeserializer implements Deserializer {

    private class NoHeaderObjectInputStream
        extends ObjectInputStream
    {
        /**
         * Inherited.
         */
        public NoHeaderObjectInputStream(InputStream is)
            throws IOException
        {
            super(is);
        }

        /**
         * Does nothing. Writing and reading headers is annoying,
         * for it makes the stream stateful -- and this is what the Serializer
         * class wants to avoid.
         */
        protected void readStreamHeader()
            throws IOException, StreamCorruptedException
        {
        }
    }

    JavaDeserializer(InputStream isUnderlying)
        throws IOException
    {
        is = new NoHeaderObjectInputStream(isUnderlying);
    }

    public NekoMessage readMessage()
        throws IOException, ClassNotFoundException
    {
        logger.fine("reading");

        // Custom deserialization for NekoMessage
        int from = is.readInt();
        int toLength = is.readInt();
        int[] to = new int[toLength];
        for (int i = 0; i < toLength; i++) {
            to[i] = is.readInt();
        }
        int type = is.readInt();
        Object protocolId = is.readObject();
        Object content = is.readObject();
        NekoMessage m = new NekoMessage(from, to, protocolId,
                                        content, type);

        logger.log(Level.FINE, "read {0}", m);
        return m;
    }

    private ObjectInput is;

    private static final Logger logger =
        NekoLogger.getLogger(JavaDeserializer.class.getName());
}










