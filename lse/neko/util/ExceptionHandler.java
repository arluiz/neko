package lse.neko.util;

/**
 * Provides a method to handle exceptions.
 * @see LauncherCatchingExceptions
 */
public interface ExceptionHandler {

    /**
     * Handles the exception e thrown by thread t.
     */
    void handle(Thread t, Throwable e);

}
