package lse.neko;

// java imports:
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public interface GenericProtocol
    extends Protocol, InvocationHandler
{

    Object invoke(Method method, Object[] args);

}

