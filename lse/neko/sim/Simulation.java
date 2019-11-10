package lse.neko.sim;

// lse.neko imports:
import lse.neko.NekoSystem;

// other imports:
import org.apache.java.util.Configurations;


/**
 * Starts up a simulation.
 */
public class Simulation {

    public Simulation(Configurations config) {
        createSystem(config).run();
    }

    /**
     * Config file entry that specifies the simulation
     * engine to be run. It contains the package name below
     * lse.neko.sim, or the name of the NekoSimSystem class.
     */
    public static final String CF_ENGINE = "simulation.engine";
    public static final String CF_ENGINE_PREFIX = "lse.neko.sim.";
    public static final String CF_ENGINE_POSTFIX = ".NekoSimSystem";

    /**
     * CF_ENGINE if not specified in the config file.
     */
    public static final String CF_DEFAULT_ENGINE = "nekosim";

    protected NekoSystem createSystem(Configurations config) {

        String engineName =
            config.getString(CF_ENGINE, CF_DEFAULT_ENGINE);
        Class engineClass;
        try {
            try {
                engineClass = Class.forName(CF_ENGINE_PREFIX
                                            + engineName
                                            + CF_ENGINE_POSTFIX);
            } catch (ClassNotFoundException ex) {
                engineClass = Class.forName(engineName);
            }
            Class[] constructorParamClasses = {
                Configurations.class
            };
            Object[] constructorParams = {
                config
            };

            return (NekoSystem) engineClass
                .getConstructor(constructorParamClasses)
                .newInstance(constructorParams);

        } catch (Exception ex) {
            throw new RuntimeException("The config entry " + CF_ENGINE
                                       + " is invalid!", ex);
        }
    }

}
