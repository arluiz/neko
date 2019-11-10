package lse.neko.layers;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;

// other imports:
import org.apache.java.util.Configurations;


public class ClockSynchronizerTest
    implements NekoProcessInitializer
{

    public void init(NekoProcess process, Configurations config)
        throws Exception
    {
        SenderInterface net = process.getDefaultNetwork();

        final Object latencyTestId = "latencyTest";
        final Object syncId = "sync";

        if (process.getID() == 0) {
            LatencyTest latencyTest =
                new LatencyTest(process);
            latencyTest.setId(latencyTestId);
            latencyTest.setSender(net);
            Test sender =
                new Test(process, latencyTest);
            sender.setId(syncId);
            sender.setSender(net);
            latencyTest.launch();
            sender.launch();
        } else {
            ClockSynchronizerSlave receiver =
                new ClockSynchronizerSlave(process, latencyTestId);
            receiver.setId(syncId);
            receiver.setSender(net);
            receiver.launch();
        }
    }

    private static class Test
        extends ClockSynchronizer
    {

        public Test(NekoProcess process,
                    LatencyTest latencyTest)
        {
            super(process, latencyTest);
        }

        public void run() {
            synchronize(0.05 /* 50 microsecond */);
            process.shutdown();
        }

    }

}
