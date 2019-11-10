package lse.neko.comm;

// java imports:
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class JavaSerializerFactory implements SerializerFactory {

    public Serializer createSerializer(OutputStream osUnderlying)
        throws IOException
    {
        return new JavaSerializer(osUnderlying);
    }

    public Deserializer createDeserializer(InputStream isUnderlying)
        throws IOException
    {
        return new JavaDeserializer(isUnderlying);
    }

}

