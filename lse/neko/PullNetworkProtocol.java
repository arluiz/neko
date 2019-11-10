package lse.neko;

public class PullNetworkProtocol
    extends PullProtocol
    implements PullNetworkInterface
{

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    public void send(NekoMessage m) {
        sender.send(m);
    }

}
