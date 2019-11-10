package lse.neko.util.logging;

// java imports:
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * Used to log messages for a specific system or application component.
 * All Neko code should use this method rather than
 * <code>java.util.logging.Logger</code>.
 * The difference is that Neko fills in the current Neko time
 * (a <code>double</code>, rather than a <code>long</code>)
 * into the LogRecord when the LogRecord is logged.
 * This works with both real time (for distributed executions)
 * and simulated time (for simulations).
 */
public class NekoLogger
    extends Logger
{

    protected NekoLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    /*
     * The following two methods are nearly exact copies of Logger methods,
     * except that "new Logger" is replaced by "new NekoLogger".
     * It's a pity that Sun didn't provide a factory method.
     */

    public static synchronized Logger getLogger(String name) {
        LogManager manager = LogManager.getLogManager();
        Logger result = manager.getLogger(name);
        if (result == null) {
            result = new NekoLogger(name, null);
            manager.addLogger(result);
            result = manager.getLogger(name);
        }
        return result;
    }

    public static synchronized Logger getLogger(String name,
                                                String resourceBundleName)
    {
        LogManager manager = LogManager.getLogManager();
        Logger result = manager.getLogger(name);
        if (result == null) {
            // Create a new logger.
            // Note: we may get a MissingResourceException here.
            result = new NekoLogger(name, resourceBundleName);
            manager.addLogger(result);
            result = manager.getLogger(name);
        }
        if (result.getResourceBundleName() == null) {
            // Note: we may get a MissingResourceException here.
            // check already done by the Logger constructor
            //result.setupResourceInfo(resourceBundleName);
        } else if (!result.getResourceBundleName().equals(resourceBundleName)) {
            throw new IllegalArgumentException(result.getResourceBundleName()
                                               + " != " + resourceBundleName);
        }
        return result;
    }

    /*
     * The following two methods cannot be made to work.
     */

    private static final String DO_NOT_CALL =
        "Do not call NekoLogger.getAnonymousLogger()!";

    public static synchronized Logger getAnonymousLogger() {
        throw new RuntimeException(DO_NOT_CALL);
    }

    public static synchronized Logger
    getAnonymousLogger(String resourceBundleName)
    {
        throw new RuntimeException(DO_NOT_CALL);
    }

    /*
     * Start of overridden convenience methods.
     * They only have to be overridden because Sun didn't provide
     * a LogRecord factory.
     */

    private ResourceBundle myCatalog;     // Cached resource bundle
    private String myCatalogName;         // name associated with catalog
    private Locale myCatalogLocale;       // locale associated with catalog

    private synchronized ResourceBundle myFindResourceBundle(String name) {
        // Return a null bundle for a null name.
        if (name == null) {
            return null;
        }

        Locale currentLocale = Locale.getDefault();

        // Normally we should hit on our simple one entry cache.
        if (myCatalog != null && currentLocale == myCatalogLocale
            && name == myCatalogName)
        {
            return myCatalog;
        }

        // Use the thread's context ClassLoader.  If there isn't one,
        // use the SystemClassloader.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            myCatalog = ResourceBundle.getBundle(name, currentLocale, cl);
            myCatalogName = name;
            myCatalogLocale = currentLocale;
            return myCatalog;
        } catch (MissingResourceException ex) {
            // Woops.  We can't find the ResourceBundle in the default
            // ClassLoader.  Drop through.
        }


        // Fall back to searching up the call stack and trying each
        // calling ClassLoader.
        for (int ix = 0; true; ix++) {
            Class clz = sun.reflect.Reflection.getCallerClass(ix);
            if (clz == null) {
                break;
            }
            ClassLoader cl2 = clz.getClassLoader();
            if (cl2 == null) {
                cl2 = ClassLoader.getSystemClassLoader();
            }
            if (cl == cl2) {
                // We've already checked this classloader.
                continue;
            }
            cl = cl2;
            try {
                myCatalog = ResourceBundle.getBundle(name, currentLocale, cl);
                myCatalogName = name;
                myCatalogLocale = currentLocale;
                return myCatalog;
            } catch (MissingResourceException ex) {
                // Ok, this one didn't work either.
                // Drop through, and try the next one.
            }
        }

        if (name.equals(myCatalogName)) {
            // Return the previous cached value for that name.
            // This may be null.
            return myCatalog;
        }
        // Sorry, we're out of luck.
        return null;
    }

    private String myGetEffectiveResourceBundleName() {
        Logger target = this;
        while (target != null) {
            String rbn = target.getResourceBundleName();
            if (rbn != null) {
                return rbn;
            }
            target = target.getParent();
        }
        return null;
    }

    private void myDoLog(LogRecord lr) {
        lr.setLoggerName(getName());
        String ebname = myGetEffectiveResourceBundleName();
        if (ebname != null) {
            lr.setResourceBundleName(ebname);
            lr.setResourceBundle(myFindResourceBundle(ebname));
        }
        log(lr);
    }

    public void log(Level level, String msg) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        myDoLog(lr);
    }

    public void log(Level level, String msg, Object param1) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        Object[] params = { param1 };
        lr.setParameters(params);
        myDoLog(lr);
    }

    public void log(Level level, String msg, Object[] params) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setParameters(params);
        myDoLog(lr);
    }

    public void log(Level level, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setThrown(thrown);
        myDoLog(lr);
    }

    public void logp(Level level, String sourceClass,
                     String sourceMethod, String msg)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        myDoLog(lr);
    }

    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Object param1)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object[] params = { param1 };
        lr.setParameters(params);
        myDoLog(lr);
    }

    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Object[] params)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        myDoLog(lr);
    }

    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Throwable thrown)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        myDoLog(lr);
    }

    private void myDoLog(LogRecord lr, String rbname) {
        lr.setLoggerName(getName());
        if (rbname != null) {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(myFindResourceBundle(rbname));
        }
        log(lr);
    }

    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        myDoLog(lr, bundleName);
    }

    public void logrb(Level level, String sourceClass, String sourceMethod,
                                String bundleName, String msg, Object param1)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object[] params = { param1 };
        lr.setParameters(params);
        myDoLog(lr, bundleName);
    }

    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Object[] params)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        myDoLog(lr, bundleName);
    }

    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Throwable thrown)
    {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        myDoLog(lr, bundleName);
    }

    public void throwing(String sourceClass, String sourceMethod,
                         Throwable thrown)
    {
        if (!isLoggable(Level.FINER)) {
            return;
        }
        LogRecord lr = new NekoLogRecord(Level.FINER, "THROW");
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        myDoLog(lr);
    }

}
