package lse.neko.comm;

// java imports:
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Starts up the master part of a network application.
 * The startup of a network application happens as follows:<p>
 * The master reads the configuration file.
 * Entries like
 * <code>slave = myhost.mydomain:myport</code>
 * identify slaves.
 * (the slavenumber configuration variable instructs the master
 * to take only the first slavenumber slaves.)
 * These slaves must listen at the address/port specified.
 * The server communicates the addresses of the master and the slave
 * as well as the configuration file to all slaves.
 */
public class Master {

    public Master(Configurations configurations) {

        // Extracting setup information from the configuration
        Config config = new Config(configurations);

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(config.getPort(0));
            // if the port is 0 meaning "any port",
            // the actual port must be put into config
            int port = serverSocket.getLocalPort();
            config.setPort(0, port);
        } catch (IOException ex) {
            throw new RuntimeException("Could not open server socket", ex);
        }

        int i = 0;
        try {

            // Create the slaves which are created using factories
            for (i = 1; i < config.getNumProcesses(); i++) {
                createWithFactory(config, i);
            }

            for (i = 1; i < config.getNumProcesses(); i++) {
                // Send the configuration information to all slaves
                Socket socket =
                    new Socket(config.getInetAddress(i), config.getPort(i));
                ObjectOutput os =
                    new ObjectOutputStream(socket.getOutputStream());
                config.setProcessId(i);
                os.writeObject(config);
                socket.close();
            }
            config.setProcessId(0);

        } catch (IOException ex) {
            throw new RuntimeException("Communication error while "
                                       + "distributing the configuration "
                                       + "to slave " + i, ex);
        }

        new NekoCommSystem(config, serverSocket).run();
    }

    private void createWithFactory(Config config, int i)
        throws IOException
    {

        //tests if this process must be created through a slavefactory
        if (config.hasFactory(i)) {

            // connect to a slave factory which will create a new slave
            Socket socket =
                new Socket(config.getInetAddress(i),
                           config.getFactoryPort(i));
            OutputStream out = socket.getOutputStream();
            OutputStreamWriter osr = new OutputStreamWriter(out);
            BufferedWriter buffout = new BufferedWriter(osr);

            // prepare the request
            String request =
                "create " + Integer.toString(config.getPort(i)) + "\n";

            // send the request
            buffout.write(request, 0, request.length());
            buffout.flush();

            // read the reply
            InputStream in = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader buffin = new BufferedReader(isr);
            String reply = buffin.readLine();

            // parse the reply
            boolean error = true;
            StringTokenizer tokens = new StringTokenizer(reply);
            try {

                if (tokens.nextToken().equals("created")) {
                    tokens.nextToken(); // to skip the pid
                    if (tokens.nextToken().equals("port")) {
                        int port = Integer.parseInt(tokens.nextToken());
                        // put the port number received into config
                        config.setPort(i, port);
                        error = false;
                    }
                }

            } catch (NoSuchElementException ex) {
            } catch (NumberFormatException ex) {
            }

            if (error) {
                // return all of the reply
                try {
                    while (true) {
                        String replyLine = buffin.readLine();
                        if (replyLine == null) {
                            break;
                        }
                        reply = reply + replyLine;
                    }
                    socket.close();
                } catch (IOException ex) {
                }
                throw new IOException("Got back an error "
                                      + "from the slave factory\n" + reply);
            }
            socket.close();
        }
    }
}
