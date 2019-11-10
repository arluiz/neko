package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.Dispatcher;
import lse.neko.NekoProcess;
import lse.neko.ProcessReceiver;
import lse.neko.SenderInterface;

// other imports:
import org.apache.java.util.Configurations;


public class DefaultRingFailureDetectorInitializer
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
        int predecessor = (process.getID() - 1 + process.getN())
            % process.getN();

        FailureDetectorInterface theFD;

        Dispatcher dispatcher = process.getDispatcher();

        boolean useHeartbeat = config.getBoolean("heartbeat");
        if (useHeartbeat) {
            double send = config.getDouble("heartbeat.send");
            double timeout = config.getDouble("heartbeat.timeout");
            boolean useApplicationMessages =
                    config.getBoolean("heartbeat.useApplicationMessages");
            //boolean measureQoS =
            //        config.getBoolean("heartbeat.measureQoS");
            int        index = config.getInteger("heartbeat.network.index");
            SenderInterface failureDetectionNetwork =
                process.getNetworks()[index];
            RingHeartbeat ringH = new RingHeartbeat(process, send, timeout);
            ringH.setSender(failureDetectionNetwork);
            ringH.useApplicationMessages(useApplicationMessages);
            ringH.setReceiver(dispatcher);

            // make sure that incoming messages from all networks
            // go through the failure detector
            ProcessReceiver[] nets = process.getReceivers();
            for (int i = 0; i < nets.length; i++) {
                nets[i].setReceiver(ringH);
            }

            theFD = ringH;
        } else if (config.getBoolean(CF_SIMULATED, false)) {
            double detectionTime =
                config.getDouble(CF_SIMULATED_DETECTION_TIME);
            SimulatedRingFailureDetector fd =
                new SimulatedRingFailureDetector(process, detectionTime);
            theFD = fd;
        /*else if (config.getBoolean(CF_SIMULATED_WRONG_SUSP, false)) {
            double tMR = config.getDouble(CF_SIMULATED_T_MR);
            double tM = config.getDouble(CF_SIMULATED_T_M);
            SimulatedDetectorWithWrongSuspicions fd =
                new SimulatedDetectorWithWrongSuspicions(process,
                                                         group,
                                                         tMR, tM);
            process.addLayer(fd);
            theFD = fd;
        } */
        } else {
            theFD = new NoSuspicion();
        }
        return theFD;
    }

}
