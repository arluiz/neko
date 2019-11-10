package lse.neko.layers;

// java imports:
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.GenericProtocolImpl;
import lse.neko.Protocol;
import lse.neko.util.logging.NekoLogger;


/**
 * This layer emulates a crash of its process. It should be placed
 * at the bottom of the protocol stack. When a process is crashed,
 * the send and deliver methods buffer all incoming and outgoing messages.
 * When the process recovers, these buffered messages are forwarded
 * (so a crash really means that the process is temporarily partitioned
 * away from the network).
 */
public class CrashEmulator
    extends GenericProtocolImpl
    implements CrashEmulatorInterface
{
    private boolean isCrashed = false;
    private Collection messages = new LinkedList();

    private Object lock = new Object();

    public static CrashEmulatorInterface newInstance(Class[] interfaces) {
        return (CrashEmulatorInterface)
            newProxyInstance(new CrashEmulator(), interfaces);
    }

    protected CrashEmulator() {
    }

    protected Protocol listener;

    public void setListener(Protocol listener) {
        this.listener = listener;
    }

    private static class Message {

        private Method method;
        private Object[] args;

        public Message(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }

        public Method getMethod() {
            return method;
        }

        public Object[] getArgs() {
            return args;
        }

    }

    public Object invoke(Method method, Object[] args) {
        synchronized (lock) {
            if (isCrashed) {
                messages.add(new Message(method, args));
                return null;
            }
        }
        return invokeMethod(listener, method, args);
    }

    /**
     * Starts an emulated crash. During an emulated crash, incoming
     * messages are buffered instead of being forwarded.
     */
    public void crash() {
        synchronized (lock) {
            if (isCrashed) {
                return;
            }
            logger.fine("crashing");
            isCrashed = true;
        }
    }

    /**
     * Starts the recovery from an emulated crash. Messages are no
     * longer buffered.
     */
    public void recover() {
        synchronized (lock) {
            if (!isCrashed) {
                return;
            }
            logger.fine("recovering");
            isCrashed = false;
        }
    }

    /**
     * Finishes the recovery from an emulated crash. Messages buffered
     * during the crash are forwarded.
     */
    public void finishRecover() {
        Collection c;
        synchronized (lock) {
            c = messages;
            messages = new LinkedList();
        }
        {
            Iterator it = c.iterator();
            while (it.hasNext()) {
                Message m = (Message) it.next();
                invokeMethod(listener, m.getMethod(), m.getArgs());
            }
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(CrashEmulator.class.getName());
}


