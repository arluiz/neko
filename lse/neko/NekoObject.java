package lse.neko;


/**
 * A class implementing NekoObjectInterface.
 */
public class NekoObject
    implements NekoObjectInterface
{
    private final NekoObjectInterface peerObject;

    public NekoObject() {
        // XXX: it might be worthwhile to create the peer object lazily.
        peerObject = NekoSystem.instance().createObject(this);
    }

    /*
     * Now comes the implementation of NekoObjectInterface.
     * All methods just delegate the actual task to the peer.
     */
    public void doNotify() {
        peerObject.doNotify();
    }

    public void doNotifyAll() {
        peerObject.doNotifyAll();
    }

    public void doWait() throws InterruptedException {
        peerObject.doWait();
    }

    public void doWait(double timeout) throws InterruptedException {
        peerObject.doWait(timeout);
    }

    /*
     * End of the implementation of NekoObjectInterface.
     */
}
