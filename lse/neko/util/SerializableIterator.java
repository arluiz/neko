package lse.neko.util;

// java imports:
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

// lse.neko imports:
import lse.neko.NekoProcess;
import lse.neko.NekoThread;
import lse.neko.layers.ListFragmenter;


public class SerializableIterator
    implements Serializable, Iterator
{

    private Object id;
    private transient Iterator peer;
    private transient List list;

    private Object fragmenterId;

    // called by ListFragmenter.sendIterator
    public void setFragmenterId(Object fragmenterId) {
        this.fragmenterId = fragmenterId;
    }

    private void lookupPeer() {
        if (peer == null) {
            // Iterator constructed by deserialization.
            // Look up the iterator by the ID.
            NekoProcess process = NekoThread.currentThread().getProcess();
            ListFragmenter fragmenter =
                (ListFragmenter)
                process.getDispatcher().getProtocol(fragmenterId);
            peer = fragmenter.getIterator(id);
        }
    }

    public boolean hasNext() {
        lookupPeer();
        return peer.hasNext();
    }

    public Object next() {
        lookupPeer();
        return peer.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public SerializableIterator(List list) {
        id = new GUID();
        if (list == null) {
            throw new NullPointerException();
        }
        this.list = list;
    }

    // only used by ListFragmenter

    public List getList() {
        return list;
    }

    public Object getId() {
        return id;
    }

    public String toString() {
        return "SerializableIterator[id=" + id + "]";
    }
}
