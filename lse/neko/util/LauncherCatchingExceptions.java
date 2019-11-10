package lse.neko.util;

/**
 * Executes a Runnable object such that all the object's exceptions
 * (not caught by the object) are handled by an exception handler.
 * This also holds for threads created by the object.
 * <br>
 * Main use: create an instance of this class in your <code>main</code>,
 * write and register an exception handler, then pass a Runnable object
 * that executes your application.
 * All the uncaught exceptions of your application will be handled by
 * the exception handler.
 */
public class LauncherCatchingExceptions {

    /**
     * Launches the runnable object and associates
     * it with the exception handler.
     */
    public LauncherCatchingExceptions(Runnable runnable,
                                      ExceptionHandler handler)
    {
        this.handler = handler;
        MyThreadGroup threadGroup = new MyThreadGroup("launcher");
        Thread thread = new Thread(threadGroup, runnable, "launcher");
        thread.start();
    }

    private ExceptionHandler handler;
    private Object lock = new Object();

    /**
     * Sets a new exception handler.
     */
    public void setHandler(ExceptionHandler handler) {
        synchronized (lock) {
            this.handler = handler;
        }
    }

    /**
     * Returns the exception handler.
     * This method is final for security.
     */
    public final ExceptionHandler getHandler() {
        synchronized (lock) {
            return handler;
        }
    }

    private class MyThreadGroup
        extends ThreadGroup
    {
        MyThreadGroup(String name) {
            super(name);
        }

        public void uncaughtException(Thread t, Throwable e) {
            getHandler().handle(t, e);
        }
    }

    /**
     * A unit test.
     */
    public static void main(String[] args) {
        ExceptionHandler handler =
            new ExceptionHandler() {
                public void handle(Thread t, Throwable e) {
                    System.out.println("Test successful");
                    System.exit(0);
                }
            };
        Runnable runnable =
            new Runnable() {
                public void run() {
                    throw new RuntimeException();
                }
            };
        try {
            new LauncherCatchingExceptions(runnable, handler);
        } catch (Exception ex) {
            System.out.println("Test failed");
            System.exit(1);
        }
    }

}
