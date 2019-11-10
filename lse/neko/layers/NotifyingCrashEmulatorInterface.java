package lse.neko.layers;

// lse.neko imports:
import lse.neko.SenderInterface;


public interface NotifyingCrashEmulatorInterface
    extends CrashEmulatorInterface
{

    void setControlSender(SenderInterface controlSender);

    void setNotifiedId(Object notifiedId);

}
