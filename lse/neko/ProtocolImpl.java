package lse.neko;

/**
 * Base class for microprotocols, that is, classes implementing {@link
 * Protocol}.
 *
 * <p>
 *
 * If extending this class is not feasible, you can easily implement
 * the <code>Protocol</code> interface by calling the static methods
 * of this class.
 */
public class ProtocolImpl
    implements Protocol
{

    private Object id;

    public void setId(Object id) {
        if (this.id != null) {
            // setId has been called already
            // we expect that setId is only called once before launch,
            // hence this implies that launch has been called already
            // and we should have the protocol in the dispatcher
            NekoProcess process = NekoThread.currentThread().getProcess();
            if (process != null) {
                process.getDispatcher().removeProtocol(this.id);
                if (id != null) {
                    process.getDispatcher().putProtocol(id, this);
                }
            }
        }
        this.id = id;
    }

    public Object getId() {
        return id;
    }

    public String toString() {
        return getClass().getName() + ":" + id.toString();
    }

    public void launch() {
        staticLaunch(this);
    }

    /**
     * The actual implementation for <code>launch</code>.
     */
    public static void staticLaunch(Protocol protocol) {
        Object id = protocol.getId();
        if (id == null) {
            throw new RuntimeException("call setId before launch!");
        }
        NekoProcess process = NekoThread.currentThread().getProcess();
        if (process != null) {
            process.getDispatcher().putProtocol(id, protocol);
        }
    }

}
