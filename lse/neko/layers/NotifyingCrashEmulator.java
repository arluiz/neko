package lse.neko.layers;

// lse.neko imports:
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.Protocol;
import lse.neko.ProtocolImpl;
import lse.neko.SenderInterface;
import lse.neko.failureDetectors.SimulatedFailureDetector;
// ambiguous with: lse.neko.layers.SimulatedFailureDetector

public class NotifyingCrashEmulator
    extends ProtocolImpl
    implements CrashEmulatorInterface
{
    protected final int[] others;
    protected final int me;

    private NekoProcess process;

    public NotifyingCrashEmulator(NekoProcess process) {
        this.process = process;
        int n = NekoSystem.instance().getProcessNum();
        me = process.getID();
        others = new int[n - 1];
        for (int i = 0; i < n - 1; i++) {
            others[i] = (i < me) ? i : i + 1;
        }
    }

    private SenderInterface controlSender;

    public void setControlSender(SenderInterface controlSender) {
        this.controlSender = controlSender;
    }

    private Object notifiedId;

    public void setNotifiedId(Object notifiedId) {
        this.notifiedId = notifiedId;
    }

    private CrashEmulatorInterface crashEmulator;

    public void setCrashEmulator(CrashEmulatorInterface crashEmulator) {
        this.crashEmulator = crashEmulator;
    }

    public void setListener(Protocol protocol) {
        // FIXME: this method should not be part of the interface
        throw new RuntimeException("Not implemented!");
    }

    public void crash() {
        crashEmulator.crash();
        notifyOfCrash(true);
    }

    public void recover() {
        crashEmulator.recover();
        notifyOfCrash(false);
    }

    public void finishRecover() {
        crashEmulator.finishRecover();
        // no notification needed
    }

    protected void notifyOfCrash(boolean isCrash) {
        NekoMessage m =
            new NekoMessage(others,
                            notifiedId,
                            new SimulatedFailureDetector.CrashContent
                            (isCrash, me, NekoSystem.instance().clock()),
                            SimulatedFailureDetector.CRASH);
        controlSender.send(m);
    }
}





