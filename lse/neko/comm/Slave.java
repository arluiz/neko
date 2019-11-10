package lse.neko.comm;

// java imports:
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

// lse.neko imports:
import lse.neko.NekoInitializer;


/**
 * Starts up the slave part of a network application.
 * The (optional) argument is the port number the slave listens on.
 * See Master for how startup happens.
 * @see Master
 */
public class Slave
    implements Runnable
{

    public static void main(String[] args) {
        new Slave(args);
    }

    private Config config;
    private ServerSocket serverSocket;

    public Slave(String[] args) {

        // Processing arguments
        if (args.length > 1) {
            usage();
        }
        int port = Config.DEFAULT_NEKO_PORT;
        if (args.length == 1) {
            try {
                port = Integer.valueOf(args[0]).intValue();
                if (port < 0) {
                    usage();
                }
            } catch (NumberFormatException ex) {
                usage();
            }
        }

        try {

            // Create a server socket.
            serverSocket = new ServerSocket(port);
            port = serverSocket.getLocalPort();
            System.out.println("Ready on port " + port);
            // Accept the connection coming from the master
            Socket socket = serverSocket.accept();
            // Read the configuration
            ObjectInput is =
                new ObjectInputStream(socket.getInputStream());
            config = (Config) is.readObject();
            socket.close();

        } catch (IOException ex) {
            throw new RuntimeException("Communication error while "
                                       + "distributing the configuration",
                                       ex);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Wrong configuration data", ex);
        }

        NekoInitializer.initLog(config.getConfigurations(), this);
    }

    public void run() {
        new NekoCommSystem(config, serverSocket).run();
    }

    public void usage() {
        System.err.println("Usage: slave [port_number]");
        System.err.println("A port_number of 0 means that");
        System.err.println("the system is free to choose the port number.");
        System.exit(2);
    }

}
