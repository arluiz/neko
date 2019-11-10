package lse.neko.comm;

// java imports:
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;


/**
 * Starts up the slave part of a network application.
 * The (optional) argument is the port number the slave listens on.
 * See Master for how startup happens.
 * @see Master
 */
public class TestServerFactory {

    public static void main(String[] args) {
        new TestServerFactory(args);
    }

    public TestServerFactory(String[] args) {

        // Processing arguments
        if (args.length != 1) {
            usage();
        }
        StringTokenizer tokens = new StringTokenizer(args[0], ":");
        if (!tokens.hasMoreTokens()) {
            usage();
        }
        String hostname = tokens.nextToken();
        int port = Config.DEFAULT_FACTORY_PORT;
        if (tokens.hasMoreTokens()) {
            try {
                port = Integer.valueOf(tokens.nextToken()).intValue();
            } catch (NumberFormatException ex) {
                usage();
            }
        }
        if (tokens.hasMoreTokens()) {
            usage();
        }

        try {

            BufferedReader stdin = new BufferedReader(new
                InputStreamReader(System.in));

            while (true) {

                // read the request
                String request = stdin.readLine();
                if (request == null) {
                    break;
                }

                // open a connection
                InetAddress address = InetAddress.getByName(hostname);
                Socket socket =
                    new Socket(address, port);
                // write the request
                BufferedWriter os =
                    new BufferedWriter(new
                        OutputStreamWriter(socket.getOutputStream()));
                os.write(request + "\n");
                os.flush();
                // read the reply
                BufferedReader is =
                    new BufferedReader(new
                        InputStreamReader(socket.getInputStream()));
                while (true) {
                    String reply = is.readLine();
                    if (reply == null) {
                        break;
                    }
                    System.out.println(reply);
                }
                socket.close();

            }

        } catch (IOException ex) {
            throw new RuntimeException("Communication error", ex);
        }

    }

    public void usage() {
        System.err.println("Usage: java " + getClass().getName()
                           + " hostname[:port]");
        System.exit(2);
    }

}
