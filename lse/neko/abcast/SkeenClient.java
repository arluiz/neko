package lse.neko.abcast;

// java imports:
import java.util.HashMap;
import java.util.Map;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.GUID;


public class SkeenClient
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface
{

    protected static final int START_SKEEN = 30;
    protected static final int RESPONSE_TO_SENDER = 31;
    protected static final int SEND_SEQ_NUMBER = 32;

    static {
        MessageTypes.instance().register(START_SKEEN, "START_SKEEN");
        MessageTypes.instance().register(RESPONSE_TO_SENDER,
                                         "RESPONSE_TO_SENDER");
        MessageTypes.instance().register(SEND_SEQ_NUMBER, "SEND_SEQ_NUMBER");
    }

    private NekoProcess process;

    public SkeenClient(NekoProcess process) {
        this.process = process;
    }

    protected SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private class Timestamps {

        public Timestamps(int expected) {
            this.expected = expected;
            max = Integer.MIN_VALUE;
        }

        private int expected;
        private int max;

        public void update(int timestamp) {
            if (max < timestamp) {
                max = timestamp;
            }
            expected--;
        }

        public boolean ready() {
            return expected == 0;
        }

        public int getMax() {
            return max;
        }

    }

    private Map idToTimestamps = new HashMap();

    public synchronized void deliver(NekoMessage m) {

        switch (m.getType()) {

        case RESPONSE_TO_SENDER:

            ContentID contentID = (ContentID) m.getContent();

            GUID id = contentID.getId();

            Timestamps timestamps = (Timestamps) idToTimestamps.get(id);
            if (timestamps == null) {
                // new message ID
                timestamps = new Timestamps(contentID.getDest().length);
                idToTimestamps.put(id, timestamps);
            }

            timestamps.update(contentID.getTimestamp());

            if (timestamps.ready()) {
                int max = timestamps.getMax();
                NekoMessage m2 =
                    new NekoMessage(contentID.getDest(),
                                    getId(),
                                    new ContentDeliver(id,
                                                       max),
                                    SEND_SEQ_NUMBER);

                sender.send(m2);
            }

            break;

        default:
            throw new UnexpectedMessageException(m);
        }

    }

    public void send(NekoMessage m) {

        NekoMessage m1 =
            new NekoMessage(m.getSource(),
                            m.getDestinations(),
                            getId(),
                            new ContentSKEEN(m.getProtocolId(),
                                             m.getContent(),
                                             m.getType(),
                                             new GUID(process)),
                            START_SKEEN);
        sender.send(m1);

    }

}
