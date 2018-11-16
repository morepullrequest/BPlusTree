import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.ByteBuffer;

public class Util {
    private static final int eightBytes = 8;
    public static final int FOUR_KB = 4096;
    public static final Gson gson = intiGson();
    ;


    public static byte[] long2Bytes(long l) {
        ByteBuffer buffer = ByteBuffer.allocate(eightBytes);
        buffer.putLong(0, l);
        return buffer.array();
    }

    public static long bytes2Long(byte[] b) {
        ByteBuffer buffer = ByteBuffer.allocate(eightBytes);
        buffer.put(b, 0, eightBytes);
        buffer.flip();
        return buffer.getLong();
    }

    private static Gson intiGson() {
        RuntimeTypeAdapterFactory<Node> factory = RuntimeTypeAdapterFactory.of(Node.class);
        factory.registerSubtype(InternalNodeImpl.class, "previous");
        factory.registerSubtype(LeafNodeImpl.class, "children");
        return new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(factory)
                .create();
    }
}
