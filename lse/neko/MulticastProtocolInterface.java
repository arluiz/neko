package lse.neko;

// java imports:
import java.lang.reflect.InvocationHandler;

public interface MulticastProtocolInterface
    extends Protocol, InvocationHandler
{
    // XXX: Maybe InvocationHandler should not be exposed
    // and should go only to the implementation class

    void addListener(Protocol protocol);

    void removeListener(Protocol protocol);

}
