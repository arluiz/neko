package lse.neko.layers;

// java imports:
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoMessageQueue;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.ProtocolImpl;
import lse.neko.SenderInterface;
import lse.neko.sim.nekosim.NekoSimSystem;
import lse.neko.util.TimerTask; // ambiguous with: java.util.TimerTask
import lse.neko.util.Util;
import lse.neko.util.logging.NekoLogger;

// other imports:
import org.apache.java.util.Configurations;
import org.apache.java.util.ExtendedProperties;


/**
 * This class imposes a maximum rate of messages sent through this layer.
 * It works by buffering messages to make sure that a minimum delay
 * elapses between two sends.
 */
public class LeakyBucket
    extends ProtocolImpl
    implements SenderInterface
{

    public LeakyBucket(double minDelay) {
        Runnable aSender = new Sender(minDelay);
        new NekoThread(aSender).start();
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    public SenderInterface getSender() {
        return sender;
    }

    private NekoMessageQueue queue = new NekoMessageQueue();

    public void send(NekoMessage m) {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,
                       "leaky bucket size {0,number,#} insert {1}",
                       new Object[] {
                           new Integer(queue.getSize()),
                           m
                       });
        }
        queue.put(m);
    }

    public int size() {
        return queue.getSize();
    }

    private class Sender
        implements Runnable
    {
        private final double minDelay;
        private double earliestNextSend;

        public Sender(double minDelay) {
            if (minDelay <= 0) {
                throw new IllegalArgumentException();
            }
            this.minDelay = minDelay;
            earliestNextSend = NekoSystem.instance().clock();
        }

        public void run() {
            while (true) {
                NekoMessage m = queue.get();
                double time = NekoSystem.instance().clock();
                if (earliestNextSend > time) {
                    try {
                        NekoThread.sleep(earliestNextSend - time);
                    } catch (InterruptedException ex) {
                    }
                }
                time = NekoSystem.instance().clock();
                earliestNextSend = time + minDelay;
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER,
                               "leaky bucket size {0,number,#} send {1}",
                               new Object[] {
                                   new Integer(queue.getSize()),
                                   m
                               });
                }
                sender.send(m);
            }
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(LeakyBucket.class.getName());

    /**
     * Unit test.
     */
    public static void main(String[] args) {

        // initalize the system
        final NekoSimSystem system =
            new NekoSimSystem(new Configurations(new ExtendedProperties()));

        final double[] in = new double[] {
            1.0, 3.0, 3.5, 3.7, 7.0, 7.0, 7.1
        };
        final double[] referenceOut = new double[] {
            1.0, 3.0, 4.0, 5.0, 7.0, 8.0, 9.0
        };
        final double endTime = 1000;

        final List outList = new ArrayList();
        for (int i = 0; i < in.length; i++) {
            final TimerTask task = new TimerTask() {
                    public void run() {
                        outList.add(new Double(system.clock()));
                    }
                };
            system.getTimer().schedule(task, in[i]);
        }

        final TimerTask endTask = new TimerTask() {
                public void run() {

                    final double[] out = new double[outList.size()];
                    for (int i = 0; i < out.length; i++) {
                        out[i] = ((Double) outList.get(i)).doubleValue();
                    }

                    if (Arrays.equals(in, out)) {
                        System.out.println("Test successful");
                    } else {
                        System.out.println("in  = " + Util.toString(in));
                        System.out.println("out = " + Util.toString(out));
                        System.out.println("Test failed");
                    }
                }
            };
        system.getTimer().schedule(endTask, endTime);

        system.run();
    }
}

