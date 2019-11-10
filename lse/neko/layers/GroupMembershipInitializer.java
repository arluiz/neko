package lse.neko.layers;

// lse.neko imports:
import lse.neko.Dispatcher;
import lse.neko.MulticastProtocol;
import lse.neko.MulticastProtocolInterface;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.abcast.ABCastInitializer;
import lse.neko.consensus.ConsensusCT;
import lse.neko.failureDetectors.DefaultFailureDetectorInitializer;
import lse.neko.failureDetectors.FailureDetectorInterface;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.rbcast.RBroadcast;

// other imports:
import org.apache.java.util.Configurations;


/**
 * This class initalizes the protocol stack of a process
 * for the group membership test.
 */
public class GroupMembershipInitializer
    implements ABCastInitializer
{
    public void createDeliverer(NekoProcess process) {
        createSenderDeliverer(process);
    }

    public SenderInterface createSender(NekoProcess process) {
        return createSenderDeliverer(process);
    }

    public SenderInterface createSenderDeliverer(NekoProcess process) {

        final Object rbcastId = "rbcast";
        final Object fdMulticastId = "fdMulticast";
        final Object consensusId = "consensus";
        final Object gcsId = "gcs";
        final Object seqId = "seq";
        final Object stableId = "stable";
        final Object stateTransferId = "stateTransfer";
        final Object abcastId = "abcast";
        final Object viewMulticastId = "viewMulticast";
        final Object completedStateTransferMulticastId =
            "completedStateTransferMulticast";

        Configurations config = NekoSystem.instance().getConfig();

        SenderInterface net = process.getDefaultNetwork();

        Dispatcher dispatcher = process.getDispatcher();

        FailureDetectorInterface fd;

        fd = new DefaultFailureDetectorInitializer().init(process, config);

        RBroadcast theRB = new RBroadcast(process);
        theRB.setId(rbcastId);
        theRB.setSender(net);
        theRB.setReceiver(dispatcher);

        // multicast failure detector notification
        MulticastProtocolInterface fdMulticast =
            MulticastProtocol.newInstance(new Class[] {
                FailureDetectorListener.class
            });
        fdMulticast.setId(fdMulticastId);
        fd.setListener((FailureDetectorListener) fdMulticast);

        ConsensusCT theConsensus =
            new ConsensusCT(process, fd, theRB);
        theConsensus.setId(consensusId);
        theConsensus.setSender(net);
        theConsensus.setFailureDetectorMulticast(fdMulticast);

        // Inits the Layer that handles the membership
        GroupMembershipLayer gcs =
            new GroupMembershipLayer(process, fd, theConsensus);
        gcs.setId(gcsId);
        gcs.setSender(net);
        theConsensus.setDecisionListener(gcs);
        fdMulticast.addListener(gcs);

        MulticastProtocolInterface viewMulticast =
            MulticastProtocol.newInstance(new Class[] {
                NewViewListener.class
            });
        viewMulticast.setId(viewMulticastId);
        gcs.setNewViewListener((NewViewListener) viewMulticast);

        // Inits layer that gives a seq. number to a message,
        // notifies when this message is stable
        VSSequencerLayer seqLayer = new VSSequencerLayer(process);
        seqLayer.setId(seqId);
        seqLayer.setAckMembershipChangeListener(gcs);
        gcs.setMessageFromConsensusListener(seqLayer);
        viewMulticast.addListener(seqLayer);
        // FIXME:
        //replicator.registerShortCut(VSSequencerLayer.U_GET_SEQNUM, seqLayer);

        VSStabilityLayer stbl = new
            VSStabilityLayer(process, seqLayer,
                             config.getDouble("VSStabilityLayer.maxDelay"));
        stbl.setId(stableId);
        stbl.setAckMembershipChangeListener(seqLayer);
        viewMulticast.addListener(stbl);
        // FIXME:
        //replicator.registerShortCut(VSStabilityLayer
        //                            .VS_SEQUENTIAL_ACKNOWLEDGMENT, stbl);

        // this layer detects when a message became stable.
        stbl.setSender(net);
        stbl.setReceiver(dispatcher);

        seqLayer.setStableLayer(stbl);
        // This layer implements Atomic BroadCast protocol using
        // group communication system.

        // this layer is responsible to perform State Transfer for
        // wrongly suspected members.
        VSStateTransfer stateTransfer =
            new VSStateTransfer(process,
                                config.getDouble("VSStateTransfer.maxDelay"));
        stateTransfer.setId(stateTransferId);
        stateTransfer.setSender(net);
        stateTransfer.setReceiver(dispatcher);
        viewMulticast.addListener(stateTransfer);
        seqLayer.setReceiver(stateTransfer);

        MulticastProtocolInterface completedStateTransferMulticast =
            MulticastProtocol.newInstance(new Class[] {
                CompletedStateTransferListener.class
            });
        completedStateTransferMulticast
            .setId(completedStateTransferMulticastId);
        gcs.setNewViewListener((NewViewListener) viewMulticast);

        stateTransfer.setCompletedStateTransferListener(
            (CompletedStateTransferListener) completedStateTransferMulticast);
        completedStateTransferMulticast.addListener(gcs);

        // This layer garantees that a messages that has been send
        // during membership changes is to be delivered. This module
        // may be used as an example of application.
        abcast = new VSWrapper(process, seqLayer);
        abcast.setId(abcastId);
        abcast.setAckMembershipChangeListener(stbl);
        gcs.setMembershipChangeListener(abcast);
        completedStateTransferMulticast.addListener(abcast);
        viewMulticast.addListener(abcast);

        final Double minDelay =
            config.getDouble("FixedSequencerNU.leakyBucket.minDelay", null);
        if (minDelay == null) {
            seqLayer.setSender(stbl);
        } else {
            LeakyBucket leakyBucket = new LeakyBucket(minDelay.doubleValue());
            leakyBucket.setId("leakyBucket");
            LeakyBucketSwitch leakyBucketSwitch =
                new LeakyBucketSwitch(stbl, leakyBucket);
            leakyBucketSwitch.setId("leakyBucketSwitch");
            abcast.setLeakyBucketSwitch(leakyBucketSwitch);
            leakyBucket.setSender(stbl);
            seqLayer.setSender(leakyBucketSwitch);
            leakyBucketSwitch.launch();
            leakyBucket.launch();
        }

        theRB.launch();
        fdMulticast.launch();
        theConsensus.launch();
        gcs.launch();
        viewMulticast.launch();
        seqLayer.launch();
        stbl.launch();
        stateTransfer.launch();
        abcast.launch();

        return abcast;
    }

    private VSWrapper abcast;

    public void setReceiver(ReceiverInterface receiver) {
        abcast.setReceiver(receiver);
    }

}
