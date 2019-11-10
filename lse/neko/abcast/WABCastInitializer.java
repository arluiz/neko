package lse.neko.abcast;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoThread;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;


public class WABCastInitializer
    implements ABCastInitializer
{

    public void createDeliverer(NekoProcess process) {
        createSender(process);
    }

    public SenderInterface createSender(NekoProcess process) {
        SenderInterface net = process.getDefaultNetwork();
        abcast = new WABCast(process);
        abcast.setSender(net);

        abcast.setUdpSender(getUdpSender());

        final Object abcastId = "abcast";
        abcast.setId(abcastId);
        abcast.launch();
        return abcast;
    }

    public static SenderInterface getUdpSender() {
        return getNetwork(UDP_NETWORK_CLASSNAME);
    }

    private static final String UDP_NETWORK_CLASSNAME =
        "lse.neko.networks.comm.SimpleMulticastNetwork";

    private static SenderInterface getNetwork(String className) {
        try {
            Class c = Class.forName(className);
            SenderInterface[] networks =
                NekoThread.currentThread().getProcess().getNetworks();
            for (int i = 0; i < networks.length; i++) {
                if (c.isInstance(networks[i])) {
                    return networks[i];
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error getting network", ex);
        }
        return null;
    }

    public SenderInterface createSenderDeliverer(NekoProcess process) {
        return createSender(process);
    }

    private WABCast abcast;

    public void setReceiver(ReceiverInterface receiver) {
        abcast.setReceiver(receiver);
    }

}
