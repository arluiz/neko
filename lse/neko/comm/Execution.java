package lse.neko.comm;

// java imports:
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Starts up one process of a Neko application.
 * All processes should run this class, with the name of the config file
 * as a parameter.
 * The config file should have one entry
 * <quote>
 * <code>master = myhost.mydomain:myport</code>
 * </quote>
 * and multiple entries
 * <quote>
 * <code>slave = myhost.mydomain:myport</code>
 * </quote>
 * The process reads the configuration file, then connects to the master
 * (the process itself could be the master).
 * The master sends the addresses of the master and the slave
 * as well as the configuration file to all slaves;
 * all this information is in a Config object.
 * The Config object also contains the identifier of the process
 * to which it is sent, such that the process can learn its identity.
 * <p>
 * The preferred way of starting Neko is with Master and Slave.
 * This way is provided for environments where the only convenient
 * way of starting up processes on multiple hosts is launching
 * the same code everywhere, at the same time.
 * E.g., an MPI program launched with the mpirun script on a cluster.
 * <p>
 * The config files of different processes can be different. Only the master
 * entries must match. Useful if you don't store the config file on a network
 * filesystem.
 * @see Config
 * @see Master
 * @see Slave
 */
public class Execution {

    private Config configFromMaster = null;
    private boolean master = false;
    private ServerSocket serverSocket;

    public Execution(Configurations configurations) {

        // Extracting setup information from the configuration
        Config config = new Config(configurations);

        // open a server socket. The master should listen there,
        // but we do not know yet who is the master.
        try {
            int port = config.getPort(0);
            if (port == 0) {
                port = Config.DEFAULT_NEKO_PORT;
                config.setPort(0, port);
            }
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            // This is probably not an error.
            // It simply means that a slave tried to use
            // the port of the master, and
            // as the master and the slave are on different machines (usually),
            // the same port is not guaranteed to work on both.
            serverSocket = null;
        }

        // start the thread that will provide configFromMaster
        new ContactServerThread(config);

        // accept connections. If there are incoming connections,
        // this process is the master,
        // and it should reply with the configuration

        Socket socket = null;
        if (serverSocket != null) {
            try {
                logger.fine("thread 1: accepting connections "
                            + "on the server socket of the master");
                socket = serverSocket.accept();
                DataInput is =
                    new DataInputStream(socket.getInputStream());
                master = is.readBoolean();
                logger.log(Level.FINE, "thread 1: accepted, with master = {0}",
                           Boolean.valueOf(master));
            } catch (IOException ex) {
                throw new RuntimeException("Error while waiting or receiving "
                                           + "from for incoming connections in "
                                           + getClass().getName());
            }
        }

        // code for the slave
        if (master) {
            configFromMaster = config;
            codeForMaster(socket);
        } else {
            codeForSlave(socket);
        }

        // initialization for both master and slave
        logger.log(Level.FINE,
                   "thread 1: further initialization starts\n"
                   + "serverSocket: {0}\n"
                   + "configFromMaster: {1}",
                   new Object[] { serverSocket, configFromMaster });
        logger.fine("thread 1: the application starts");
        new NekoCommSystem(configFromMaster, serverSocket).run();
    }

