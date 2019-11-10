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
 */
public class MovingSequencerUniform
    extends ActiveReceiver
{
    private int n;
    private int me;
    private int[] allButP1;
    private int[] allButP2;

    private static int finished;
    private static double startTime;

    private static final int INITIAL = 1242;
    private static final int SEQ = 1243;
    private static final int TOKEN = 1244;

    public MovingSequencerUniform(NekoProcess process) {
        super(process, "MovingSequencerUniform");
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

        allButP2 = new int[n - 1];
        for (int i = 1; i < n - 1; i++) {
            allButP2[i] = i + 1;
        }
        allButP2[0] = 0;

        if (me == 0) {

            receive();
            sender.send(new NekoMessage(allButP1, getId(), null, SEQ));
            receive();
            sender.send(new NekoMessage(new int[] { 1 },
                                        getId(),
                                        null,
                                        TOKEN));

        } else if (me == 1) {

            startTime = clock();
            finished = 0;

            sender.send(new NekoMessage(allButP2, getId(), null, INITIAL));
            receive();
            sender.send(new NekoMessage(new int[] { 2 % n },
                                        getId(),
                                        null,
                                        TOKEN));
            receive();
            if (n > 2) {
                sender.send(new NekoMessage(new int[] { 2 },
                                            getId(),
                                            null,
                                            TOKEN));
            }

        } else {

            for (int token = 0; token < 2; token++) {
                NekoMessage m;
                do {
                    m = receive();
                } while (m.getType() != TOKEN);
                if (token == 0 || me != n - 1) {
                    sender.send(new NekoMessage(new int[] { (me + 1) % n },
                                                getId(),
                                                null,
                                                TOKEN));
                }
            }

        }

        finished++;
        if (finished == n) {
            logger.info("finishing at " + (clock() - startTime));
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(MovingSequencerUniform.class.getName());
}


