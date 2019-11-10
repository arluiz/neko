package lse.neko.comm;

// java imports:
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public interface SerializerFactory {

    Serializer createSerializer(OutputStream osUnderlying)
        throws IOException;

    Deserializer createDeserializer(InputStream isUnderLying)
        throws IOException;

}

