package lse.neko.abcast;

// lse.neko imports:
import lse.neko.Dispatcher;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.failureDetectors.DefaultRingFailureDetectorInitializer;
import lse.neko.failureDetectors.FailureDetectorInterface;
import lse.neko.rbcast.FakeRBCast;
import lse.neko.rbcast.RBroadcast;

// other imports:
import org.apache.java.util.Configurations;


public class TokenAndFDBasedInitializer
    implements ABCastInitializer
{

    public void createDeliverer(NekoProcess process) {
        createSenderDeliverer(process);
    }

    public SenderInterface createSender(NekoProcess process) {
        return createSenderDeliverer(process);
    }

    public static final String FAKE_RBCAST = "algorithm.rbcast.fake";
    public static final String FAILURES = "algorithm.f";


    public static final String MAXPROPOSALSIZE =
        "lse.neko.abcast.TokenAndFDBased.maxproposalsize";

    public static final String BROADCASTDECISIONS =
        "lse.neko.abcast.TokenAndFDBased.broadcastdecisions";
    public static final String SENDATONCE =
        "lse.neko.abcast.TokenAndFDBased.sendatonce";
    public static final String MULTIPLEPROPOSALS =
        "lse.neko.abcast.TokenAndFDBased.multipleproposals";

    public static final String TSEND = "heartbeat.send";
    public static final String TRECEIVE = "heartbeat.timeout";

    public SenderInterface createSenderDeliverer(NekoProcess process) {

        Configurations config = NekoSystem.instance().getConfig();
        SenderInterface net = process.getDefaultNetwork();
        Dispatcher dispatcher = process.getDispatcher();

        SenderInterface theRB = null;
        FailureDetectorInterface theFD = null;

        int f = -1;

        // get the RBCast
        if (config.getBoolean(FAKE_RBCAST, false)) {
            FakeRBCast rbcast = new FakeRBCast(process);
            rbcast.setSender(net);
            rbcast.setReceiver(dispatcher);
            theRB = rbcast;
        } else {
            RBroadcast rbcast = new RBroadcast(process);
            rbcast.setSender(net);
            rbcast.setReceiver(dispatcher);
            theRB = rbcast;
        }
        final Object rbcastId = "rbcast";
        theRB.setId(rbcastId);

        // get the Failure detector, the FD layer is added by the FDInitializer
        theFD = (FailureDetectorInterface)
            new DefaultRingFailureDetectorInitializer().init(process, config);

        //new RingHeartbeat(process,
        //                  config.getInteger(TSEND, 500), // ms
        //                  config.getInteger(TRECEIVE, 1000)); // ms

        f = config.getInteger(FAILURES, -1);

        if (f == -1) {
            throw new RuntimeException("The number of tolerated failures "
                                       + "must be indicated as 'algorithm.f' "
                                       + "in the configuration file "
                                       + "for TokenAndFDBased abcast");
        }

        boolean sendAtOnce = config.getBoolean(SENDATONCE, true);
        boolean multipleProposals = config.getBoolean(MULTIPLEPROPOSALS, true);
        boolean broadcastDecisions =
            config.getBoolean(BROADCASTDECISIONS, true);

        int propSize = config.getInteger(MAXPROPOSALSIZE, -1);

        theAbcast =
            new TokenAndFDBased(process,
                    theRB,
                    theFD,
                    f,
                    sendAtOnce,
                    multipleProposals,
                    broadcastDecisions);

        if (propSize != -1) {
            theAbcast.setMaxProposalSize(propSize);
        }

        final Object abcastId = "abcast";
        theAbcast.setId(abcastId);

        theRB.launch();
        theAbcast.launch();

        return theAbcast;
    }

    private TokenAndFDBased theAbcast;

    public void setReceiver(ReceiverInterface receiver) {
        theAbcast.setReceiver(receiver);
    }

}
