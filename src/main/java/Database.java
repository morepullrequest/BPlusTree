import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Database {
    public static BPlusTree tree;

    static {

        try {
            Config.init();
            tree = BPlusTree.buildTreeFromJson(Config.indexFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] read(byte[] key) {
        return tree.get(Main.bytes2Long(key));
    }

    public void write(byte[] key, byte[] value) {
        tree.insertOrUpdate(Main.bytes2Long(key), value);
    }

    public static final int FOUR_KB = 4096;

    public static void main(String[] args) {
        Database db = new Database();
        Random random = new Random();


        Map<byte[], byte[]> m = new HashMap<byte[], byte[]>();
        for (int i = 0; i < 1000; i++) {
            long keyLong = random.nextLong();
            byte[] keyByte = Main.long2Bytes(keyLong);
            ByteBuffer bf = ByteBuffer.allocate(FOUR_KB);
            for (int j = 0; j < FOUR_KB / 16; j++) {
                bf.putLong(random.nextLong());
            }
            byte[] valueByte = bf.array();

            m.put(keyByte, valueByte);

            db.write(keyByte, valueByte);

        }
        tree.save();

        for (byte[] key : m.keySet()) {
            byte[] value1 = m.get(key);
            byte[] value2 = db.read(key);
            if (!value1.equals(value2)) {
                System.out.println("error");
            }
        }
    }


}
