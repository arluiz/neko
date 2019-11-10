package lse.neko.layers;

// java.util imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.ProtocolImpl;
import lse.neko.SenderInterface;
import lse.neko.util.logging.NekoLogger;


public class LeakyBucketSwitch
    extends ProtocolImpl
    implements SenderInterface
{

    private SenderInterface sender;

    private LeakyBucket leakyBucket;

    private boolean insertIntoLeakyBucket;

    public LeakyBucketSwitch(SenderInterface sender,
                             LeakyBucket leakyBucket)
    {
        if (sender == null) {
            throw new NullPointerException("sender cannot be null!");
        }
        if (leakyBucket == null) {
            throw new NullPointerException("leakyBucket cannot be null!");
        }
        this.sender = sender;
        this.leakyBucket = leakyBucket;
    }

    public void send(NekoMessage m) {
        logger.log(Level.FINER, "send {0}", m);
        if ((m.getType() != FixedSequencerNU.NU_GET_SEQNUM
             && m.getType() != FixedSequencerU.U_GET_SEQNUM)
            || leakyBucket.size() == 0 && !insertIntoLeakyBucket)
        {
            sender.send(m);
        } else {
            leakyBucket.send(m);
        }
    }

    public void setInsertIntoLeakyBucket(boolean insertIntoLeakyBucket) {
        logger.log(Level.FINE, "setInsertIntoLeakyBucket {0}",
                   Boolean.valueOf(insertIntoLeakyBucket));
        this.insertIntoLeakyBucket = insertIntoLeakyBucket;
    }

    private static final Logger logger =
        NekoLogger.getLogger(LeakyBucketSwitch.class.getName());

}
