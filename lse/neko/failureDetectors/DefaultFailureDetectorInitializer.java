package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.Dispatcher;
import lse.neko.NekoProcess;
import lse.neko.ProcessReceiver;
import lse.neko.SenderInterface;

// other imports:
import org.apache.java.util.Configurations;


public class DefaultFailureDetectorInitializer
    implements FailureDetectorInitializer
{

    public static final String CF_SIMULATED = "fd.simulated";
    public static final String CF_SIMULATED_DETECTION_TIME =
        CF_SIMULATED + ".detectionTime";

    public static final String CF_SIMULATED_WRONG_SUSP =
        "fd.simulatedWithWrongSuspicions";
    public static final String CF_SIMULATED_T_MR =
        CF_SIMULATED_WRONG_SUSP + ".tMR";
    public static final String CF_SIMULATED_T_M =
        CF_SIMULATED_WRONG_SUSP + ".tM";

    public FailureDetectorInterface init(NekoProcess process,
                                         Configurations config)
    {
        int[] group = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            group[i] = i;
        }
        return init(process, config, group);
    }

    public FailureDetectorInterface init(NekoProcess process,
                                         Configurations config,
                                         int[] group)
    {
        FailureDetectorInterface theFD;

        Dispatcher dispatcher = process.getDispatcher();

        boolean useHeartbeat = config.getBoolean("heartbeat");
        if (useHeartbeat) {
            double send = config.getDouble("heartbeat.send");
            double timeout = config.getDouble("heartbeat.timeout");
            boolean useApplicationMessages =
                config.getBoolean("heartbeat.useApplicationMessages");
            boolean measureQoS =
                config.getBoolean("heartbeat.measureQoS");
            int        index = config.getInteger("heartbeat.network.index");
            SenderInterface failureDetectionNetwork =
                process.getNetworks()[index];
            Heartbeat h;
            if (measureQoS) {
                h = new HeartbeatMeasuringQoS(process, send, timeout,
                                              group);
            } else {
                h = new Heartbeat(process, send, timeout,
                                  group);
            }
            h.setSender(failureDetectionNetwork);
            //h.stop();
            h.useApplicationMessages(useApplicationMessages);
            h.setReceiver(dispatcher);

            // make sure that incoming messages from all networks
            // go through the failure detector
            ProcessReceiver[] nets = process.getReceivers();
            for (int i = 0; i < nets.length; i++) {
                nets[i].setReceiver(h);
            }

            theFD = h;
        } else if (config.getBoolean(CF_SIMULATED, false)) {
            double detectionTime =
                config.getDouble(CF_SIMULATED_DETECTION_TIME);
            SimulatedFailureDetector fd =
                new SimulatedFailureDetector(process, detectionTime);
            theFD = fd;
        } else if (config.getBoolean(CF_SIMULATED_WRONG_SUSP, false)) {
            double tMR = config.getDouble(CF_SIMULATED_T_MR);
            double tM = config.getDouble(CF_SIMULATED_T_M);
            SimulatedDetectorWithWrongSuspicions fd =
                new SimulatedDetectorWithWrongSuspicions(process,
                                                         group,
                                                         tMR, tM);
            theFD = fd;
        } else {
            theFD = new NoSuspicion();
        }
        final Object fdId = "fd";
        theFD.setId(fdId);
        theFD.launch();
        return theFD;
    }


    // FIXME : find a more elegant way (factory)

    public OmegaFailureDetectorInterface omegaInit(NekoProcess process,
                                                   Configurations config)
    {
        int[] group = new int[process.getN()];
        for (int i = 0; i < process.getN(); i++) {
            group[i] = i;
        }
        return omegaInit(process, config, group);
    }

    public OmegaFailureDetectorInterface omegaInit(NekoProcess process,
                                         Configurations config,
                                         int[] group)
    {
        OmegaFailureDetector ofd =
            new OmegaFailureDetector(process);
        final Object ofdId = "omega";
        ofd.setId(ofdId);
        FailureDetectorInterface fd =
            init(process, config, group);

        fd.setListener(ofd);
        ofd.setFailureDetector(fd);

        ofd.launch();

        return ofd;
    }
}
