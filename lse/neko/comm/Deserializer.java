package lse.neko.comm;

// java imports:
import java.io.IOException;

// lse.neko imports:
import lse.neko.NekoMessage;


public interface Deserializer {

    NekoMessage readMessage()
        throws IOException, ClassNotFoundException;

}










