package lse.neko.examples.basic;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.layers.NoMulticastLayer;

// other imports:
import org.apache.java.util.Configurations;


/**
 * This class initalizes the protocol stack of a process
 * for the basic example.
 * Its <code>init</code> method adds two layers on top of the
 * NekoProcess:
 * <ul><li>(top layer) An algorithm. The implementing
 *   class is specified by the <code>algorithm</code>
 *   config option.</li>
 * <li>(bottom layer) A layer that transforms outgoing multicast
 *   messages into several unicast messages (one per destination).</li>
 * </ul>
 */
public class TestInitializer
    implements NekoProcessInitializer
{

    public TestInitializer() {
    }

    public void init(NekoProcess process, Configurations config)
        throws Exception
    {
        // gets the name of the class implementing the algorithm
        Class algorithmClass =
            Class.forName(config.getString("algorithm"));

        // Using reflection to create an instance of the algorithm
        // class. If the algorithm is Lamport, the effect is the same
        // as having
        // ReceiverInterface algorithm = new Lamport(process);
        Class[] constructorParamClasses = { NekoProcess.class };
        Object[] constructorParams = { process };
        ReceiverInterface algorithm = (ReceiverInterface)
            algorithmClass
                .getConstructor(constructorParamClasses)
                .newInstance(constructorParams);
        algorithm.setId("alg");

        NoMulticastLayer nm = new NoMulticastLayer();
        nm.setId("nm");

        SenderInterface net = process.getDefaultNetwork();

        // algorithm.setSender(nm) is called using reflection
        algorithmClass
            .getMethod("setSender", new Class[] { SenderInterface.class })
            .invoke(algorithm, new Object[] { nm });

        nm.setSender(net);

        algorithm.launch();
        nm.launch();
    }

}
