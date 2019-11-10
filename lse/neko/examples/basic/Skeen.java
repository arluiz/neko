package lse.neko.examples.basic;

// java imports:
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.ActiveReceiver;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.SenderInterface;
import lse.neko.util.logging.NekoLogger;


/**
 * Skeen's total order algorithm.
 * Just the communication pattern of one run.
 * See the papers
 * <blockquote>BJ87<br>
 * K.~P. Birman and T.~A. Joseph.<br>
 * Reliable communication in presence of failures.<br>
 * ACM Trans. Comput. Syst., 5(1):47--76, Feb. 1987.</blockquote>
 * <blockquote>GS97f<br>
 * R.~Guerraoui and A.~Schiper.<br>
 * Total order multicast to multiple groups.<br>
 * In Proc. 17th Int'l Conf. on Distributed Computing Systems
 * (ICDCS), pages 578--585, May 1997.</blockquote>
 */
public class Skeen
    extends ActiveReceiver
{
    private int n;
    private int me;
    private int[] allButP1;

    private static int finished;
    private static double startTime;

    private static final int SKEEN_INITIAL = 1222;
    private static final int SKEEN_ACK = 1223;
    private static final int SKEEN_STABLE = 1224;

    public Skeen(NekoProcess process) {
        super(process, "Skeen");
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    public void run() {

        n = process.getN();
        me = process.getID();

        allButP1 = new int[n - 1];
        for (int i = 0; i < n - 1; i++) {
            allButP1[i] = i + 1;
        }

        if (me == 0) {

            startTime = clock();
            finished = 0;

            sender.send(new NekoMessage(allButP1, getId(),
                                        null, SKEEN_INITIAL));
            for (int i = 0; i < n - 1; i++) {
                receive(); // SKEEN_ACKs
            }
            sender.send(new NekoMessage(allButP1, getId(),
                                        null, SKEEN_STABLE));

        } else {

            receive(); // SKEEN_ACKs
            NekoMessage out = new NekoMessage(new int[] { 0 },
                                              getId(),
                                              null,
                                              SKEEN_ACK);
            sender.send(out);
            receive();

        }

        finished++;
        if (finished == n) {
            logger.info("finishing at " + (clock() - startTime));
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(Skeen.class.getName());
}


