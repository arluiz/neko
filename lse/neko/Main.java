package lse.neko;

// lse.neko imports:
import lse.neko.comm.Execution;
import lse.neko.comm.Master;
import lse.neko.sim.Simulation;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Starts a Neko application, configured by the file supplied on the
 * command line.
 */
public class Main
    implements Runnable
{
    private static final String CF_SIMULATION = "simulation";
    private static final String CF_STARTUP_SYMMETRIC = "startup.symmetric";

    public static void main(String[] args) {
        Configurations config = NekoInitializer.getConfig(args);
        NekoInitializer.initLog(config, new Main(config));
    }

    private Configurations config;

    public Main(Configurations config) {
        this.config = config;
    }

    public void run() {
        boolean isSimulation = config.getBoolean(CF_SIMULATION);
        if (isSimulation) {
            new Simulation(config);
        } else if (config.getBoolean(CF_STARTUP_SYMMETRIC, false)) {
            new Execution(config);
        } else {
            new Master(config);
        }
    }
}
