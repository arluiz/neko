package lse.neko.abcast;

// lse.neko imports:
import lse.neko.MessageTypeConst;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.SenderInterface;
import lse.neko.util.ContentRB;
import lse.neko.util.GUID;


/**
 * A variant of the Chandra-Toueg atomic broadcast.
 * Consensus is not on vectors of messages,
 * but on vectors of message identifiers.
 */
public class ChandraTouegClient
    extends ProtocolImpl
    implements SenderInterface
{
    /**
     * Reliable broadcast.
     */
    protected SenderInterface rbcast;

    private NekoProcess process;

    public ChandraTouegClient(NekoProcess process) {
        this.process = process;
    }

    public void setRbcast(SenderInterface rbcast) {
        this.rbcast = rbcast;
    }

    public void send(NekoMessage m) {

        GUID guid = new GUID(process);
        NekoMessage m1 =
            new NekoMessage(m.getSource(),
                            m.getDestinations(),
                            getId(),
                            new ContentRB(guid,
                                          m.getSource(),
                                          m.getProtocolId(),
                                          m.getContent(),
                                          m.getType()),
                            MessageTypeConst.AB_START);
        rbcast.send(m1);

    }

}







