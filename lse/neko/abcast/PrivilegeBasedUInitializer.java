package lse.neko.abcast;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;


public class PrivilegeBasedUInitializer implements ABCastInitializer {

    public void createDeliverer(NekoProcess process) {
        createSender(process);
    }

    public SenderInterface createSender(NekoProcess process) {
        SenderInterface net = process.getDefaultNetwork();
        abcast = new PrivilegeBasedU(process);
        abcast.setSender(net);
        final Object abcastId = "abcast";
        abcast.setId(abcastId);
        abcast.launch();
        return abcast;
    }

    public SenderInterface createSenderDeliverer(NekoProcess process) {
        return createSender(process);
    }

    private PrivilegeBasedU abcast;

    public void setReceiver(ReceiverInterface receiver) {
        abcast.setReceiver(receiver);
    }

} // end class PrivilegeBasedUInitializer
