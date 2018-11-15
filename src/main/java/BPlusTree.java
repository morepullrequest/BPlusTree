public class BPlusTree {
    public int order;


    public BPlusNode root;


    public BPlusNode firstLeaf; // 第一个叶子节点，


//    public int height = 0;


    public byte[] get(long key) {
        return root.get(key);
    }

    public byte[] remove(long key) {
        return root.remove(key,this);
    }

    public void insertOrUpdate(long key, byte[] value) {
        root.insertOrUpdate(key, value, this);
    }

    public BPlusTree(int order) {
        if (order < 3) {
            System.out.println("Order must be greater than 2");
            System.exit(0);
        }

        this.order = order;
        root = new BPlusNode(true);
        firstLeaf = root;
    }
}
