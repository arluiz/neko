package lse.neko.comm;

// java imports:
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Contains configuration information used to set up networks.
 * One instance is created, at Neko application startup.
 * @see CommNetwork#init
 */
public class Config
    implements Serializable
{

    /**
     * Default port number that Neko processes listen on
     * at startup.
     */
    public static final int DEFAULT_NEKO_PORT = 8632;

    /**
     * Default port number that Neko process factories listen on.
     */
    public static final int DEFAULT_FACTORY_PORT = 8765;

    // configuration directives
    public static final String CF_SLAVE = "slave";
    public static final String CF_NUMPROC = "process.num";
    public static final String CF_MASTER = "master";

    /**
     * @return the embedded full configuration.
     */
    public Configurations getConfigurations() {
        return config;
    }

    /**
     * @return the IP address of process #i.
     */
    public InetAddress getInetAddress(int i) {
        return (InetAddress) addresses.get(i);
    }

    /**
     * @return the port number of process #i.
     */
    public int getPort(int i) {
        return ((Integer) ports.get(i)).intValue();
    }

    /**
     * Sets the port number of process #i.
     * Only used at the very beginning of startup,
     * when server factories create slaves and return their port
     * numbers.
     */
    public void setPort(int i, int port) {
        ports.set(i, new Integer(port));
    }

    /**
     * @return the number of processes.
     */
    public int getNumProcesses() {
        return addresses.size();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("number of processes: " + getNumProcesses() + "\n");
        sb.append("this process: " + getProcessId() + "\n");
        sb.append("addresses ports:\n");
        for (int i = 0; i < getNumProcesses(); i++) {
            sb.append("  " + getInetAddress(i).getHostName()
                      + " " + getPort(i) + "\n");
        }
        return sb.toString();
    }

    /**
     * For testing.
     */
    public static void main(String[] args) {
        try {
            Config c = new Config
                (new Configurations
                 (new org.apache.java.util.ExtendedProperties
                  (args[0])));
            System.out.println(c);
        } catch (java.io.IOException ex) {
            System.out.println("Error!");
            System.exit(1);
        }
        System.exit(0);
    }

    private Configurations config;
    private List addresses = new ArrayList();
    private List ports = new ArrayList();
    private List factories = new ArrayList();
    private List factoryports = new ArrayList();

    /**
     * Extracts information needed to set up networks
     * out of the full configuration
     * and keeps it inside the object.
     * FIXME: A problem with this constructor is that it does DNS lookups.
     * Usually, only the Master process should do that;
     * others only need to lookup
     * the master (if at all).
     */
    public Config(Configurations config) {

        this.config = config;

        try {

            String masterHostName = "";
            Integer masterPort = new Integer(0);
            String master =
                config.getString(CF_MASTER, null);
            if (master != null) {
                StringTokenizer st = new StringTokenizer(master, ":");
                masterHostName = st.nextToken();
                if (st.hasMoreTokens()) {
                    masterPort = Integer.valueOf(st.nextToken());
                    if (st.hasMoreTokens()) {
                        throw new RuntimeException("Invalid master "
                                                   + "specification");
                    }
                }
            }
            InetAddress masterHost;
            if (masterHostName.equals("")) {
                masterHost = InetAddress.getLocalHost();
            } else {
                masterHost = InetAddress.getByName(masterHostName);
            }
            addresses.add(masterHost);
            ports.add(masterPort);

            String[] sa = config.getStringArray(CF_SLAVE);
            int numProc = config.getInteger(CF_NUMPROC, sa.length + 1);
            if (numProc < 1) {
                throw new RuntimeException("The number of processes ("
                                           + CF_NUMPROC
                                           + ") should be positive");
            } else if (numProc > sa.length + 1) {
                throw new RuntimeException("The number of processes ("
                                           + CF_NUMPROC
                                           + ") is too high, "
                                           + "not enough slaves");
            }
            Integer port, fport;

            //Extracting the slaves startup configurations
            for (int i = 1; i < numProc; i++) {
                String slave = sa[i - 1];
                // cut 'username@' part
                int at = slave.indexOf('@');
                if (at >= 0) {
                    slave = slave.substring(at + 1);
                }
                StringTokenizer st = new StringTokenizer(slave, ":");
                InetAddress host = InetAddress.getByName(st.nextToken());
                addresses.add(host);

                if (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    StringTokenizer st1 = new StringTokenizer(s, "-");
                    if (st1.nextToken().equals("factory")) {
                        factories.add(Boolean.TRUE);
                        fport = (st1.hasMoreTokens())
                            ? Integer.valueOf(st1.nextToken())
                            : new Integer(DEFAULT_FACTORY_PORT);
                        factoryports.add(fport);
                        if (st1.hasMoreTokens()) {
                            throw new RuntimeException("Invalid slave "
                                                       + "specification");
                        }
                        port = st.hasMoreTokens()
                            ? Integer.valueOf(st.nextToken())
                            : new Integer(0);
                        ports.add(port);
                        if (st.hasMoreTokens()) {
                            throw new RuntimeException("Invalid slave "
                                                       + "specification");
                        }
                    } else {
                        if (st1.hasMoreTokens()) {
                            throw new RuntimeException("Invalid slave "
                                                       + "specification");
                        }
                        factories.add(Boolean.FALSE);
                        fport = new Integer(DEFAULT_FACTORY_PORT);
                        factoryports.add(fport);
                        port = Integer.valueOf(s);
                        ports.add(port);
                        if (st.hasMoreTokens()) {
                            throw new RuntimeException("Invalid slave "
                                                       + "specification: "
                                                       + slave);
                        }
                    }
                } else {
                    factories.add(Boolean.FALSE);
                    fport = new Integer(DEFAULT_FACTORY_PORT);
                    factoryports.add(fport);
                    port =  new Integer(DEFAULT_NEKO_PORT);
                    ports.add(port);
                }
            }
        } catch (UnknownHostException ex) {
            throw new RuntimeException("Cannot find the address of a host!");
        }
    }

    private int processId = 0;

    /**
     * Set the id of the process that has this object.
     */
    public void setProcessId(int processId) {
        this.processId = processId;
    }

    /**
     * @return the id of the process that has this object.
     */
    public int getProcessId() {
        return processId;
    }

    /**
     * @return if process #i should be created a server factory.
     */
    public boolean hasFactory(int i) {
        return ((Boolean) factories.get(i - 1)).booleanValue();
    }

    /**
     * @return the port number of the server factory used to
     * create process #i.
     */
    public int getFactoryPort(int i) {
        return ((Integer) factoryports.get(i - 1)).intValue();
    }
}
