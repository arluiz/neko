package lse.neko.abcast;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;


/**
 * A class which has an ABCast's interface but just does an ordinary
 * send to all destinations, without ordering and agreement
 * guarantees.
 */
public class FakeABCast
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface
{

    public FakeABCast() {
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    public void send(NekoMessage m) {
        sender.send(m);
    }

    public void deliver(NekoMessage m) {
        receiver.deliver(m);
    }
}
