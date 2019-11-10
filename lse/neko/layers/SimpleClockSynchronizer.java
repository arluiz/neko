package lse.neko.layers;

import java.util.logging.Logger;

import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ProtocolImpl;
import lse.neko.SenderInterface;
import lse.neko.util.logging.NekoLogger;

/**
 * Synchronizes the clocks of all machines
 * in a NekoStat application.
 *
 * Clock must be synchronized outside of Neko, this layer set
 * only the process to use the same offset of the master.
 */

public class SimpleClockSynchronizer
    extends ProtocolImpl
{

    private int[] group;

    static final int MASTERCLOCK_SYNC = 9157;
    static {
        MessageTypes.instance().register(MASTERCLOCK_SYNC,
                                        "MASTERCLOCK_SYNC");
    }

    public SimpleClockSynchronizer(NekoProcess process) {
        this(process, getGroupWithoutMe(process));
    }

    private NekoProcess process;

    public SimpleClockSynchronizer(NekoProcess process,
                                   int[] group)
    {
        this.process = process;
        this.group = group;
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    private static int[] getGroupWithoutMe(NekoProcess process) {
        int[] group = new int[process.getN() - 1];
        for (int i = 1; i < process.getN(); i++) {
            if (i != process.getID()) {
                group[i - 1] = i;
            }
        }
       return group;
    }

    public void synchronize() {
        int sync = 0;
        logger.fine("Synchronization start");
        // send initialClock to all
        NekoMessage m =
            new NekoMessage(group,
                            getId(),
                            new Double(NekoSystem.instance().getInitialClock()),
                            MASTERCLOCK_SYNC);
        sender.send(m);
        logger.fine("Sent initialClock of the master to all slaves");
    }

    private static final Logger logger =
        NekoLogger.getLogger(SimpleClockSynchronizer.class.getName());
}