    private void codeForSlave(Socket socket) {

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Error when closing the connection");
            }
        }

        // wait for configFromMaster
        synchronized (this) {
            while (configFromMaster == null) {
                try {
                    logger.fine("thread 1: slave waiting for configFromMaster");
                    wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        logger.fine("thread 1: slave got configFromMaster");

    }

    /**
     * The master should send to configuration to each slave.
     * These connect to the serverSocket. One connection is already
     * established and passed as argument.
     */
    private void codeForMaster(Socket socketParam) {

        logger.fine("thread 1: master starts distributing the configuration");
        Config config = configFromMaster;

        int cnt = 0;
        try {

            Socket socket = socketParam;
            while (true) {

                // find the processId of the process
                // on the other end of the connection
                InetAddress address = socket.getInetAddress();
                int port = socket.getPort();
                logger.fine("thread 1: master looking for "
                            + address + ":" + port);
                int processId = 0; // the master, if no slave matches
                for (int i = 1; i < config.getNumProcesses(); i++) {
                    logger.fine("thread 1: master comparing with "
                                + config.getInetAddress(i)
                                + ":" + config.getPort(i));
                    // XXX: only comparing by IP address. Is this enough?
                    if (address.equals(config.getInetAddress(i))) {
                        processId = i;
                        break;
                    }
                }

                logger.fine("thread 1: master sends config to process "
                            + processId);
                config.setProcessId(processId);

                // send the configuration
                ObjectOutput os =
                    new ObjectOutputStream(socket.getOutputStream());
                os.writeObject(config);
                DataInput is2 = new DataInputStream(socket.getInputStream());
                boolean everythingFine = is2.readBoolean();
                if (!everythingFine) {
                    throw new RuntimeException("A slave signalled an error.");
                }
                socket.close();

                cnt++;
                if (cnt >= config.getNumProcesses()) {
                    break;
                }

                logger.fine("thread 1: master accepting incoming connections");
                socket = serverSocket.accept();

                // read the boolean indicating
                // that this is a connection to the master
                DataInput is =
                    new DataInputStream(socket.getInputStream());
                boolean connectionToMaster = is.readBoolean();
                if (!connectionToMaster) {
                    throw new RuntimeException("Unexpected event in the "
                                               + "startup protocol");
                }

            }

        } catch (IOException ex) {
            throw new RuntimeException("Communication error while "
                                       + "distributing the configuration "
                                       + "to slave", ex);
        }

        // restoring the process ID of the master
        // (it was changed when the Config object was sent to the slaves)
        config.setProcessId(0);

    }

    private class ContactServerThread
        extends Thread
    {

        private Config config;

        public ContactServerThread(Config config) {
            this.config = config;
            start();
        }

        public static final String CF_MASTER_RETRY_INTERVAL =
            "master.retry.interval";
        public static final int RETRY_INTERVAL = 1000;
        public static final String CF_MASTER_RETRY = "master.retry";
        public static final int RETRY = 5;

        public void run() {

            int retryInterval =
                config.getConfigurations().getInteger(CF_MASTER_RETRY_INTERVAL,
                                                      RETRY_INTERVAL);
            int retry = config.getConfigurations().getInteger(CF_MASTER_RETRY,
                                                              RETRY);

            try {

                // Try to repeatedly connect
                Socket socket;
                while (true) {
                    try {
                        logger.fine("thread 2: connecting to master");
                        socket = new Socket(config.getInetAddress(0),
                                            config.getPort(0));
                        DataOutput os =
                            new DataOutputStream(socket.getOutputStream());
                        // we are connecting to the master
                        os.writeBoolean(true);
                        break;
                    } catch (IOException ex) {
                        if (retry <= 0) {
                            throw ex;
                        }
                        retry--;
                        try {
                            sleep(retryInterval);
                        } catch (InterruptedException exx) {
                        }
                    }
                }
                logger.fine("thread 2: connected");

                // read the configuration
                ObjectInput is =
                    new ObjectInputStream(socket.getInputStream());
                logger.fine("thread 2: reading config");
                Config configRead = (Config) is.readObject();
                logger.fine("thread 2: read config");

                // meanwhile, master was set to true for the master.
                // Slaves should close the serverSocket to interrupt the accept
                // of the main thread.
                if (!master) {
                    configFromMaster = configRead;
                    // re-open the serverSocket on a different port
                    int newPort = configFromMaster
                        .getPort(configFromMaster.getProcessId());
                    logger.fine("thread 2: slave got id "
                                + configFromMaster.getProcessId());
                    logger.fine("thread 2: slave opens server socket on port "
                                + newPort);
                    try {

                        Socket socketToSelf =
                            new Socket(InetAddress.getLocalHost(),
                                       serverSocket.getLocalPort());
                        DataOutput os =
                            new DataOutputStream(socketToSelf
                                                     .getOutputStream());
                        os.writeBoolean(false); // we are connecting to a slave
                        socketToSelf.close();

                        if (newPort != serverSocket.getLocalPort()) {
                            serverSocket.close();
                            serverSocket = new ServerSocket(newPort);
                        }

                    } catch (IOException ex) {
                        throw new RuntimeException("Error while re-opening "
                                                   + "the server socket in "
                                                   + getClass().getName()
                                                   + ".codeForSlave()", ex);
                    }
                    synchronized (this) {
                        notify();
                    }
                }

                // Only close the socket now. Otherwise there will be
                // synchronization problems at startup,
                // for the master will likely try to connect
                // to the server socket of the slaves,
                // which might not yet be open.
                DataOutput os =
                    new DataOutputStream(socket.getOutputStream());
                os.writeBoolean(true);
                socket.close();

            } catch (IOException ex) {
                throw new RuntimeException("Communication error while "
                                           + "distributing the configuration",
                                           ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("Wrong configuration data", ex);
            }

            logger.fine("thread 2: done");

        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(Execution.class.getName());
}











