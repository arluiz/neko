package lse.neko;

// other imports:
import org.apache.java.util.Configurations;


public interface NekoProcessInitializer {

    void init(NekoProcess process, Configurations config)
        throws Exception;

}

