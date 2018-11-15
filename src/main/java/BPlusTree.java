import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;

import java.io.*;


public class BPlusTree {
    @Expose
    public int order;
    @Expose
    public BPlusNode root;
    public BPlusNode firstLeaf;

    public String filename;

    public BPlusTree(int order) {
        if (order < 3) {
            System.out.println("Order must be greater than 2");
            System.exit(0);
        }
        this.order = order;
        root = new BPlusNode(true);
        firstLeaf = root;
        filename = Config.indexDir + Config.seperator + System.currentTimeMillis() + ".json";
    }

    public static BPlusTree buildTreeFromJson(String path) throws IOException {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IOException("filename is null or empty");
        } else {
            BPlusTree tree = null;
            File jf = new File(path);
            if (!jf.exists()) {
                tree = new BPlusTree(Config.order);
                tree.filename = path;
                tree.save();
                return tree;
            } else {
                Gson gson = new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .setPrettyPrinting()
                        .create();

                BufferedReader reader = new BufferedReader(new FileReader(path));
                tree = gson.fromJson(reader, BPlusTree.class);
                tree.filename = path;
                tree.root.checkRelationship();

                try {
                    tree.firstLeaf = tree.root.buildLeavesChain();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return tree;
            }
        }
    }


    public byte[] get(long key) {
        return root.get(key);
    }

    public byte[] remove(long key) {
        return root.remove(key, this);
    }

    public void insertOrUpdate(long key, byte[] value) {
        root.insertOrUpdate(key, value, this);
    }

    public void walk() {
        if (root != null) {
            int height = root.getTreeHeight();
            System.out.println("===================================================================================");
            System.out.println("order: " + order);
            for (int i = 1; i <= height; i++) {
                root.walk(i, 1);
                System.out.println("");
            }

            System.out.println("leaf:");
            BPlusNode node = firstLeaf;
            while (node != null) {
                for (BPlusData d : node.data) {
                    System.out.print(d.key + " ");
                }
                node = node.next;
            }
            System.out.println("\n");
        }
    }

    public void save() {
        BufferedWriter writer = null;
        File file = new File(filename);
        try {

            if (!file.exists()) {

            }
            writer = new BufferedWriter(new FileWriter(file));
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(this, this.getClass(), writer);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
