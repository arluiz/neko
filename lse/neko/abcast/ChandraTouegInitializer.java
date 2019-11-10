package lse.neko.abcast;

// lse.neko imports:
import lse.neko.Dispatcher;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.consensus.ConsensusCTInitializer;
import lse.neko.consensus.ConsensusInterface;
import lse.neko.consensus.ConsensusMRInitializer;
import lse.neko.consensus.ConsensusPaxosInitializer;
import lse.neko.rbcast.FakeRBCast;
import lse.neko.rbcast.NackRBroadcast;
import lse.neko.rbcast.RBroadcast;

// other imports:
import org.apache.java.util.Configurations;


public class ChandraTouegInitializer
    implements ABCastInitializer
{

    final Object abcastId = "abcast";

    public void createDeliverer(NekoProcess process) {
        createSenderDeliverer(process);
    }

    public SenderInterface createSender(NekoProcess process) {

        Configurations config = NekoSystem.instance().getConfig();
        SenderInterface net = process.getDefaultNetwork();
        Dispatcher dispatcher = process.getDispatcher();
        ChandraTouegClient abcastClient = new ChandraTouegClient(process);
        abcastClient.setId(abcastId);
        final Object rbcastId = "rbcast";
        if (config.getBoolean(FAKE_RBCAST, false)) {
            FakeRBCast rbcast = new FakeRBCast(process);
            rbcast.setId(rbcastId);
            abcastClient.setRbcast(rbcast);
            rbcast.setSender(net);
            rbcast.setReceiver(dispatcher);
            rbcast.launch();
        } else if (config.getBoolean(NACK_RBCAST, false)) {
            NackRBroadcast rbcast = new NackRBroadcast(process);
            rbcast.setId(rbcastId);
            abcastClient.setRbcast(rbcast);
            rbcast.setSender(net);
            rbcast.setReceiver(dispatcher);
            rbcast.launch();
        } else {
            RBroadcast rbcast = new RBroadcast(process);
            rbcast.setId(rbcastId);
            abcastClient.setRbcast(rbcast);
            rbcast.setSender(net);
            rbcast.setReceiver(dispatcher);
            rbcast.launch();
        }
        abcastClient.launch();
        return abcastClient;

    }

    public static final String NACK_RBCAST = "algorithm.rbcast.nackbased";
    public static final String FAKE_RBCAST = "algorithm.rbcast.fake";
    public static final String CONSENSUS_INIT = "consensus.initializer";

    public static final String CONSENSUS_INIT_CT =
        "lse.neko.consensus.ConsensusCTInitializer";
    public static final String CONSENSUS_INIT_MR =
        "lse.neko.consensus.ConsensusMRInitializer";
    public static final String CONSENSUS_INIT_PX =
        "lse.neko.consensus.ConsensusPaxosInitializer";

    public SenderInterface createSenderDeliverer(NekoProcess process) {

        Configurations config = NekoSystem.instance().getConfig();

        ConsensusInterface theConsensus;
        SenderInterface theRB;
        String consensusInitializerName =
            config.getString(CONSENSUS_INIT, CONSENSUS_INIT_CT);
        if (consensusInitializerName.equals(CONSENSUS_INIT_CT)) {
            ConsensusCTInitializer ci = new ConsensusCTInitializer();
            // FIXME: ugly way of getting the group
            // (assumes that lse.neko.abcast.tests.LatencyTest is running)
            int[] servers = lse.neko.abcast.tests.TestConstants.getServers();
            if (servers != null) {
                theConsensus = ci.init(process, config, servers);
            } else {
                theConsensus = ci.init(process, config);
            }

            theRB = ci.getRBCast();
        } else if (consensusInitializerName.equals(CONSENSUS_INIT_MR)) {
            ConsensusMRInitializer ci = new ConsensusMRInitializer();
            theConsensus = ci.init(process, config);
            theRB = ci.getRBCast();
        } else if (consensusInitializerName.equals(CONSENSUS_INIT_PX)) {
            ConsensusPaxosInitializer ci = new ConsensusPaxosInitializer();
            int[] servers = lse.neko.abcast.tests.TestConstants.getServers();
            if (servers != null) {
                theConsensus = ci.init(process, config, servers);
            } else {
                theConsensus = ci.init(process, config);
            }
            theRB = ci.getRBCast();
        } else {
            throw new RuntimeException("Value of " + CONSENSUS_INIT
                                       + " illegal!");
        }

        abcast = new ChandraToueg(process, theConsensus);
        abcast.setId(abcastId);
        abcast.setRbcast(theRB);
        theConsensus.setDecisionListener(abcast);
        abcast.launch();
        return abcast;
    }

    private ChandraToueg abcast;

    public void setReceiver(ReceiverInterface receiver) {
        abcast.setReceiver(receiver);
    }

}
