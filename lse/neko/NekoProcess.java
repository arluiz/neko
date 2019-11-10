package lse.neko;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Processes are the entities that communicate
 * using message passing over networks.
 * Each process has an address,
 * an integer identifier (0, 1, etc.).
 * In a distributed execution, each process is usually
 * on a different host.
 * Each process has an associated protocol composition that
 * implements the Neko application.
 * A microprotocol can always access its process by calling
 * <code>NekoThread.currentThread().getProcess();</code>
 * special microprotocols that intercept communication to and from
 * the process make this work.
 * The process exposes some of its microprotocols with getter methods.
 * It provides support for shutting down the application.
 *
 * @see NekoThread#getProcess
 * @see ProcessSender
 * @see ProcessReceiver
 */
public class NekoProcess {

    private int id;

    public static final String CF_PROCESS = "process";
    public static final String CF_INITIALIZER = "initializer";

    /**
     * Creates a new process.
     * Only called by Neko initialization code;
     * users of Neko should not create processes.
     * @param id A unique identifier for the process.
     */
    public NekoProcess(int id) {
        this.id = id;

        dispatcher = new Dispatcher();
        // FIXME: for the time being, using dispatcher's ID
        // will lead to an infinite loop
        dispatcher.setId(dispatcherId);
        dispatcher.launch();
    }

    private static class DispatcherId {
        public String toString() {
            return "process-dispatcher";
        }
    }

    private static class ReceiverId {

        private int id;

        public ReceiverId(int id) {
            this.id = id;
        }

        public boolean equals(Object o) {
            if (o instanceof ReceiverId) {
                ReceiverId s = (ReceiverId) o;
                return id == s.id;
            }
            return false;
        }

        private static final int SEED = 23288;

        public int hashCode() {
            return SEED + id;
        }

        public String toString() {
            return "process-receiver-" + id;
        }

    }

    private static class SenderId {

        private int id;

        public SenderId(int id) {
            this.id = id;
        }

        public boolean equals(Object o) {
            if (o instanceof SenderId) {
                SenderId s = (SenderId) o;
                return id == s.id;
            }
            return false;
        }

        private static final int SEED = 23237;

        public int hashCode() {
            return SEED + id;
        }

        public String toString() {
            return "process-sender-" + id;
        }

    }

    private static Object dispatcherId = new DispatcherId();

    public static Object getDispatcherId() {
        return dispatcherId;
    }

    public static Object getReceiverId(int id) {
        return new ReceiverId(id);
    }

    public static Object getSenderId(int id) {
        return new SenderId(id);
    }

    private Dispatcher dispatcher;

    /**
     * Returns the microprotocol that can dispatch a NekoMessage to
     * the microprotocol indicated by the protocol ID embedded in the
     * NekoMessage.
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public ProcessReceiver[] getReceivers() {
        return (ProcessReceiver[]) receivers.clone();
    }

    private ProcessReceiver[] receivers;

    public void setReceivers(ProcessReceiver[] receivers) {
        this.receivers = receivers;
    }

    /**
     * Returns the microprotocol intercepting all messages going to
     * the default network.
     */
    public SenderInterface getDefaultNetwork() {
        return networks[0];
    }

    /**
     * Returns the microprotocols intercepting all outgoing messages.
     * There is one such microprotocol for each network.
     */
    public SenderInterface[] getNetworks() {
        return (SenderInterface[]) networks.clone();
    }

    private SenderInterface[] networks;

    public void setNetworks(SenderInterface[] networks) {
        this.networks = (SenderInterface[]) networks.clone();
    }

    /**
     * Sets up the protocol composition associated with this process.
     * Only called by Neko initialization code.
     */
    public void init() {

        Configurations config = NekoSystem.instance().getConfig();
        if (config != null) {

            String cfThisProcess = CF_PROCESS + "." + id + ".";
            String initializer =
                config.getString(cfThisProcess + CF_INITIALIZER, null);
            if (initializer == null) {
                initializer =
                    config.getString(CF_PROCESS + "." + CF_INITIALIZER, null);
            }
            if (initializer == null) {
                throw new RuntimeException("There is no initializer class "
                                           + "for process #" + id);
            } else {
                NekoProcessInitializer init;
                try {
                    Class aClass = Class.forName(initializer);
                    init = (NekoProcessInitializer) aClass.newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException("Cannot initialize class "
                                               + initializer, ex);
                }
                try {
                    init.init(this, config);
                } catch (java.lang.reflect.InvocationTargetException ex) {
                    throw new RuntimeException("Error in init method: ",
                                               ex.getCause());
                } catch (Exception ex) {
                    throw new RuntimeException("Error while initializing "
                                               + "process " + id, ex);
                }
            }

        }
    }

    /**
     * Shuts down the whole Neko application.
     * @param phase number of shutdown phases
     */
    public void shutdown(int phase) {
        NekoSystem.instance().shutdown(phase);
    }

    /**
     * Shuts down the whole Neko application.
     */
    public void shutdown() {
        shutdown(2);
    }

    /**
     * @return the unique identifier for the process.
     */
    public int getID() {
        return id;
    }

    public String toString() {
        return "p" + id;
    }

    // FIXME: remove, as redundant:
    /**
     * @return the number of processes in the system.
     */
    public static int getN() {
        return NekoSystem.instance().getProcessNum();
    }

    /**
     * @return the current local time for the process
     * in milliseconds.
     */
    public double clock() {
        return NekoSystem.instance().clock();
    }

}
