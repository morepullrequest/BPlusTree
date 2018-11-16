import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TT {
    public static void main(String[] args) {
        int order = 50;
        int size = 1000;
//        testInsert(order, size);
//        randomTest(order);
//        testBug(order, size);
//        randomTest(order);
        testLoadFromFile();
    }

    public static void testInsert(int order, int size) {
        Tree tree = new Tree(order);
        for (int i = 0; i < size; i++) {
            tree.insertOrUpdate((long) i, Util.long2Bytes((long) i));
            tree.walk();
        }
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();

        gson.toJson(tree, System.out);
    }

    public static void testBug(int order, int size) {
        long[] a = {5, 9, 14, 16, 18, 25, 30, 32, 45, 47, 51, 53, 66, 72, 79, 98};
        Tree tree = new Tree(order);

        for (long b : a) {
            tree.insertOrUpdate(b, Util.long2Bytes(b));
        }
        tree.walk();

//        tree.remove(45);

        for (long key : a) {
            System.out.println("removing " + key);
            tree.remove(key);
            tree.walk();
        }

    }

    public static void randomTest(int order) {
        Set<Long> keys = new HashSet<Long>();
        Tree tree = new Tree(order);
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int op = random.nextInt(3);
            long insertKey = 0;
            switch (op) {
                case 0:
                    for (int j = 0; j < 5; j++) {
                        insertKey = (long) random.nextInt(100);
                        tree.insertOrUpdate(insertKey, Util.long2Bytes(insertKey));
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
                        long valueLong = Util.bytes2Long(valueByte);
                        if (valueLong != k) {
                            tree.walk();
                            System.out.println("key: " + k + ", value: " + valueByte + ", long: " + valueLong);
                            System.out.println("========================================================================");
                        }
                    }
                    break;
                default:
                    for (long k : keys) {
                        if (random.nextBoolean()) {
                            System.out.println("deleting: " + k);
                            tree.remove(k);
                            tree.walk();
                        }
                    }
                    keys.clear();
                    LeafNode node = tree.firstLeaf;
                    while (node != null) {
                        for (long k : node.keys) {
                            keys.add(k);
                        }
                        node = node.next;
                    }
                    break;
            }
        }
        tree.save();
    }


    public static void testLoadFromFile() {
        Tree tree;
        try {
            tree = Tree.buildTreeFromJson(Config.indexFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
