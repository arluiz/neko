package lse.neko;

// java imports:
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


public class MulticastProtocol
    extends GenericProtocolImpl
    implements MulticastProtocolInterface
{

    protected MulticastProtocol() {
    }

    public static MulticastProtocolInterface newInstance(Class[] interfaces) {
        return (MulticastProtocolInterface)
            newProxyInstance(new MulticastProtocol(), interfaces);
    }

    private Set listeners = new LinkedHashSet();

    // FIXME: check if the protocol is present

    public synchronized void addListener(Protocol protocol) {
        listeners.add(protocol);
    }

    public synchronized void removeListener(Protocol protocol) {
        listeners.remove(protocol);
    }

    public synchronized Object invoke(Method method, Object[] args) {
        // XXX: during invoke, addListener and removeListener may be called
        // by the same thread, yielding ConcurrentModificationException
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            Object listener = it.next();
            // call the method and throw away the return value
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,
                           "{0} calling listener {1} method {2} args {3}",
                           new Object[] {
                               this, listener, method, args
                           });
            }
            invokeMethod(listener, method, args);
        }
        return null;
    }

    private static final Logger logger =
        NekoLogger.getLogger(MulticastProtocol.class.getName());

}


