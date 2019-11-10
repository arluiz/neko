package lse.neko.layers;

// java imports:
import java.util.Iterator;


public interface MessageFromConsensusListener {

    void handleMessageFromConsensus(Iterator[] iterators);

}

