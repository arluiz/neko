package lse.neko.layers;

// java imports:
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// lse.neko imports:
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.NekoThread;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.util.SerializableIterator;

// other imports:
import org.apache.java.util.Configurations;


public class ListFragmenterTestInitializer
    implements NekoProcessInitializer
{

    public void init(NekoProcess process, Configurations config) {

        SenderInterface net = process.getDefaultNetwork();

        final int fragmentSize = 100;
        final int windowSize = 10;
        final int bufferSize = 1000;
        ListFragmenter frag =
            new ListFragmenter(fragmentSize, windowSize, bufferSize);
        String fragmenterId = "fragmenter";
        frag.setId(fragmenterId);
        frag.setSender(net);
        frag.launch();

        if (process.getN() != 2) {
            throw new RuntimeException("This is a test for two processes");
        }

        final int listSize = 10009;

        String id = "test";
        if (process.getID() == 0) {
            Receiver receiver = new Receiver(process, listSize);
            receiver.setId(id);
            receiver.launch();
        } else {
            Sender sender = new Sender(listSize);
            sender.setId(id);
            sender.setSender(net);
            sender.setFragmenter(frag);
            sender.launch();
        }
    }

    public static final int LIST = 1119;
    static {
        MessageTypes.instance().register(LIST, "LIST");
    }

    public static class Sender
        extends ProtocolImpl
        implements Runnable
    {
        public Sender(int listSize) {
            this.listSize = listSize;
        }

        private SenderInterface sender;

        public void setSender(SenderInterface sender) {
            this.sender = sender;
        }

        private ListFragmenter fragmenter;

        public void setFragmenter(ListFragmenter fragmenter) {
            this.fragmenter = fragmenter;
        }

        public void launch() {
            super.launch();
            new NekoThread(this).start();
        }

        private int listSize;

        public void run() {
            // create and fill the list
            List list = new ArrayList(listSize);
            for (int i = 0; i < listSize; i++) {
                list.add(new Integer(i));
            }

            SerializableIterator it = new SerializableIterator(list);
            NekoMessage m = new NekoMessage(new int[] { 0 },
                                            getId(),
                                            it,
                                            LIST);
            sender.send(m);
            // send the list
            fragmenter.sendIterator(it, new int[] { 0 });
            //System.out.println("Sender finished");
        }

    }

    public static class Receiver
        extends ProtocolImpl
        implements ReceiverInterface
    {
        private NekoProcess process;

        public Receiver(NekoProcess process, int listSize) {
            this.process = process;
            this.listSize = listSize;
        }

        private int listSize;

        public void deliverList(Iterator it) {
            int i = 0;
            try {
                while (it.hasNext()) {
                    int value = ((Integer) it.next()).intValue();
                    if (value != i) {
                        throw new RuntimeException("Element #" + i
                                                   + " is " + value
                                                   + ", not " + i);
                    }
                    i++;
                }
            } catch (IndexOutOfBoundsException ex) {
                if (i != listSize) {
                    throw new RuntimeException("The size of the list is "
                                               + i + ", not "
                                               + listSize);
                }
            }
        }

        public class MyThread
            extends NekoThread
        {
            public MyThread(Iterator it) {
                super("deliverer");
                this.it = it;
                start();
            }

            private Iterator it;

            public void run() {
                deliverList(it);
                System.out.println("Test successful");
                Receiver.this.process.shutdown();
            }
        }

        public void deliver(NekoMessage m) {
            if (m.getType() == LIST) {
                Iterator it = (Iterator) m.getContent();
                new MyThread(it);
                return;
            }
            throw new UnexpectedMessageException(m);
        }
    }

}
