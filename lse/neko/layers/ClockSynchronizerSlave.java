package lse.neko.layers;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.comm.NekoCommSystem;


public class ClockSynchronizerSlave
    extends ProtocolImpl
    implements ReceiverInterface
{

    private NekoProcess process;

    private Object latencyTestId;

    public ClockSynchronizerSlave(NekoProcess process, Object latencyTestId) {
        this.process = process;
        this.latencyTestId = latencyTestId;
        eventCollector =
            new LatencyTest.EventCollector(process, 1);
        // sets the identifier of the latencyTest object that eventCollector
        // needs to communicate with
        eventCollector.setLatencyTestId(latencyTestId);
        // sets identifier of eventCollector != identifier of the latencyTest
        eventCollector.setId(ClockSynchronizer.EVENT_COLLECTOR_ID);

        // reply is initialized in launch() (after setId(...) has been called)
        reply = null;
        eventName = "receive" + process.getID();
    }

    private SenderInterface sender;

    public void setSender(SenderInterface sender) {
        this.sender = sender;
        eventCollector.setSender(sender);
    }

    public void launch() {
        // reply is initialized here (a correct return value
        // of getId() is needed)
        int[] dest = { 0 };
        reply = new NekoMessage(process.getID(),
                                dest,
                                getId(),
                                null,
                                ClockSynchronizer.SYNC);
        super.launch();
        eventCollector.launch();
    }

    private LatencyTest.EventCollector eventCollector;
    private int i = 0;
    private NekoMessage reply;
    private String eventName;

    public void deliver(NekoMessage m) {

        if (m.getType() != ClockSynchronizer.SYNC) {
            throw new UnexpectedMessageException(m);
        }

        if (i == 0) {
            eventCollector.register(eventName, 0);
            sender.send(reply);
        } else if (i == 1) {
            eventCollector.finish();
            eventCollector =
                new LatencyTest.EventCollector(process, 1);
            eventCollector.setLatencyTestId(latencyTestId);
            // sets ID of eventCollector (!= ID of the latencyTest)
            eventCollector.setId(ClockSynchronizer.EVENT_COLLECTOR_ID);
            eventCollector.setSender(sender);
            eventCollector.launch();
        } else if (i == 2) {
            double increment = ((Double) m.getContent()).doubleValue();
            ((NekoCommSystem) NekoSystem.instance()).adjustClock(increment);
        }
        i = (i + 1) % 3;
    }

}

