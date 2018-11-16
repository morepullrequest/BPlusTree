import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class LeafNodeImpl extends LeafNode {
    public LeafNodeImpl() {
        isLeaf = true;
        keys = new LinkedList<Long>();
        filename = "data/" + UUID.randomUUID().toString() + ".ser";
    }

    byte[] get(long key) {
        return getOneByKey(key).value;
    }


    byte[] remove(long key, Tree tree) {
        // remove
        KeyValue kv = removeOneByKey(key);

        // check
        checkAfterRemove(tree);

        tree.save();
        return kv.value;
    }

    void insertOrUpdate(long key, byte[] value, Tree tree) {
        // insert
        saveOne(key, value);
        // check
        checkAfterInsert(tree);

        tree.save();
    }

    int getInsertIndexOf(long key) {
        if (parent == null && keys.size() == 0) {
            return 0;
        }

        if (key < keys.get(0)) {
            return 0;
        } else if (key > keys.get(keys.size() - 1)) {
            return keys.size();
        } else {
            int leftIndex = 0, rightIndex = keys.size() - 1, midIndex;
            while (leftIndex + 1 < rightIndex) {
                midIndex = (leftIndex + rightIndex) / 2;
                long tempKey = keys.get(midIndex);
                if (tempKey == key) {
                    // not insert but update
                    return -1;
                } else if (tempKey > key) {
                    rightIndex = midIndex;
                } else {
                    leftIndex = midIndex;
                }
            }
            return leftIndex + 1;
        }
    }


    KeyValue getOneByKey(long key) {
        KeyValue kv = new KeyValue();
        kv.key = key;
        DatabaseSliceOuterClass.DatabaseSlice slice = readSliceFromStorage();
        kv.value = slice.getDataOrThrow(kv.key).toByteArray();
        return kv;
    }

    KeyValue getOneByKeyIndex(int keyIndex) {
        KeyValue kv = new KeyValue();
        kv.key = keys.get(keyIndex);
        DatabaseSliceOuterClass.DatabaseSlice slice = readSliceFromStorage();
        kv.value = slice.getDataOrThrow(kv.key).toByteArray();
        return kv;
    }

    List<KeyValue> slice(int startIndex, int endIndex) {
        DatabaseSliceOuterClass.DatabaseSlice slice = readSliceFromStorage();
        List<KeyValue> keyValueList = new LinkedList<KeyValue>();
        for (int i = startIndex; i <= endIndex; i++) {
            KeyValue kv = new KeyValue();
            kv.key = keys.get(i);
            kv.value = slice.getDataOrThrow(kv.key).toByteArray();
            keyValueList.add(kv);
        }
        return keyValueList;
    }

    KeyValue removeOneByKey(long key) {
        KeyValue kv = new KeyValue();
        kv.key = key;
        DatabaseSliceOuterClass.DatabaseSlice.Builder builder = readSliceFromStorage().toBuilder();
        kv.value = builder.getDataOrThrow(kv.key).toByteArray();
        builder.removeData(kv.key);
        saveSliceToStorage(builder.build());
        keys.remove(key);
        return kv;
    }

    KeyValue removeOneByKeyIndex(int keyIndex) {
        KeyValue kv = new KeyValue();
        kv.key = keys.get(keyIndex);
        DatabaseSliceOuterClass.DatabaseSlice.Builder builder = readSliceFromStorage().toBuilder();
        kv.value = builder.getDataOrThrow(kv.key).toByteArray();
        builder.removeData(kv.key);
        saveSliceToStorage(builder.build());
        keys.remove(keyIndex);
        return kv;

    }

    void saveAll(List<KeyValue> keyValueList) {
        DatabaseSliceOuterClass.DatabaseSlice.Builder builder = readSliceFromStorage().toBuilder();
        for (KeyValue kv : keyValueList) {
            int keyIndex = getKeyIndexOf(kv.key);
            if (keyIndex != -1) {
                //update
                builder.putData(kv.key, ByteString.copyFrom(kv.value));
            } else {
                //insert
                int insertKeyIndex = getInsertIndexOf(kv.key);
                builder.putData(kv.key, ByteString.copyFrom(kv.value));

                keys.add(insertKeyIndex, kv.key);
            }
        }
        saveSliceToStorage(builder.build());
    }

    protected void saveOne(long key, byte[] value) {
        DatabaseSliceOuterClass.DatabaseSlice.Builder builder = readSliceFromStorage().toBuilder();
        int keyIndex = getKeyIndexOf(key);
        if (keyIndex != -1) {
            //update
            builder.putData(key, ByteString.copyFrom(value));
            saveSliceToStorage(builder.build());
        } else {
            //insert
            int insertKeyIndex = getInsertIndexOf(key);
            builder.putData(key, ByteString.copyFrom(value));
            saveSliceToStorage(builder.build());
            keys.add(insertKeyIndex, key);
        }
    }

    void saveOne(KeyValue kv) {
        saveOne(kv.key, kv.value);
    }


    void saveSliceToStorage(DatabaseSliceOuterClass.DatabaseSlice slice) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(filename);
            slice.writeTo(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    DatabaseSliceOuterClass.DatabaseSlice readSliceFromStorage() {
        FileInputStream in = null;
        DatabaseSliceOuterClass.DatabaseSlice slice = null;
        try {
            if (keys.size() == 0) {
                slice = DatabaseSliceOuterClass.DatabaseSlice.newBuilder().build();
            } else {
                in = new FileInputStream(filename);
                slice = DatabaseSliceOuterClass.DatabaseSlice.parseFrom(in);
                in.close();
            }
            return slice;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void checkAfterInsert(Tree tree) {
        if (keys.size() <= tree.order - 1) {
            return;
        } else {
            split(tree);
        }
    }

    void split(Tree tree) {

        // split
        LeafNode leftNode = new LeafNodeImpl();
        LeafNode rightNode = new LeafNodeImpl();

        if (previous != null) {
            previous.next = leftNode;
            leftNode.previous = previous;
        } else {
            tree.firstLeaf = leftNode;
        }
        if (next != null) {
            next.previous = rightNode;
            rightNode.next = next;
        }
        leftNode.next = rightNode;
        rightNode.previous = leftNode;
        previous = null; // going to remove this node
        next = null; // going to remove this node

        int leftSize = tree.order / 2;
        leftNode.saveAll(this.slice(0, leftSize - 1));
        rightNode.saveAll(this.slice(leftSize, keys.size() - 1));

        // filename = null
        // delete file

        // parent check
        if (parent != null) {
            int index = parent.getChildIndex(this);
            parent.children.remove(this);
            leftNode.parent = parent;
            rightNode.parent = parent;
            parent.children.add(index, leftNode);
            parent.children.add(index + 1, rightNode);
            parent.keys.add(index, rightNode.keys.get(0));

            parent.checkAfterInsert(tree);
            parent = null;
        } else {
            InternalNode newRoot = new InternalNodeImpl();

            leftNode.parent = newRoot;
            rightNode.parent = newRoot;
            newRoot.children.add(leftNode);
            newRoot.children.add(rightNode);
            newRoot.keys.add(rightNode.keys.get(0));

            tree.root = newRoot;
            tree.firstLeaf = leftNode;
        }
        tree.save();
    }


    void checkAfterRemove(Tree tree) {
        if (parent == null || keys.size() >= tree.order / 2) {
            return;
        }


        if (previous != null && previous.parent == parent && previous.keys.size() - 1 >= tree.order / 2) {
            borrowOneFromPreviousBrother();
        } else if (next != null && next.parent == parent && next.keys.size() - 1 >= tree.order / 2) {
            borrowOneFromNextBrother();
        } else if (previous != null && previous.parent == parent) {
            mergeIntoPreviousBrother(tree);
        } else if (next != null && next.parent == parent) {
            next.mergeIntoPreviousBrother(tree);
        }
    }


    void borrowOneFromPreviousBrother() {
        KeyValue borrowNode = previous.removeOneByKeyIndex(previous.keys.size() - 1);
        saveOne(borrowNode);

        int keyIndexInParent = parent.getKeyIndexOfChild(this);
        parent.keys.set(keyIndexInParent, this.keys.get(0));
    }

    void borrowOneFromNextBrother() {

        KeyValue borrowNode = next.removeOneByKeyIndex(0);
        saveOne(borrowNode);

        int keyIndexInParent = parent.getKeyIndexOfChild(next);
        parent.keys.set(keyIndexInParent, next.keys.get(0));
    }

    void mergeIntoPreviousBrother(Tree tree) {
        // data
        previous.saveAll(this.slice(0, keys.size() - 1));
        // remove data
        //
        keys.clear();
        keys = null; // todo delete file


        // parent
        int keyIndexInParent = parent.getKeyIndexOfChild(this);
        parent.children.remove(this);
        parent.keys.remove(keyIndexInParent);

        // previous next
        previous.next = next;
        if (next != null) {
            next.previous = previous;
        }
        parent.checkAfterRemove(tree);

        previous = null;
        next = null;
        parent = null;
    }

    int getTreeHeight() {
        return 1;
    }

    @Override
    void walk(int depth, int thisDepth) {
        if (thisDepth == depth) {
            System.out.print("(" + (isLeaf ? "L " : "I "));
            for (long d : keys) {
                System.out.print(d + " ");
            }
            System.out.print(")");
        }
    }

    @Override
    LeafNode buildLeavesChain() {
        this.previous = null;
        this.next = null;
        return this;
    }
}
