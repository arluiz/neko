package lse.neko.comm;

// java imports:
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// lse.neko imports:
import lse.neko.AbstractId;
import lse.neko.MessageTypeConst;
import lse.neko.NekoMessage;
import lse.neko.NekoObjectInterface;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.NekoThreadInterface;
import lse.neko.PullNetworkInterface;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.failureDetectors.Heartbeat;
import lse.neko.layers.ShutdownStack;
import lse.neko.networks.comm.ControlNetwork;
import lse.neko.util.MySystem;


/**
 * This class contains generic functionality.
 */
public class NekoCommSystem
    extends NekoSystem
{

    protected NekoThreadInterface createPeerThread(NekoThread thread,
                                                   Runnable runnable,
                                                   String name)
    {
        return new NekoCommThread(thread, runnable, name);
    }

    public NekoObjectInterface createObject() {
        return new NekoCommObject();
    }

    public NekoObjectInterface createObject(Object object) {
        return new NekoCommObject(object);
    }

    /**
     * Shuts down the whole Neko application.
     * @param phase number of shutdown phases
     */
    public void shutdown(int phase) {
        shutdown(phase, null);
    }

    public void shutdown(int phase, String errorMessage) {
        if (phase < 2) {
            throw new RuntimeException("The shutdown should have "
                                       + "at least two phases!");
        }

        shutdownReceiver.shutdown(phase, errorMessage);
    }

    private ShutdownInterface shutdownReceiver;

    public NekoCommSystem(Config parsedConfig, ServerSocket serverSocket) {
        super(parsedConfig.getConfigurations(),
              new NekoCommThreadStatic());
        preciseClock = getConfig().getBoolean("clock.precise", false);
        initialClock = (double) System.currentTimeMillis();
        this.parsedConfig = parsedConfig;
        this.serverSocket = serverSocket;
    }

    private boolean preciseClock;
    private Config parsedConfig;
    private ServerSocket serverSocket;

    protected int[] getProcessIds() {
        return new int[] { parsedConfig.getProcessId() };
    }

    private static class Id extends AbstractId {
        public Id(Object name) {
            super(name);
        }
    }

    private NetworkInitLayer initControlNetwork() {

        NekoProcess proc = getNekoProcess(0);
        NekoThread.currentThread().setProcess(proc);

        // Create the control network.
        ControlNetwork controlNetwork =
            new ControlNetwork(parsedConfig, serverSocket);
        controlNetwork.setReceiver(proc.getDispatcher());

        // Build the protocol stack of the control network.
        NetworkInitLayer networkInitLayer = new NetworkInitLayer();
        networkInitLayer.setId(new Id("networkInitLayer"));
        networkInitLayer.setSender(controlNetwork);

        // No heartbeats are sent and no suspicions are generated
        // unless shutdown has started.
        final Heartbeat fd =
            new Heartbeat(proc,
                          Double.MAX_VALUE, // no heartbeats are sent
                          Double.MAX_VALUE, // no suspicions unless shutdown
                          null); // monitor every process
        fd.setId(new Id("fd"));
        fd.setSender(controlNetwork);

        final Object shutdownId = new Id("shutdown");
        shutdownReceiver = new ShutdownInterface() {
                public void shutdown(int phase, String errorMessage) {
                    int p = NekoSystem.instance().getNekoProcess(0).getID();
                    NekoMessage m =
                        new NekoMessage(p,
                                        new int[] { p },
                                        shutdownId,
                                        new ShutdownStack.Content(phase,
                                                                  errorMessage),
                                        MessageTypeConst.SHUTDOWN);
                    fd.deliver(m);
                }
            };

        ShutdownStack shutdownLayer =
            new ShutdownStack(proc, fd, controlNetwork);
        shutdownLayer.setId(shutdownId);
        shutdownLayer.setSender(controlNetwork);
        fd.setReceiver(shutdownLayer);
        fd.setListener(shutdownLayer);

        networkInitLayer.launch();
        fd.launch();
        shutdownLayer.launch();

        controlNetwork.startDelivering();

        NekoThread.currentThread().setProcess(null);

        return networkInitLayer;
    }

    protected void initNetworks() {

        NetworkInitLayer networkInitLayer = initControlNetwork();

        // Initialize the network objects.
        SenderInterface[] networks = getNetworks();
        for (int i = 0; i < networks.length; i++) {

            CommNetwork network;
            try {
                network = (CommNetwork) networks[i];
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Network " + i
                                                   + " is not a communication "
                                                   + "network");
            }
            Id id = new Id("network-" + i);
            network.setId(id);
            ReceiverInterface receiver = getNekoProcess(0).getReceivers()[i];
            network.setReceiver(receiver);
            PullNetworkInterface initNetwork =
                networkInitLayer.registerNetwork(id);
            network.init(parsedConfig, initNetwork);
            networkInitLayer.unregisterNetwork(id);
        }

        // from now on, prevent threads from starting
        blockStart();
    }

    private boolean startBlocked = false;

    private synchronized void blockStart() {
        startBlocked = true;
    }

    protected void start() {

        // Networks can start delivering
        SenderInterface[] networks = getNetworks();
        for (int i = 0; i < networks.length; i++) {

            CommNetwork network;
            try {
                network = (CommNetwork) networks[i];
            } catch (ClassCastException ex) {
                throw new RuntimeException("A network is not a CommNetwork");
            }
            network.startDelivering();
        }

        unblockStart();
    }

    private synchronized void unblockStart() {
        // Start all threads of the application.
        startBlocked = false;
        Iterator it = threads.iterator();
        while (it.hasNext()) {
            Thread t = (Thread) it.next();
            t.start();
        }
        threads.clear();
        threads = null;

    }

    private List threads = new ArrayList();

    /**
     * NekoCommThread calls this function to register threads being
     * started if starting threads is blocked.  If starting threads is
     * not blocked, nothing happens.
     *
     * @return whether starting threads is blocked.
     */
    synchronized boolean registerThread(Thread t) {
        if (startBlocked) {
            threads.add(t);
        }
        return startBlocked;
    }

    private double initialClock;

    public double getInitialClock() {
        return initialClock;
    }

    public double clock() {
        if (preciseClock) {
            return (double) (MySystem.currentTimeMicros()) / 1000
                - initialClock;
        } else {
            return (double) (System.currentTimeMillis()) - initialClock;
        }
    }

    public void adjustClock(double increment) {
        initialClock -= increment;
    }

    public boolean isSimulation() {
        return false;
    }

}
