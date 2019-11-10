package lse.neko.comm;

// lse.neko imports:
import lse.neko.ProtocolImpl;
import lse.neko.PullNetworkInterface;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;


public abstract class CommNetwork
    extends ProtocolImpl
    implements SenderInterface
{

    protected ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    /**
     * Initializes the network using the configuration and the control
     * network.  This method should use the send() and receive()
     * methods of the control network to exchange information needed
     * to initialize the network. This method should receive all
     * messages sent by the init() methods of other processes,
     * otherwise the init methods of other networks will be confused
     * by the incoming messages intended for this network. It is
     * guaranteed that the control network is up when this method is
     * called.
     */
    public abstract void init(Config config,
                              PullNetworkInterface controlNetwork);

    /**
     * Called to indicate that messages can be delivered.  This method
     * is called after init() finishes. No incoming messages should be
     * delivered before it is called.
     */
    public abstract void startDelivering();

    public abstract void shutdown(int phase);

}
