import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// todo 分别派生 叶子节点 内部节点
public class BPlusNode {
    public boolean isLeaf;

    public BPlusNode parent; // root : parent is null
    public BPlusNode previous; // 前一个节点 leaf
    public BPlusNode next; // 后一个节点 leaf

    public List<BPlusData> data;
    public List<BPlusNode> children;


    public BPlusNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.data = new LinkedList<BPlusData>();
        if (!isLeaf) {
            children = new LinkedList<BPlusNode>();
        }
    }


    public byte[] get(long key) {
        if (isLeaf) {
            int index = getDataIndexOfKey(key);
            if (index == -1) {
                return null;
            } else {
                return data.get(index).value;
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

    public byte[] remove(long key, BPlusTree tree) {
        // todo remove and check
        if (isLeaf) {
            int removeIndex = getDataIndexOfKey(key);
            if (removeIndex == -1) {
                return null;
            }
            // root
            if (parent == null) {
//                if (data.size() == 1) {
//                    tree.height = 0;
//                }
                return data.remove(removeIndex).value;
            }
            // not root
            if (data.size() - 1 >= tree.order / 2) {

                byte[] val = data.remove(removeIndex).value; // ??
                return val;

            } else {
                // borrow a (key, value) from other nodes

                if (previous != null && previous.parent == parent && previous.data.size() - 1 >= tree.order / 2) {

                    BPlusData tempData = previous.data.remove(previous.data.size() - 1);
                    data.add(0, tempData);

                    int changeIndex = parent.children.indexOf(this);
                    parent.data.get(changeIndex - 1).key = tempData.key;

                    removeIndex = getDataIndexOfKey(key);
                    return data.remove(removeIndex).value;
                }

                if (next != null && next.parent == parent && next.data.size() - 1 >= tree.order / 2) {
                    BPlusData tempData = next.data.remove(0);
                    data.add(tempData);

                    int changeIndex = parent.children.indexOf(next);
                    parent.data.get(changeIndex - 1).key = next.data.get(0).key;

                    removeIndex = getDataIndexOfKey(key);
                    return data.remove(removeIndex).value;

                }

                // 与其他node合并 todo
                if (previous != null && previous.parent == parent) {
                    byte[] val = null;
                    for (int i = 0; i < data.size(); i++) {
                        if (data.get(i).key == key) {
                            val = data.get(i).value;
                        } else {
                            previous.data.add(data.get(i));
                        }
                    }
                    int indexInParent = parent.children.indexOf(this);
                    parent.children.remove(this);
                    parent.data.remove(indexInParent - 1);


                    previous.next = next;
                    if (next != null) {
                        next.previous = previous;
                    }

                    parent.checkAfterRemove(tree);

                    data = null;
                    previous = null;
                    next = null;
                    parent = null;

                    return val;
                }
                if (next != null && next.parent == parent) {
                    removeIndex = getDataIndexOfKey(key);
                    byte[] val = data.remove(removeIndex).value;

                    for (int i = 0; i < next.data.size(); i++) {
                        data.add(next.data.get(i));
                    }

                    int indexInParent = parent.children.indexOf(next);
                    parent.children.remove(next);
                    parent.data.remove(indexInParent - 1);

                    next = next.next;
                    if (next != null) {
                        next.previous = this;
                    }

                    parent.checkAfterRemove(tree);
                    return val;
                }
            }
        }
        // not leaf todo
        return null;
    }

    private void checkAfterRemove(BPlusTree tree) {

    }

    public void insertOrUpdate(long key, byte[] value, BPlusTree tree) {

        if (isLeaf) {
            // update or insert , no split
            if (getDataIndexOfKey(key) != -1 || data.size() < tree.order - 1) { // todo - 1
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
        if (isLeaf) {
            int updateIndex = getDataIndexOfKey(key);
            if (updateIndex != -1) {
                // update
                data.get(updateIndex).value = value;
            } else {
                // insert

                int insertIndex = getLeafInsertIndexOfKey(key);

                // key should >= data[0].key
                // review
                if (insertIndex == 0 || insertIndex == -1) {
                    System.out.println("error insert");
                    return;
                }

//                if (insertIndex != -1) {
                data.add(insertIndex, new BPlusData(key, value));
//                }


            }
        }
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


        // size
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
                insertNode.data.add(data.get(i));

            } else {
                if (data.get(i).key > key) {
                    insertNode.data.add(new BPlusData(key, value));
                    hasInsert = true;
                    i--;
                } else {
                    insertNode.data.add(data.get(i));
                }
            }
        }
        if (!hasInsert) {
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
//            tree.height = 1;
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
}

