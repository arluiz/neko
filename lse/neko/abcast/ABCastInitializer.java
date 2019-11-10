package lse.neko.abcast;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;


/**
 * The ABCast test application uses this interface
 * to initialize a concrete ABCast implementation.
 */
public interface ABCastInitializer {
    // FIXME: this interface is wrong, because one also needs the set of all
    // servers for initialization

    void createDeliverer(NekoProcess process);

    SenderInterface createSender(NekoProcess process);

    SenderInterface createSenderDeliverer(NekoProcess process);

    /**
     * Sets the receiver for the microprotocols created by
     * createDeliverer or createSenderDeliverer.
     */
    void setReceiver(ReceiverInterface receiver);

}
