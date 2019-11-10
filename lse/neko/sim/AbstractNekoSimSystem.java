package lse.neko.sim;

// java imports:
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.AbstractId;
import lse.neko.NekoSystem;
import lse.neko.NekoThreadStaticInterface;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.networks.sim.SimNetwork;
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;


public abstract class AbstractNekoSimSystem
    extends NekoSystem
{

    public AbstractNekoSimSystem(Configurations config,
                                 NekoThreadStaticInterface threadStatic)
    {
        super(config, threadStatic);
    }

    public boolean isSimulation() {
        return true;
    }

    protected int[] getProcessIds() {
        int n = getProcessNum();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = i;
        }
        return ids;
    }

    private static class Id extends AbstractId {
        public Id(Object name) {
            super(name);
        }
    }

    protected void initNetworks() {
        SenderInterface[] networks = getNetworks();
        int processNum = getProcessNum();
        for (int i = 0; i < networks.length; i++) {

            SimNetwork network;
            try {
                network = (SimNetwork) networks[i];
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Network " + i
                                                   + " is not a simulated "
                                                   + "network");
            }

            network.setId(new Id("network-" + i));

            ReceiverInterface[] receivers =
                new ReceiverInterface[processNum];
            for (int j = 0; j < processNum; j++) {
                receivers[j] = getNekoProcess(j).getReceivers()[i];
            }
            network.setReceivers(receivers);

        }
    }

    public void shutdown(int phase) {
        shutdown(phase, null);
    }

    public void shutdown(int phase, String errorMessage) {
        // log the error
        if (errorMessage != null) {
            Logger logger = NekoLogger.getLogger("global.neko");
            logger.severe(errorMessage);
        }

        // no need to shutdown networks
        /*
        // FIXME: same code as in ShutdownStack follows

        for (int p = phase; p >= 0; p--) {

            Network[] nets = getNetworks();
            for (int i = 0; i < nets.length; i++) {
                nets[i].shutdown(p);
            }
            //if (p == 2) {
            //        process.dropMessages();
            //}

        }
        */

        // Needed when Neko is run with hprof
        // to get realistic SITES information
        System.gc();
        System.exit((errorMessage == null) ? 0 : 1);

    }
}
