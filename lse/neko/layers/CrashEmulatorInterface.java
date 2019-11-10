package lse.neko.layers;

// lse.neko imports:
import lse.neko.Protocol;


public interface CrashEmulatorInterface
    extends Protocol
{

    void setListener(Protocol listener);

    void crash();

    void recover();

    void finishRecover();

}
