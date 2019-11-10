package lse.neko.examples.basic;

// java imports:
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.ActiveReceiver;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.SenderInterface;
import lse.neko.util.logging.NekoLogger;


/**
 * Lamport's total order algorithm.
 * Just the communication pattern of one run.
 * See the paper
 * <blockquote>Lam78 <br>
 * L.~Lamport. <br>
 * Time, clocks, and the ordering of events in a distributed system. <br>
 * Commun. ACM, 21(7):558--565, July 1978.</blockquote>
 * for details of the algorithm.
 * The <code>run</code> method implements the algorithm.
 */
public class Lamport
    extends ActiveReceiver
{

    private static int finished;
    private static double startTime;

    // message types used by this algorithm
    private static final int LAMPORT = 1225;
    private static final int LAMPORT_NULL = 1226;

    // registering the message types and associating names with the types.
    static {
        MessageTypes.instance().register(LAMPORT, "LAMPORT");
        MessageTypes.instance().register(LAMPORT_NULL, "LAMPORT_NULL");
    }

    public Lamport(NekoProcess process) {
        super(process, "Lamport-p" + process.getID());
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    public void run() {

        // the number of processes
        int n = process.getN();
        // the id of this process
        int me = process.getID();

        // constructing a list of addresses
        // that includes all processes but this one
        int[] allButMe = new int[n - 1];
        for (int i = 0; i < n - 1; i++) {
            allButMe[i] = (i < me) ? i : i + 1;
        }

        if (me == 0) {

            // executed by process #0

            if (NekoSystem.instance().isSimulation()) {
                // only for simulations
                startTime = clock();
                finished = 0;
            }

            // the algorithm
            sender.send(new NekoMessage(allButMe, getId(), null, LAMPORT));
            for (int i = 0; i < n - 1; i++) {
                receive();
            }

        } else {

            // executed by all other processes
            receive();
            sender.send(new NekoMessage(allButMe, getId(), null, LAMPORT_NULL));
            for (int i = 0; i < n - 2; i++) {
                receive();
            }

        }

        if (NekoSystem.instance().isSimulation()) {
            // only for simulations
            finished++;
            if (finished == n) {
                logger.info("finishing at " + (clock() - startTime));
                process.shutdown();
            }
        }

        logger.info("p" + me + " finished");

        if (!NekoSystem.instance().isSimulation()) {
            // The code just before only works with simulations.
            // In distributed executions, static members are _not_
            // shared by all processes, so avoid using static
            // members. Use only messages for communication.

            // In this example, we just wait 2 seconds and
            // then decide to shut down all processes of the application.
            try {
                sleep(2000);
            } catch (InterruptedException ex) {
            }
            process.shutdown();
        }

    }

    private static final Logger logger =
        NekoLogger.getLogger(Lamport.class.getName());
}
