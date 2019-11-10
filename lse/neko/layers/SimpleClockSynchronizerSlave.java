package lse.neko.layers;

import java.util.logging.Logger;

import lse.neko.NekoMessage;
import lse.neko.NekoSystem;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.comm.NekoCommSystem;
import lse.neko.util.logging.NekoLogger;

public class SimpleClockSynchronizerSlave
    extends ProtocolImpl
    implements ReceiverInterface
{

    public SimpleClockSynchronizerSlave() {
    }

    public void deliver(NekoMessage m) {
        if (m.getType() == SimpleClockSynchronizer.MASTERCLOCK_SYNC) {
            logger.fine("Initial clock before sync: "
                + NekoSystem.instance().getInitialClock());
            double initialClockMaster = ((Double) m.getContent()).doubleValue();
            ((NekoCommSystem) NekoSystem.instance()).adjustClock(
                            NekoSystem.instance().getInitialClock()
                            - initialClockMaster);
            logger.fine("Initial clock after sync: "
                + NekoSystem.instance().getInitialClock());
            return;
        }
        throw new UnexpectedMessageException(m);
    }

    private static final Logger logger =
        NekoLogger.getLogger(SimpleClockSynchronizerSlave.class.getName());

}
