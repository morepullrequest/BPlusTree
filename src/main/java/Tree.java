import com.google.gson.annotations.Expose;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;

import java.io.*;

public class Tree {
    @Expose
    public int order;
    @Expose
    public Node root;
    public LeafNode firstLeaf;

    public String filename;

    public Tree(int order) {
        if (order < 3) {
            System.out.println("Order must be greater than 2");
            System.exit(0);
        }
        Config.init();
        this.order = order;
        root = new LeafNodeImpl();
        firstLeaf = (LeafNode) root;
        filename = Config.indexDir + Config.seperator + System.currentTimeMillis() + ".json";
    }

    public static Tree buildTreeFromJson(String path) throws IOException {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IOException("Filename is null or empty");
        } else {
            Tree tree = null;
            File jf = new File(path);
            if (!jf.exists()) {
                tree = new Tree(Config.order);
                tree.filename = path;
                tree.save();
                return tree;
            } else {


                BufferedReader reader = new BufferedReader(new FileReader(path));
                tree = Util.gson.fromJson(reader, Tree.class);
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
            System.out.println("order: " + order);
            for (int i = 1; i <= height; i++) {
                root.walk(i, 1);
                System.out.println("");
            }

            System.out.println("leaf:");
            LeafNode node = firstLeaf;
            while (node != null) {
                for (long d : node.keys) {
                    System.out.print(d + " ");
                }
                node = node.next;
            }
            System.out.println("\n");
        }
    }

    public void save() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));

            Util.gson.toJson(this, this.getClass(), writer);
            writer.close();
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
