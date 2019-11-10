package lse.neko.comm;

// java imports:
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


/**
 * Starts up the slave part of a network application.
 * The (optional) argument is the port number the slave listens on.
 * See Master for how startup happens.
 * @see Master
 */
public class ServerFactory {

    public static void main(String[] args) {
        new ServerFactory(args);
    }

    public ServerFactory(String[] args) {

        // Processing arguments
        if (args.length > 1) {
            usage();
        }
        int port = Config.DEFAULT_FACTORY_PORT;
        if (args.length == 1) {
            try {
                port = Integer.valueOf(args[0]).intValue();
            } catch (NumberFormatException ex) {
                usage();
            }
        }

        ServerSocket serverSocket;
        try {
            // Create a server socket.
            serverSocket = new ServerSocket(port);
            System.out.println("Ready on port " + port);
        } catch (IOException ex) {
            throw new RuntimeException("Error while creating server socket",
                                       ex);
        }

        ServerFactoryWorker worker = new ServerFactoryWorker();

        // repeat the following:
        // accept connections, read a one-line request, send the reply
        boolean active = true;
        while (active) {

            Socket socket = null;
            try {

                // Accept the connection coming from the master
                socket = serverSocket.accept();
                // Read the configuration
                BufferedReader is =
                    new BufferedReader(new
                        InputStreamReader(socket.getInputStream()));
                String request = is.readLine();
                logger.log(Level.FINE, "request: {0}", request);

                // process the request
                String reply;
                try {
                    reply = worker.processRequest(request);
                } catch (WantsToQuit ex) {
                    active = false;
                    reply = ex.getMessage();
                } catch (Throwable ex) {
                    reply = "error Exception while processing request\n"
                        + ex.getMessage();
                }

                logger.log(Level.FINE, "reply: {0}", reply);

                BufferedWriter os =
                    new BufferedWriter(new
                        OutputStreamWriter(socket.getOutputStream()));
                os.write(reply + "\n");
                os.flush();
                os.close();
                socket.close();

            } catch (IOException ex) {

                logger.log(Level.WARNING, "Communication error: ", ex);
                // try to close the socket
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ex2) {
                        // can't do much
                    }
                }
                // but don't quit

            }

        } // of while (active)

        // close the server socket
        try {
            serverSocket.close();
        } catch (IOException ex) {
            throw new RuntimeException("Error while closing server socket",
                                       ex);
        }
        System.exit(0);

    }

    public void usage() {
        System.err.println("Usage: java " + getClass().getName()
                           + " [port_number]");
        System.exit(2);
    }

    private static final Logger logger =
        NekoLogger.getLogger(ServerFactory.class.getName());
}
