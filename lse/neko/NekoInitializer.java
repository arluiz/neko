package lse.neko;

// java imports:
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.ExceptionHandler;
import lse.neko.util.LauncherCatchingExceptions;
import lse.neko.util.logging.NekoLogManagerInitializer;
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;
import org.apache.java.util.ExtendedProperties;


/**
 * Common part of initialization for all Neko applications.
 */
public class NekoInitializer {

    /**
     * Processes command line arguments.
     * This involves reading the configuration file.
     * @param args command line arguments to a main() function
     * @return the configuration
     */
    public static Configurations getConfig(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: program_name configuration_file");
            System.exit(2);
        }

        ExtendedProperties ep;
        try {
            ep = new ExtendedProperties(args[0]);
        } catch (IOException ex) {
            throw new RuntimeException("Configuration file " + args[0]
                                       + " unreadable!");
        }
        return new Configurations(ep);
    }

    public static final String CF_NETWORK = "network";

    /**
     * Creates the array of networks that the application uses,
     * based on information in the configuration.
     */
    public static SenderInterface[] getNetworks(Configurations config) {

        String[] classNames =
            config.getStringArray(CF_NETWORK);
        if (classNames == null) {
            classNames = new String[0];
        }

        SenderInterface[] ret = new SenderInterface[classNames.length];

        for (int i = 0; i < classNames.length; i++) {
            String className = classNames[i];

            try {
                Class theClass = Class.forName(className);
                SenderInterface network =
                    (SenderInterface) theClass.newInstance();
                ret[i] = network;
            } catch (Exception ex) {
                throw new RuntimeException("Cannot create network of type "
                                           + className, ex);
            }
        }

        return ret;

    }

    /**
     * Second phase of Neko startup, called when having the configuration.
     * Initializes Neko logging, launches a new thread running
     * the <code>run</code> method of <code>runnable</code>
     * and returns. The calling thread should finish after calling
     * this method; the rest of the application should be started by
     * <code>runnable</code>.
     * All uncaught exceptions raised after calling this method are
     * caught and logged. Also, <code>NekoSystem.shutdown</code>
     * is called.
     *
     * @param config the configuration of the Neko application.
     * @param runnable the rest of the application code.
     */
    public static void initLog(Configurations config, Runnable runnable) {
        NekoLogManagerInitializer.init(config);
        new LauncherCatchingExceptions(runnable, new MyExceptionHandler());
    }

    private static class MyExceptionHandler
        implements ExceptionHandler
    {
        /**
         * Handles the exception e thrown by thread t.
         */
        public void handle(Thread t, Throwable e) {

            // get the NekoSystem singleton object
            // might be null if not initialized yet
            NekoSystem system = NekoSystem.instance();

            // Even though the NekoSystem object is useful for this
            // method, we do not wait for its initialization.
            // Reason: we want logging available as soon as possible,
            // even for the initialization of the NekoSystem object.

            // construct a string message about the exception
            // FIXME: it might be better just to forward the exception
            // to the logging framework, because then
            // the user could decide the format of the message

            // get the current process
            // XXX: the code is assuming that t == Thread.currentThread()
            // is this always true?
            NekoProcess process = (system != null)
                ? NekoThread.currentThread().getProcess() : null;
            String processName = (process == null)
                ? ""
                : " of process #" + process.getID();

            // get the name of the thread
            String threadName = (system != null)
                ? NekoThread.currentThread().getName() : t.getName();

            // get the stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            try {
                sw.close();
            } catch (IOException ex) {
                // unexpected
                // shutdown must continue anyway
            }
            String stackTrace = sw.toString();

            // assemble the message
            String message =
                "UNCAUGHT EXCEPTION by thread " + threadName
                + processName + ":\n" + stackTrace;

            if (system == null) {
                // log the exception
                Logger logger = NekoLogger.getLogger("global.neko");
                logger.severe(message);
                // quit with an error code
                System.exit(1);
            } else {
                // shut down the Neko application
                // give the message as reason
                // the shutdown function takes care of logging the message
                NekoSystem.instance().shutdown(2, message);
            }
        }
    }
}
