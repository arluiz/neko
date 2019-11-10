package lse.neko.comm;

// java imports:
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.util.logging.NekoLogger;


public class JavaSerializer implements Serializer {

    private class NoHeaderObjectOutputStream
        extends ObjectOutputStream
    {
        /**
         * Inherited.
         */
        public NoHeaderObjectOutputStream(OutputStream os)
            throws IOException
        {
            super(os);
        }

        /**
         * Does nothing. Writing and reading headers is annoying,
         * for it makes the stream stateful -- and this is what the Serializer
         * class wants to avoid.
         */
        protected void writeStreamHeader()
            throws IOException
        {
        }
    }

    JavaSerializer(OutputStream osUnderlying)
        throws IOException
    {
        // This solution is probably inefficient:
        // both ObjectOutputStream and BufferedOutputStream do buffering.
        // But if no BufferedOutputStream is used, NekoMessages get
        // written in two pieces => horrible performance,
        // if the messages go through TCP with Nagle's algorithm disabled.
        //
        // A good solution is to push Sun to remove buffering from
        // ObjectOutputStream. This would also speed up marshalling for UDP.
        //
        os = new NoHeaderObjectOutputStream
            (new BufferedOutputStream(osUnderlying));
    }

    private int resetCounter = 0;
    /**
     * The stream is only reset every resetCounterMax-th time.
     * You might get problems if you set it to more than 1,
     * but performance improves greatly.
     */
    private int resetCounterMax = 1;

    public void setResetCounterMax(int resetCounterMax) {
        this.resetCounterMax = resetCounterMax;
    }

    public void writeMessage(NekoMessage m)
        throws IOException
    {
        logger.log(Level.FINE, "writing {0}", m);
        if (++resetCounter >= resetCounterMax) {
            resetCounter = 0;
            // It is very important that the reset()
            // be done before the writeObject().
            // Otherwise (when passing messages
            // rather than using a stream of messages)
            // the RESET token is never read
            // by the Deserializer on the other side!
            os.reset();
        }

        // Custom serialization for NekoMessage
        os.writeInt(m.getSource());
        int[] to = m.getDestinations();
        os.writeInt(to.length);
        for (int i = 0; i < to.length; i++) {
            os.writeInt(to[i]);
        }
        os.writeInt(m.getType());
        os.writeObject(m.getProtocolId());
        os.writeObject(m.getContent());

        os.flush(); // propagates to the underlying stream
        // flush() is an indication that the message can be sent
        logger.log(Level.FINE, "wrote {0}", m);
    }

    /**
     * Not ObjectOutput, for we need to access reset.
     */
    private ObjectOutputStream os;

    private static final Logger logger =
        NekoLogger.getLogger(JavaSerializer.class.getName());
}




































































































