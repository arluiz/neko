package lse.neko;

// lse.neko imports:
import lse.neko.util.Timer;

// other imports:
import org.apache.java.util.Configurations;


/**
 * This class is a singleton. It contains generic functionality, and
 * provides access to features that differ between simulations and
 * executions on a network.
 */
public abstract class NekoSystem {

    protected static NekoSystem theInstance;

    public static NekoSystem instance() {
        return theInstance;
    }

    private static void initInstance(NekoSystem instance) {
        if (theInstance != null) {
            throw new RuntimeException("Initializing NekoSystem twice!");
        }
        theInstance = instance;
    }

    protected abstract
    NekoThreadInterface createPeerThread(NekoThread thread,
                                         Runnable runnable,
                                         String name);

    public abstract NekoObjectInterface createObject();

    public abstract NekoObjectInterface createObject(Object object);

    public abstract void shutdown(int phase);

    public abstract void shutdown(int phase, String errorMessage);

    public synchronized void run() {
        networks = NekoInitializer.getNetworks(config);
        createProcesses(getProcessIds());
        initNetworks();
        initProcesses();
        start();
    }

    protected abstract void initNetworks();

    protected abstract void start();

    private Configurations config;

    public Configurations getConfig() {
        return config;
    }

    private static final String CF_PROCESS_NUM = "process.num";

    public NekoSystem(Configurations config,
                      NekoThreadStaticInterface threadStatic)
    {
        initInstance(this);

        this.config = config;

        processNum = config.getInteger(CF_PROCESS_NUM, 0);
        if (processNum < 0) {
            throw new IllegalArgumentException("process.num should be >= 0!");
        }
        // processNum == 0 is useful for unit tests that need a NekoSystem
        // but no processes / networks / protocol stacks

        if (threadStatic == null) {
            throw new NullPointerException();
        }
        this.threadStatic = threadStatic;
    }

    private NekoThreadStaticInterface threadStatic;

    NekoThreadStaticInterface getThreadStatic() {
        return threadStatic;
    }

    private SenderInterface[] networks;

    public SenderInterface[] getNetworks() {
        return networks;
    }

    private int processNum;

    public int getProcessNum() {
        return processNum;
    }

    private NekoProcess[] processes;

    /**
     * Returns the IDs of processes that this Java Virtual Machine
     * should run.
     */
    protected abstract int[] getProcessIds();

    /**
     * Creates the list of processes managed by this Java Virtual Machine.
     *
     * @param ids the processes are created with these IDs.
     */
    private void createProcesses(int[] ids) {
        // XXX: check the ids
        // (non-null, unique, 0 <= id < processNum, sorted in increasing order)
        processes = new NekoProcess[ids.length];
        for (int i = 0; i < ids.length; i++) {

            NekoProcess process = new NekoProcess(ids[i]);
            processes[i] = process;
            NekoThread.currentThread().setProcess(process);

            // add a ProcessSender and a ProcessReceiver
            // for each network to the process
            SenderInterface[] processSenders =
                new SenderInterface[networks.length];
            ProcessReceiver[] processReceivers =
                new ProcessReceiver[networks.length];
            for (int netIndex = 0; netIndex < networks.length; netIndex++) {

                ProcessReceiver processReceiver =
                    new ProcessReceiver(process);
                processReceiver.setId(NekoProcess.getReceiverId(netIndex));
                processReceiver.setReceiver(process.getDispatcher());
                processReceiver.launch();
                processReceivers[netIndex] = processReceiver;

                SenderInterface network = networks[netIndex];

                ProcessSender processSender =
                    new ProcessSender(process);
                processSender.setId(NekoProcess.getSenderId(netIndex));
                processSender.setSender(network);
                processSender.launch();
                processSenders[netIndex] = processSender;
            }
            process.setReceivers(processReceivers);
            process.setNetworks(processSenders);
            // NekoProcess.getNetworks and getDefaultNetwork
            // can be used to look up the ProcessSenders
            // FIXME: this breaks code that tries to check the
            // class type of these networks!
        }

        NekoThread.currentThread().setProcess(null);
    }

    /**
     * Initializes the processes in this Java Virtual Machine.
     */
    private void initProcesses() {
        NekoThread currentThread = NekoThread.currentThread();
        for (int i = 0; i < processes.length; i++) {
            NekoProcess proc = processes[i];
            currentThread.setProcess(proc);
            proc.init();
        }
        currentThread.setProcess(null);
    }

    /**
     * Returns a process managed by this Java Virtual Machine.
     * These processes are indexed from 0 in the order of their IDs
     * (that is, not necessarily by their IDs!).
     */

    public NekoProcess getNekoProcess(int i) {
        return processes[i];
    }

    public abstract boolean isSimulation();

    /**
     * Returns the current (simulated or real) time.
     */
    public abstract double clock();

    private Timer timer;
    private Object lock = new Object();

    /**
     * Returns the system-wide timer object.
     *
     * @see lse.neko.util.Timer
     */
    public Timer getTimer() {
        synchronized (lock) {
            if (timer == null) {
                timer = new Timer() {
                    public String toString() {
                        return "system";
                    }
                };
            }
        }
        return timer;
    }

    public abstract double getInitialClock();

}
