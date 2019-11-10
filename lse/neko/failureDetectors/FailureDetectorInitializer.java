package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.NekoProcess;

// other imports:
import org.apache.java.util.Configurations;


public interface FailureDetectorInitializer {

    /**
     * Adds layers to process that implement a failure detector.
     * Returns the object that implements FailureDetectorInterface.
     */
    FailureDetectorInterface init(NekoProcess process,
                                  Configurations config);

}

