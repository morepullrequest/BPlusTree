import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        int order = 5;
        int size = 30;
//        testInsert(order,size);
        // testLB();
//        testRandomInsert(order, size);
//        testBug(order, size);
//        randomTest(order);

        File file = new File("data/1.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static int eightBytes = 8;

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

    public static void testInsert(int order, int size) {
        BPlusTree tree = new BPlusTree(order);
        for (int i = 0; i < size; i++) {
            tree.insertOrUpdate((long) i, long2Bytes((long) i));
            tree.walk();
        }
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();

        gson.toJson(tree, System.out);
    }

    public static void testLB() {
        Random random = new Random();
        for (int i = 0; i < 23; i++) {
            long a = random.nextLong();
            byte[] b = long2Bytes(a);

            long c = bytes2Long(b);
            System.out.println("a: " + a + ", b: " + b + ", c: " + c);
        }
    }

    public static void testRandomInsert(int order, int size) {
        BPlusTree tree = new BPlusTree(order);
        Random random = new Random();
        Set<Long> longSet = new HashSet<Long>();
        long randomNum = random.nextLong();
        for (int i = 0; i < size; i++) {
            randomNum = random.nextInt(100);
            System.out.println("inserting " + randomNum);
            tree.insertOrUpdate(randomNum, long2Bytes(randomNum));
            longSet.add(new Long(randomNum));
            tree.walk();
        }


        for (Long k : longSet) {
            long key = k.longValue();
            byte[] valueByte = tree.get(key);
            long valueLong = bytes2Long(valueByte);
            if (key != valueLong) {
                System.out.println("key: " + key + ", value: " + valueByte + ", long: " + valueLong);
            }
        }

        for (Long k : longSet) {
            long key = k.longValue();
            tree.walk();
            System.out.println("removing " + key);
            tree.remove(key);
        }

    }

    public static void testBug(int order, int size) {
        long[] a = {6, 13, 16, 17, 18, 19, 20, 23, 24, 25, 31, 44, 45, 54, 60, 65, 66, 80, 85, 89, 97, 99};
        BPlusTree tree = new BPlusTree(order);
        for (long b : a) {
            tree.insertOrUpdate(b, long2Bytes(b));
        }
        tree.walk();

        for (long key : a) {
            System.out.println("removing " + key);
            tree.remove(key);
            tree.walk();
        }

    }


    public static void randomTest(int order) {
        Set<Long> keys = new HashSet<Long>();
        BPlusTree tree = new BPlusTree(order);
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int op = random.nextInt(3);
            long insertKey = 0;
            long searchKey = 0;
            switch (op) {
                case 0:
                    for (int j = 0; j < 5; j++) {
                        insertKey = (long) random.nextInt(100);
                        tree.insertOrUpdate(insertKey, long2Bytes(insertKey));
                        keys.add(insertKey);
                        System.out.println("insert key: " + insertKey);
                        tree.walk();
                    }
                    break;
                case 1:
                    // search
                    System.out.println("searching");
                    for (long k : keys) {

                        byte[] valueByte = tree.get(k);
                        if (valueByte == null) {
                            tree.walk();
                            System.out.println("search " + k + " null");
                            break;
                        }
                        long valueLong = bytes2Long(valueByte);
                        if (valueLong != k) {
                            tree.walk();
                            System.out.println("key: " + k + ", value: " + valueByte + ", long: " + valueLong);
                        }
                    }
                    break;
                default:
                    System.out.println("deleting");
                    for (long k : keys) {
                        if (random.nextBoolean()) {
                            System.out.println("deleting " + k);
                            tree.remove(k);
                            tree.walk();
                        }
                    }
                    keys.clear();
                    BPlusNode node = tree.firstLeaf;
                    while (node != null) {
                        for (BPlusData k : node.data) {
                            keys.add(k.key);
                        }
                        node = node.next;
                    }
                    break;
            }
        }
    }
}
