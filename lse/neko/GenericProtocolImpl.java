package lse.neko;

// java imports:
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.util.logging.NekoLogger;


public abstract class GenericProtocolImpl
    extends ProtocolImpl
    implements GenericProtocol
{

    public Object invoke(Object proxy, Method method, Object[] args) {

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "invoke method {0} args {1}",
                       new Object[] { method, args });
        }

        // check if the object has a matching method

        // the following code should do more checks,
        // e.g., on the return type,
        // and the result should be cached.
        boolean found;
        try {
            getClass().getMethod(method.getName(),
                                 method.getParameterTypes());
            found = true;
        } catch (NoSuchMethodException ex) {
            found = false;
        }
        logger.log(Level.FINE, "found {0}", Boolean.valueOf(found));

        // call the specific or the generic method
        if (found) {
            return invokeMethod(this, method, args);
        } else {
            return invoke(method, args);
        }
    }

    /**
     * Calls the method on the specified object with the specified
     * arguments, but, unlike Method.invoke, throws no checked
     * exception.  Checked exceptions are wrapped in an
     * UndeclaredThrowableException.
     */
    protected static Object invokeMethod(Object object,
                                         Method method,
                                         Object[] args)
    {
        try {
            return method.invoke(object, args);
        } catch (IllegalAccessException ex) {
            Error error =
                new IllegalAccessError("access error in generic method");
            error.initCause(ex);
            throw error;
        } catch (InvocationTargetException ex) {
            // unwrap the exception thrown by the called method
            Throwable cause = ex.getCause();
            // throw it,
            // wrapped into an unchecked exception if necessary
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new UndeclaredThrowableException(cause);
            }
        }
    }

    protected static Object newProxyInstance(InvocationHandler newInstance,
                                             Class[] interfaces)
    {
        Class[] thisInterfaces = newInstance.getClass().getInterfaces();
        logger.log(Level.FINE, "thisInterfaces {0}", (Object) thisInterfaces);

        // add thisInterfaces to interfaces if necessary
        Class[] oldInterfaces = (Class[]) interfaces.clone();
        logger.log(Level.FINE, "oldInterfaces {0}", (Object) oldInterfaces);

        Class[] notFound = new Class[thisInterfaces.length];
        int numNotFound = 0;
        for (int i = 0; i < thisInterfaces.length; i++) {
            Class thisInterface = thisInterfaces[i];
            boolean isFound = false;
            for (int j = 0; j < oldInterfaces.length; j++) {
                if (thisInterface == oldInterfaces[j]) {
                    isFound = true;
                    break;
                }
            }
            notFound[numNotFound] = thisInterface;
            numNotFound++;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "numNotFound {0} notFound {1}",
                       new Object[] { new Integer(numNotFound), notFound });
        }

        Class[] newInterfaces =
            new Class[oldInterfaces.length + numNotFound];
        System.arraycopy(oldInterfaces, 0, newInterfaces, 0,
                         oldInterfaces.length);
        System.arraycopy(notFound, 0, newInterfaces, oldInterfaces.length,
                         numNotFound);
        logger.log(Level.FINE, "newInterfaces {0}", (Object) newInterfaces);

        return Proxy.newProxyInstance(newInstance.getClass().getClassLoader(),
                                      newInterfaces,
                                      newInstance);
    }

    private static final Logger logger =
        NekoLogger.getLogger(GenericProtocolImpl.class.getName());

}

