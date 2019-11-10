package lse.neko.comm;

// java imports:
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
// import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoInitializer;
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Connects all slave factories specified in the config file
 * and instructs them to kill all slaves they started.
 */
public class KillSlaves {

    public static void main(String[] args) {
        new KillSlaves(args);
    }

    private Config config;

    public KillSlaves(String[] args) {

        // Processing arguments
        boolean readCommands = false;
        String[] newArgs;
        if (args.length > 0 && args[0].equals("-x")) {
            readCommands = true;
            newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        } else {
            newArgs = args;
        }

        // Getting the configuration
        Configurations configurations = NekoInitializer.getConfig(newArgs);

        // Extracting setup information from the configuration
        config = new Config(configurations);

        // Constructing a reader for the input
        BufferedReader is;
        if (readCommands) {
            is = new BufferedReader(new InputStreamReader(System.in));
        } else {
            is = new BufferedReader(new StringReader("all kill\n"));
        }

        // Array of commands, one for each slave factory
        int n = config.getNumProcesses();
        List[] commands = new List[n];
        for (int i = 1; i < n; i++) {
            commands[i] = new ArrayList();
        }

        // Prepare the array of commands
        while (true) {

            String commandWithRange = null;
            try {

                commandWithRange = is.readLine();
                if (commandWithRange == null) {
                    break;
                }
                StringTokenizer tokens =
                    new StringTokenizer(commandWithRange);
                String range = tokens.nextToken();
                String command = tokens.nextToken();
                try {
                    command += tokens.nextToken("");
                } catch (NoSuchElementException ex) {
                }

                logger.fine("range *" + range + "*");
                logger.fine("command *" + command + "*");

                if (range.equals("all")) {
                    for (int i = 1; i < n; i++) {
                        commands[i].add(command);
                    }
                } else {
                    int index = Integer.parseInt(range);
                    if (index < 1 || index >= n) {
                        throw new NoSuchElementException();
                    }
                    commands[index].add(command);
                }

            } catch (NoSuchElementException ex) {
                throw new RuntimeException("Invalid input: "
                                           + commandWithRange);
            } catch (IOException ex) {
                throw new RuntimeException("Error reading input");
            }

        }

        /*
        for (int i=1; i<n; i++) {
            logger.fine("Commands for factory #"+i+":");
            Iterator it = commands[i].iterator();
            while (it.hasNext()) {
                String c = (String)it.next();
                logger.fine("  "+c);
            }
        }
        */

        for (int i = 1; i < config.getNumProcesses(); i++) {
            try {
                execute(i, commands[i]);
            } catch (IOException ex) {
                throw new RuntimeException("Communication error "
                                           + "(slave factory "
                                           + i + ")", ex);
            }
        }

    }

    private void execute(int i, List requests)
        throws IOException
    {

        //tests if this slave was created through a slavefactory
        if (!config.hasFactory(i)) {
            return;
        }

        Iterator it = requests.iterator();
        while (it.hasNext()) {
            String request = (String) it.next() + "\n";
            logger.fine("request " + request);

            // connect to a slave factory which will create a new slave
            Socket socket =
                new Socket(config.getInetAddress(i),
                           config.getFactoryPort(i));
            logger.fine("socket opened");
            OutputStream out = socket.getOutputStream();
            OutputStreamWriter osr = new OutputStreamWriter(out);
            BufferedWriter buffout = new BufferedWriter(osr);

            // send the request
            buffout.write(request, 0, request.length());
            buffout.flush();
            logger.fine("request written");

            // read the replies
            InputStream in = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader buffin = new BufferedReader(isr);

            // parse the replies
            logger.fine("reading reply");
            String reply = buffin.readLine();
            logger.fine("reply read");
            boolean error = !reply.equals("ok");

            // handle errors
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

    public void usage() {
        System.err.println("Usage: java " + getClass().getName()
                           + " [-x] configuration_file");
        System.exit(2);
    }

    private static final Logger logger =
        NekoLogger.getLogger(KillSlaves.class.getName());
}
