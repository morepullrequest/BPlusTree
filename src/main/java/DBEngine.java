import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DBEngine {
    public static Tree tree;

    static {

        try {
            Config.init();
            tree = Tree.buildTreeFromJson(Config.indexFilename);
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

    public static void main(String[] args) {
        File file = new File(Config.indexFilename);
        file.delete();
        DBEngine db = new DBEngine();
        Random random = new Random();

        Map<Long, byte[]> m = new HashMap<Long, byte[]>();
        for (int i = 0; i < 1000; i++) {
            long keyLong = random.nextLong();

            byte[] keyByte = Main.long2Bytes(keyLong);

            ByteBuffer bf = ByteBuffer.allocate(Util.FOUR_KB);
            for (int j = 0; j < Util.FOUR_KB / 16; j++) {
                bf.putLong(random.nextLong());
            }
            byte[] valueByte = bf.array();

            m.put(keyLong, valueByte);

            db.write(keyByte, valueByte);
        }
        tree.save();

        for (long key : m.keySet()) {
            byte[] value1 = m.get(key);
            byte[] value2 = db.read(Util.long2Bytes(key));

            long value1Long = Util.bytes2Long(value1);
            long value2Long = Util.bytes2Long(value2);
            if (value1Long != value2Long) {

                System.out.println("error " + value1Long + " - " + value2Long);
            }
        }
    }
}
