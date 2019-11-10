package lse.neko.abcast;

// java imports:
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.util.GUID;


public class Skeen
    extends SkeenClient
{

    /*Logical clock of this process */
    private LamportClock clock = new LamportClock();

    public Skeen(NekoProcess process) {
        super(process);
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    private class Element
        implements Comparable
    {

        public Element(NekoMessage message,
                       int timestamp,
                       GUID id)
        {
            this.message = message;
            this.timestamp = timestamp;
            this.id = id;
        }

        private NekoMessage message;
        private int timestamp;
        private GUID id;
        private boolean stamped = false;

        public NekoMessage getMessage() {
            return message;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public GUID getId() {
            return id;
        }

        public boolean getStamped() {
            return stamped;
        }

        public void stamp(int newTimestamp) {
            stamped = true;
            this.timestamp = newTimestamp;
        }

        public int compareTo(Object right) {
            Element r = (Element) right;
            if (timestamp != r.timestamp) {
                return (timestamp < r.timestamp) ? -1 : +1;
            } else if (stamped != r.stamped) {
                // stamped elements come last
                return (r.stamped) ? -1 : +1;
            } else {
                // two different elements should never compare equal
                return id.compareTo(r.id);
                // we use GUIDs rather than hash codes,
                // in order to make simulations reproducible
            }
        }

    }

    private Map idToElement = new HashMap(); // GUIDs to Elements
    private SortedSet received = new TreeSet(); // of Elements

    public synchronized void deliver(NekoMessage m) {

        switch (m.getType()) {

        case START_SKEEN:

            ContentSKEEN content = (ContentSKEEN) m.getContent();
            GUID id = content.getId();

            Element element =
                new Element(new NekoMessage(m.getSource(),
                                            m.getDestinations(),
                                            content.getProtocolId(),
                                            content.getContent(),
                                            content.getType()),
                            clock.getValue(),
                            id);

            idToElement.put(content.getId(), element);
            received.add(element);

            // Response from the receiver to the sender of the original Message
            // for calculation of the sequence number
            int[] destination = { m.getSource() };
            NekoMessage m1 =
                new NekoMessage(destination,
                                getId(),
                                new ContentID(content.getId(),
                                              m.getDestinations(),
                                              clock.getValue()),
                                RESPONSE_TO_SENDER);
            sender.send(m1);

            clock.update();

            break;

        case SEND_SEQ_NUMBER:

            /*Content from the ID Structure*/
            ContentDeliver contentID = (ContentDeliver) m.getContent();

            int timestamp = contentID.getTimestamp();
            clock.update(timestamp);

            Element element2 =
                (Element) idToElement.get(contentID.getId());
            received.remove(element2);
            element2.stamp(timestamp);
            received.add(element2);

            Iterator it = received.iterator();
            while (it.hasNext()) {
                Element e = (Element) it.next();
                if (!e.getStamped()) {
                    break;
                }
                it.remove();
                receiver.deliver(e.getMessage());
            }

            break;

        default:
            super.deliver(m);
        }
    }

}
