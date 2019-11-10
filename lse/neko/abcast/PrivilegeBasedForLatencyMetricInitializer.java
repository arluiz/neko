package lse.neko.abcast;

// java imports:
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.util.logging.NekoLogger;


public abstract class PrivilegeBasedForLatencyMetricInitializer
    implements ABCastInitializer
{

    protected abstract ABCastInitializer createDelegate();

    private ABCastInitializer delegate = createDelegate();

    public void createDeliverer(NekoProcess process) {
        createSender(process);
    }

    private TokenInterceptor ti;

    public SenderInterface createSender(NekoProcess process) {

        ti = new TokenInterceptor(process);
        final Object tiId = "tokenInterceptor";
        ti.setId(tiId);

        // XXX: only works if anly the abcast protocol sends messages
        SenderInterface[] nets = process.getNetworks();
        ti.setSender(nets[0]);
        nets[0] = ti;
        process.setNetworks(nets);

        abcast = (PrivilegeBasedNU) delegate.createSender(process);
        ti.setAbcast(abcast);

        Wrapper wrapper = new Wrapper(abcast, ti);
        final Object wrapperId = "wrapper";
        wrapper.setId(wrapperId);

        ti.launch();
        wrapper.launch();
        return wrapper;
    }

    public SenderInterface createSenderDeliverer(NekoProcess process) {
        return createSender(process);
    }

    private PrivilegeBasedNU abcast;

    public void setReceiver(ReceiverInterface receiver) {
        abcast.setReceiver(receiver);
    }

    private static class TokenInterceptor
        extends ProtocolImpl
        implements ReceiverInterface, SenderInterface
    {
        private static boolean doKillToken = true;
        private PrivilegeBasedNU abcast;
        private int tokenType;
        private static PrivilegeBasedNU.Content token;

        private static int countDown;
        private static final int NUM_PASS = 4;

        private NekoProcess process;

        public TokenInterceptor(NekoProcess process) {
            this.process = process;
        }

        private SenderInterface sender;

        public void setSender(SenderInterface sender) {
            this.sender = sender;
        }

        public void setAbcast(PrivilegeBasedNU abcast) {
            this.abcast = abcast;
            this.tokenType = abcast.getMessageType();
        }

        public void generateToken() {
            logger.fine("calling generateToken");
            doKillToken = false;
            if (token == null) {
                throw new RuntimeException("Could not yet store the token. "
                                           + "Wait more after killToken()");
            }
            Object content = abcast.createContent(null,
                                                  token.getSeqNum(),
                                                  process.getID());
            NekoMessage m =
                new NekoMessage(new int[]{ process.getID() },
                                getId(),
                                content,
                                tokenType);
            token = null;
            countDown = NUM_PASS * process.getN();
            abcast.deliver(m);
        }

        public void deliver(NekoMessage m) {
            logger.log(Level.FINE, "delivering {0}", m);
            if (m.getType() == tokenType) {
                logger.log(Level.FINE, "countDown {0}",
                           new Integer(countDown));
                countDown--;
                if (countDown <= 0) {
                    doKillToken = true;
                }
                if (doKillToken) {
                    if (token != null) {
                        throw new RuntimeException("received a second token");
                    }
                    token = (PrivilegeBasedNU.Content) m.getContent();
                    return;
                }
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "delivering {0} to {1}",
                           new Object[] { m, abcast });
            }
            abcast.deliver(m);
        }

        public void send(NekoMessage m) {
            // only changing the protocol ID
            // so that the deliver method can intercept this message
            NekoMessage newM = new NekoMessage(m.getSource(),
                                               m.getDestinations(),
                                               getId(),
                                               m.getContent(),
                                               m.getType());
            sender.send(newM);
        }

    }

    private static class Wrapper
        extends ProtocolImpl
        implements SenderInterface
    {
        private final SenderInterface abcast;
        private final TokenInterceptor ti;

        public Wrapper(SenderInterface abcast, TokenInterceptor ti) {
            this.abcast = abcast;
            this.ti = ti;
        }

        public void send(NekoMessage m) {
            abcast.send(m);
            ti.generateToken();
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(PrivilegeBasedForLatencyMetricInitializer
                             .class.getName());

} // end class PrivilegeBasedNUInitializer
