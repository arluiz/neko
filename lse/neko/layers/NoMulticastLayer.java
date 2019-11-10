package lse.neko.layers;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.ProtocolImpl;
import lse.neko.SenderInterface;


/**
 * This layer transforms multicast messages into several unicast messages.
 * Use if the underlying network is not multicast capable,
 * or you want to disallow multicasts.
 */
public class NoMulticastLayer
    extends ProtocolImpl
    implements SenderInterface
{

    public NoMulticastLayer() {
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    public void send(NekoMessage m) {
        int[] dests = m.getDestinations();
        if (dests.length > 1) {

            for (int i = 0; i < dests.length; i++) {
                int[] dest = { dests[i] };
                NekoMessage m2 =
                    new NekoMessage(m.getSource(), dest, m.getProtocolId(),
                                    m.getContent(), m.getType());
                sender.send(m2);
            }

        } else {

            sender.send(m);

        }
    }

}
