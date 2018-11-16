import com.google.gson.annotations.Expose;
import com.google.protobuf.ByteString;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// todo 分别派生 叶子节点 内部节点
public class BPlusNode {
    @Expose
    public boolean isLeaf;

    public BPlusNode parent; // root : parent is null

    public BPlusNode previous; // 前一个节点 leaf

    public BPlusNode next; // 后一个节点 leaf
    @Expose
    public List<BPlusData> data;
    @Expose
    public List<BPlusNode> children;
    @Expose
    public String filename;
    // proto
//    DatabaseSliceOuterClass.DatabaseSlice storage = null;


    public BPlusNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.data = new LinkedList<BPlusData>();
        if (!isLeaf) {
            children = new LinkedList<BPlusNode>();
        }
        filename = "data/" + UUID.randomUUID().toString() + ".ser";
    }

    public BPlusNode(boolean isLeaf, String filename, BPlusTree tree) {//throws IOException {
        this.isLeaf = isLeaf;
        this.data = new LinkedList<BPlusData>();
        if (!isLeaf) {
            children = new LinkedList<BPlusNode>();
        } else {
            if (StringUtil.isNullOrEmpty(filename)) { // todo 文件名校验 正则 uuid.ser
//                throw new IOException("filename is nul or empty");
                File dataDir = new File("data");
                if (!dataDir.exists()) {
                    dataDir.mkdir();
                }

                filename = "data/" + UUID.randomUUID().toString() + ".ser";
                tree.save();
            }
        }
    }

    public byte[] get(long key) {
        if (isLeaf) {
            try {
                DatabaseSliceOuterClass.DatabaseSlice tempData = DatabaseSliceOuterClass.DatabaseSlice.parseFrom(new FileInputStream(filename));
                return tempData.getDataMap().get(key).toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // not leaf. have child
        int childIndex = getChildIndexMayContainKey(key);
        if (childIndex == -1) {
            return null;
        } else {
            return children.get(childIndex).get(key);
        }
    }

    private BPlusData getOneByKey(long key) {
        if (!isLeaf) {
            return null;
        }
        return getOneByIndex(getDataIndexOfKey(key));
    }


    private BPlusData getOneByIndex(int index) {
        if (!isLeaf) {
            return null;
        }
        FileInputStream in = null;
        DatabaseSliceOuterClass.DatabaseSlice slice = null;
        BPlusData getData = data.get(index);
        getData.value = null;
        try {
            in = new FileInputStream(filename);
            slice = DatabaseSliceOuterClass.DatabaseSlice.parseFrom(in);
            in.close();

            if (slice.containsData(getData.key)) {
                getData.value = slice.getDataOrThrow(getData.key).toByteArray();
                return getData;
            } else {
                // throw new DBException("key "+bPlusData.key+" not exists.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private int getDataIndexOfKey(long key) {
        int leftIndex = 0, rightIndex = data.size() - 1, midIndex;
        while (leftIndex <= rightIndex) {
            midIndex = (leftIndex + rightIndex) / 2;
            long tempKey = data.get(midIndex).key;
            if (tempKey == key) {
                return midIndex;
            } else if (tempKey > key) {
                rightIndex = midIndex - 1;
            } else {
                leftIndex = midIndex + 1;
            }
        }
        return -1;
    }

    private int getLeafInsertIndexOfKey(long key) {
        if (parent == null && data.size() == 0) {
            return 0;
        }

        if (key < data.get(0).key) {
            return 0;
        } else if (key > data.get(data.size() - 1).key) {
            return data.size();
        } else {
            int leftIndex = 0, rightIndex = data.size() - 1, midIndex;
            while (leftIndex + 1 < rightIndex) {
                midIndex = (leftIndex + rightIndex) / 2;
                long tempKey = data.get(midIndex).key;
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

    private int getChildIndexMayContainKey(long key) {
        if (isLeaf) {
            return -1;
        }
        if (key < data.get(0).key) {
            return 0;
        } else if (key >= data.get(data.size() - 1).key) {
            return children.size() - 1;
        } else {
            // binary search to find
            int leftIndex = 0, rightIndex = data.size() - 1, midIndex;
            while (leftIndex <= rightIndex) {
                midIndex = (leftIndex + rightIndex) / 2;
                long tempKey = data.get(midIndex).key;
                if (key == tempKey) {
                    return midIndex + 1;
                } else if (key > tempKey) {
                    leftIndex = midIndex + 1; // if let leftIndex = midIndex, loop forever
                } else {
                    rightIndex = midIndex - 1;
                }
            }
            return leftIndex - 1 + 1;
        }
    }

    public byte[] removeReal(long key) {
        int removeIndex = getDataIndexOfKey(key);
        if (removeIndex == -1) {
            return null;
        }

        return removeIndex(removeIndex).value;
    }

    private BPlusData removeIndex(int index) {
        if (index >= 0 && index < data.size()) {
            BPlusData bPlusData = data.get(index);
            bPlusData.value = null;
            Map<Long, ByteString> map;
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(filename);
                DatabaseSliceOuterClass.DatabaseSlice tempData = DatabaseSliceOuterClass.DatabaseSlice.parseFrom(in);
                in.close();

                map = tempData.getDataMap();
                if (map.containsKey(bPlusData.key)) {
                    data.remove(index);
                    bPlusData.value = map.remove(bPlusData.key).toByteArray();


                    // save
                    out = new FileOutputStream(filename);
                    tempData.writeTo(out);
                    out.close();
                } else {

                    // throw new DBException("key "+bPlusData.key+" not exists.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bPlusData;
        }
        // throw
        return null;
    }


    private BPlusData removeLastNode() {
        return removeIndex(data.size() - 1);
    }

    private BPlusData removeFirstNode() {
        return removeIndex(0);
    }


    private void saveOne(long key, byte[] value) {
        Map<Long, ByteString> map;
        FileInputStream in;
        FileOutputStream out;
        DatabaseSliceOuterClass.DatabaseSlice slice;
        DatabaseSliceOuterClass.DatabaseSlice.Builder builder;
        File file = new File(filename);

        try {
            if (data.size() == 0) {
                builder = DatabaseSliceOuterClass.DatabaseSlice.newBuilder();
            } else {

                in = new FileInputStream(filename);
                slice = DatabaseSliceOuterClass.DatabaseSlice.parseFrom(in);
                builder = slice.toBuilder();
                in.close();

            }
            if (value == null) {
                System.out.println("null");
                ;
            }
            builder.putData(key, ByteString.copyFrom(value));
            slice = builder.build();

            out = new FileOutputStream(filename);
            slice.writeTo(out);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void saveOneIndexAndData(long key, byte[] value) {
        FileInputStream in;
        FileOutputStream out;
        DatabaseSliceOuterClass.DatabaseSlice slice;
        DatabaseSliceOuterClass.DatabaseSlice.Builder builder;
        if (isLeaf) {
            int updateIndex = getDataIndexOfKey(key);
            if (updateIndex != -1) {
                // update
                try {
                    in = new FileInputStream(filename);
                    slice = DatabaseSliceOuterClass.DatabaseSlice.parseFrom(in);
                    in.close();

                    slice.getDataMap().put(key, ByteString.copyFrom(value));

                    out = new FileOutputStream(filename);
                    slice.writeTo(out);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // insert
                int insertIndex = getLeafInsertIndexOfKey(key);
                try {
                    if (data.size() == 0) {
                        builder = DatabaseSliceOuterClass.DatabaseSlice.newBuilder();

                    } else {

                        in = new FileInputStream(filename);
                        slice = DatabaseSliceOuterClass.DatabaseSlice.parseFrom(in);
                        builder = slice.toBuilder();
                        in.close();

                    }
                    builder.putData(key, ByteString.copyFrom(value));
                    slice = builder.build();


                    out = new FileOutputStream(filename);
                    slice.writeTo(out);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (insertIndex == -1) {
                    System.out.println("error insert");
                } else {
                    data.add(insertIndex, new BPlusData(key, null));
                }
            }
        }
    }

    public byte[] remove(long key, BPlusTree tree) {
        if (isLeaf) {
            int removeIndex = getDataIndexOfKey(key);
            if (removeIndex == -1) {
                return null;
            }
            // root, size can be fewer than order/2, just remove
            if (parent == null) {
                return removeReal(key);
            }
            // not root
            if (data.size() - 1 >= tree.order / 2) {
                byte[] val = removeReal(key);
                return val;

            } else {
                // borrow a (key, value) from other nodes
                if (previous != null && previous.parent == parent && previous.data.size() - 1 >= tree.order / 2) {

                    // index and storage
                    BPlusData tempData = previous.removeLastNode();
                    data.add(0, tempData);
                    saveOne(tempData.key, tempData.value);
                    tempData.value = null;

                    int changeIndex = parent.children.indexOf(this);
                    parent.data.get(changeIndex - 1).key = tempData.key;

                    return removeReal(key);
                }

                if (next != null && next.parent == parent && next.data.size() - 1 >= tree.order / 2) {
                    BPlusData tempData = next.removeFirstNode();
                    data.add(tempData);
                    saveOne(tempData.key, tempData.value);
                    tempData.value = null;

                    int changeIndex = parent.children.indexOf(next);
                    parent.data.get(changeIndex - 1).key = next.data.get(0).key;

                    return removeReal(key);
                }

                // merge with other node
                if (previous != null && previous.parent == parent) {
                    // merge with previous brother

                    byte[] val = null;
                    for (int i = 0; i < data.size(); i++) {
                        if (data.get(i).key == key) {
//                            val = data.get(i).value;
                            val = getOneByIndex(i).value;
                        } else {
//                            previous.data.add(data.get(i));
                            BPlusData temp = getOneByIndex(i);
                            previous.data.add(temp);
                            previous.saveOne(temp.key, temp.value);
                            temp.value = null;
                        }
                    }
                    // parent
                    int indexInParent = parent.children.indexOf(this);
                    parent.children.remove(this);
                    parent.data.remove(indexInParent - 1);

                    // previous next
                    previous.next = next;
                    if (next != null) {
                        next.previous = previous;
                    }

                    parent.checkAfterRemove(tree);

                    data = null;
                    // todo delete data file
                    previous = null;
                    next = null;
                    parent = null;

                    return val;
                }
                if (next != null && next.parent == parent) {
                    // merge with next brother
                    removeIndex = getDataIndexOfKey(key);
                    byte[] val = removeIndex(removeIndex).value;

                    for (int i = 0; i < next.data.size(); i++) {
                        data.add(next.data.get(i));
                        BPlusData temp = next.getOneByIndex(i);
                        saveOne(temp.key, temp.value);
                    }

                    int indexInParent = parent.children.indexOf(next);
                    parent.children.remove(next);
                    parent.data.remove(indexInParent - 1);
                    // todo delete file

                    next.parent = null;
                    next.data = null;
                    next.previous = null;
                    next = next.next;
                    if (next != null) {
                        next.previous = this;
                    }
//                    System.out.println("before check");
//                    tree.walk();
                    parent.checkAfterRemove(tree);
//                    System.out.println("after check");
//                    tree.walk();
                    return val;
                }

                return null;
            }
        } else {
            // not leaf
            int childIndex = getChildIndexMayContainKey(key);
            if (childIndex == -1) {
                System.out.println("no key" + key);
                return null;
            }

            return children.get(childIndex).remove(key, tree);

        }
    }

    private void checkAfterRemove(BPlusTree tree) {
        // internal node remove a child and a key, check if valid
        if (data.size() >= tree.order / 2) {
            // valid
            return;
        }

        // data.size()< order / 2
        if (parent == null) {
            // root
            if (children.size() >= 2) {
                return;
            }

            // root and data.size == 1
            // merge with child
            BPlusNode newRoot = children.get(0);
            tree.root = newRoot;
            newRoot.parent = null;

            data = null;
            children = null;
            return;
        }

        // parent != null
        // get brothers
        int indexInParentChildren = parent.children.indexOf(this);
        BPlusNode previousBrother = null, nextBrother = null;
        if (indexInParentChildren - 1 >= 0) {
            previousBrother = parent.children.get(indexInParentChildren - 1);
        }
        if (indexInParentChildren + 1 < parent.children.size()) {
            nextBrother = parent.children.get(indexInParentChildren + 1);
        }

        if (previousBrother != null && previousBrother.data.size() - 1 >= tree.order / 2) {
            int borrowIndex = previousBrother.children.size() - 1;
            BPlusNode borrow = previousBrother.children.remove(borrowIndex);
            this.children.add(0, borrow);
            borrow.parent = this;

            BPlusData newKeyInParentData = previousBrother.data.remove(borrowIndex - 1);
            BPlusData oldKeyInParentData = parent.data.remove(indexInParentChildren - 1);
            parent.data.add(indexInParentChildren - 1, newKeyInParentData);
            this.data.add(0, oldKeyInParentData);

            return;
        }

        if (nextBrother != null && nextBrother.data.size() - 1 >= tree.order / 2) {

            BPlusNode borrow = nextBrother.children.remove(0);
            this.children.add(borrow);
            borrow.parent = this;

            BPlusData newKeyInParentData = nextBrother.data.remove(0); //?
            int nextBrotherDataIndexInParent = parent.children.indexOf(nextBrother) - 1;
            BPlusData oldKeyInParentData = parent.data.remove(nextBrotherDataIndexInParent);//?
            parent.data.add(nextBrotherDataIndexInParent, newKeyInParentData);
            this.data.add(oldKeyInParentData);
            return;
        }

        // merge with pre brother
        if (previousBrother != null) {
            // children
            for (int i = 0; i < children.size(); i++) {
                children.get(i).parent = previousBrother;
                previousBrother.children.add(children.get(i));
            }
            children = null;

            // data
            indexInParentChildren = parent.children.indexOf(this);
            BPlusData keyInParentData = parent.data.remove(indexInParentChildren - 1);
            parent.children.remove(this);

            previousBrother.data.add(keyInParentData);
            for (int i = 0; i < data.size(); i++) {
                previousBrother.data.add(data.get(i));
            }
            data = null;

//            System.out.println("before check");
//            tree.walk();
            parent.checkAfterRemove(tree);
//            System.out.println("after check");
//            tree.walk();
            parent = null;
            return;
        }
        if (nextBrother != null) {
            for (int i = 0; i < nextBrother.children.size(); i++) {
                nextBrother.children.get(i).parent = this;
                children.add(nextBrother.children.get(i));
            }
            nextBrother.children = null;

            BPlusData keyInParentData = parent.data.remove(indexInParentChildren);
            parent.children.remove(nextBrother);

            data.add(keyInParentData);
            for (int i = 0; i < nextBrother.data.size(); i++) {
                data.add(nextBrother.data.get(i));
            }
            nextBrother.data = null;
            nextBrother.parent = null;

//            System.out.println("before check");
//            tree.walk();
            parent.checkAfterRemove(tree);
//            System.out.println("after check");
//            tree.walk();
            return;
        }
    }

    public void insertOrUpdate(long key, byte[] value, BPlusTree tree) {

        if (isLeaf) {
            // update or insert , no split
            if (getDataIndexOfKey(key) != -1 || data.size() + 1 < tree.order) {
                leafInsertOrUpdate(key, value);

//                if (tree.height == 0) { // ???
//                    tree.height = 1;
//                }
                return;
            }

            // split into two nodes
            leafSplitAndInsert(key, value, tree);
            return;
        }

        // no leaf
        int childIndex = getChildIndexMayContainKey(key);
        children.get(childIndex).insertOrUpdate(key, value, tree);
    }

    private void leafInsertOrUpdate(long key, byte[] value) {
        saveOneIndexAndData(key, value);
    }

    private void leafSplitAndInsert(long key, byte[] value, BPlusTree tree) {
        BPlusNode leftNode = new BPlusNode(true);
        BPlusNode rightNode = new BPlusNode(true);

        // previous and next
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


        // data and index
        int leftSize = tree.order / 2;
        boolean hasInsert = false;
        for (int i = 0; i < data.size(); i++) {
            BPlusNode insertNode = null;
            if (leftNode.data.size() < leftSize) {
                insertNode = leftNode;
            } else {
                insertNode = rightNode;
            }

            if (hasInsert) {
                BPlusData temp = getOneByIndex(i);
                insertNode.saveOne(data.get(i).key, temp.value);
                insertNode.data.add(data.get(i));
            } else {
                if (data.get(i).key > key) {
                    insertNode.saveOne(data.get(i).key, getOneByIndex(i).value);
                    insertNode.data.add(new BPlusData(key, value));
                    hasInsert = true;
                    i--;
                } else {
                    insertNode.saveOne(data.get(i).key, getOneByIndex(i).value);
                    insertNode.data.add(data.get(i));
                }
            }
        }
        if (!hasInsert) {
            rightNode.saveOne(key, value);
            rightNode.data.add(new BPlusData(key, value));
        }
        data = null;
        children = null;

        // parent
        if (parent != null) {
            int index = parent.children.indexOf(this);
            parent.children.remove(this);
            leftNode.parent = parent;
            rightNode.parent = parent;
            parent.children.add(index, leftNode);
            parent.children.add(index + 1, rightNode);
            parent.data.add(index, new BPlusData(rightNode.data.get(0).key));

            // split parent
            parent.internalNodeCheck(tree);

            parent = null;
            // cg
        } else {
            // root node
            BPlusNode rootParent = new BPlusNode(false);

            leftNode.parent = rootParent;
            rightNode.parent = rootParent;
            rootParent.children.add(leftNode);
            rootParent.children.add(rightNode);
            rootParent.data.add(new BPlusData(rightNode.data.get(0).key));

            tree.root = rootParent;
            tree.firstLeaf = leftNode;
        }
    }

    private void internalNodeCheck(BPlusTree tree) {
        if (children.size() > tree.order) {
            // invalid, thus split
            BPlusNode leftNode = new BPlusNode(false);
            BPlusNode rightNode = new BPlusNode(false);

            // children
            // children.size() == tree.order()+1
            int leftNodeChildrenSize = children.size() / 2;
            for (int i = 0; i < children.size(); i++) {
                if (i < leftNodeChildrenSize) {
                    leftNode.children.add(children.get(i));
                    children.get(i).parent = leftNode;
                } else {
                    rightNode.children.add(children.get(i));
                    children.get(i).parent = rightNode;
                }
            }
            children = null;

            // data
            BPlusData midData = data.get(leftNodeChildrenSize - 1);
            for (int i = 0; i < data.size(); i++) {
                if (i < leftNodeChildrenSize - 1) {
                    leftNode.data.add(data.get(i));
                } else if (i == leftNodeChildrenSize - 1) {
                    continue;
                } else {
                    rightNode.data.add(data.get(i));
                }
            }
            data = null;

            // parent
            if (parent != null) {
                int index = parent.children.indexOf(this);
                parent.children.remove(this);
                leftNode.parent = parent;
                rightNode.parent = parent;
                parent.children.add(index, leftNode);
                parent.children.add(index + 1, rightNode);

                parent.data.add(index, midData);
                parent.internalNodeCheck(tree);
                parent = null;
            } else {
                // root
                BPlusNode newRoot = new BPlusNode(false);
                tree.root = newRoot;
//                tree.height++;

                leftNode.parent = newRoot;
                rightNode.parent = newRoot;

                newRoot.children.add(leftNode);
                newRoot.children.add(rightNode);
                newRoot.data.add(midData);
            }
        }
        // else valid
    }


    public int getTreeHeight() {
        if (isLeaf) {
            return 1;
        } else {
            int childHeight = children.get(0).getTreeHeight();
            // check
            for (int i = 0; i < children.size(); i++) {
                int tempHeight = children.get(i).getTreeHeight();
                if (tempHeight != childHeight) {
                    System.out.println("error...........children has different height");
                }
            }
            return childHeight + 1;
        }
    }

    public int getNodeDepth() {
        if (parent == null) {
            return 1;
        } else {
            return parent.getNodeDepth() + 1;
        }
    }

    public void walk(int depth, int thisDepth) {
        if (thisDepth == depth) {
            System.out.print("(" + (isLeaf ? "L " : "I "));
            for (BPlusData d : data) {
                System.out.print(d.key + " ");
            }
            System.out.print(")");

        } else {
            for (BPlusNode child : children) {
                child.walk(depth, thisDepth + 1);
            }
        }
    }

    public void checkRelationship() {
        if (!isLeaf) {
            for (BPlusNode node : children) {
                node.parent = this;
                node.checkRelationship();
            }
        }
    }

    public BPlusNode getFirstLeaf() {
        if (isLeaf) {
            return this;
        } else {
            if (children.size() > 0) {
                children.get(0).getFirstLeaf();
            }
        }
        return null;
    }

    public BPlusNode getLastLeaf() {
        if (isLeaf) {
            return this;
        } else {
            if (children.size() > 0) {
                return children.get(children.size() - 1).getLastLeaf();
            }
            return null;
        }
    }

    public BPlusNode buildLeavesChain() throws Exception {

        if (!isLeaf) {
            if (children.size() > 0) {
                BPlusNode pre = null;
                BPlusNode head = null;
                for (BPlusNode node : children) {
                    BPlusNode now = node.buildLeavesChain();
                    if (head != null) {
                        pre.next = now;
                        now.previous = pre;
                    } else {
                        head = now;
                    }

                    BPlusNode tail = now;
                    while (tail.next != null) {
                        tail = tail.next;
                    }
                    pre = tail;
                }
                return head;
            } else {
                throw new Exception("internal node has no child.");
            }
        } else {
            this.previous = null;
            this.next = null;
            return this;
        }
    }
}


