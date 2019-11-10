package lse.neko.layers;

// java imports:
import java.util.Iterator;

// lse.neko imports:
import lse.neko.NekoMessage;


interface SequencedMessageInterface {

    /**
     * Returns the type of handled messages.
     */
    int getType();

    /**
     * Returns sequence number of the message.
     */
    int getSeqNum(NekoMessage m);

    /**
     * Returns session identifier of the message.
     */
    int getSessionId(NekoMessage m);

    Iterator iterator(NekoMessage m);
}
