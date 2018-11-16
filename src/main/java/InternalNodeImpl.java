import java.util.LinkedList;

public class InternalNodeImpl extends InternalNode {
    public InternalNodeImpl() {
        isLeaf = false;
        keys = new LinkedList<Long>();
        children = new LinkedList<Node>();
    }

    public int getChildIndexMayContainKey(long key) {
        if (key < keys.get(0)) {
            return 0;
        } else if (key >= keys.get(keys.size() - 1)) {
            return children.size() - 1;
        } else {
            // binary search to find
            int leftIndex = 0, rightIndex = keys.size() - 1, midIndex;
            while (leftIndex <= rightIndex) {
                midIndex = (leftIndex + rightIndex) / 2;
                long midKey = keys.get(midIndex);
                if (key == midKey) {
                    return midIndex + 1;
                } else if (key > midKey) {
                    leftIndex = midIndex + 1;
                } else {
                    rightIndex = midIndex - 1;
                }
            }
            return leftIndex - 1 + 1;
        }
    }

    int getChildIndex(Node child) {
        return children.indexOf(child);
    }

    int getKeyIndexOfChild(Node child) {
        return getChildIndex(child) - 1;
    }

    public Node getChildMayContainKey(long key) {
        int childIndex = getChildIndexMayContainKey(key);
        return children.get(childIndex);
    }

    Node getPreviousChildOf(Node child) {
        int childIndex = getChildIndex(child);
        if (childIndex - 1 >= 0) {
            return children.get(childIndex - 1);
        }
        return null;
    }

    Node getNextChildOf(Node child) {
        int childIndex = getChildIndex(child);
        if (childIndex + 1 < children.size()) {
            return children.get(childIndex + 1);
        }
        return null;
    }


    byte[] get(long key) {
        return getChildMayContainKey(key).get(key);
    }

    byte[] remove(long key, Tree tree) {
        return getChildMayContainKey(key).remove(key, tree);
    }

    void checkAfterInsert(Tree tree) {
        if (children.size() <= tree.order) {
            return;
        }
        split(tree);
    }


    void checkAfterRemove(Tree tree) {
        if (keys.size() >= tree.order / 2) {
            return;
        }
        //  keys.size() < tree.order / 2
        if (parent == null) {
            if (children.size() >= 2) {
                return;
            }
            Node newRoot = children.get(0);
            tree.root = newRoot;
            newRoot.parent = null;

            keys = null;
            children = null;
            return;
        } else {
            InternalNode previousBrother = null, nextBrother = null;
            previousBrother = (InternalNode) getPreviousBrother();
            nextBrother = (InternalNode) getNextBrother();

            if (previousBrother != null && previousBrother.keys.size() - 1 >= tree.order / 2) {
                borrowOneFromPreviousBrother();
                return;
            } else if (nextBrother != null && nextBrother.keys.size() - 1 >= tree.order / 2) {
                borrowOneFromNextBrother();
                return;
            } else if (previousBrother != null) {
                mergeIntoPreviousBrother(tree);
                return;
            } else if (nextBrother != null) {
                nextBrother.mergeIntoPreviousBrother(tree);
            }
        }
    }

    void split(Tree tree) {
        // split
        InternalNode leftNode = new InternalNodeImpl();
        InternalNode rightNode = new InternalNodeImpl();

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

        // keys
        long midKey = keys.get(leftNodeChildrenSize - 1);
        for (int i = 0; i < keys.size(); i++) {
            if (i < leftNodeChildrenSize - 1) {
                leftNode.keys.add(keys.get(i));
            } else if (i == leftNodeChildrenSize - 1) {
                continue;
            } else {
                rightNode.keys.add(keys.get(i));
            }
        }
        keys = null;

        // parent
        if (parent != null) {
            int index = parent.getChildIndex(this);
            parent.children.remove(this);
            leftNode.parent = parent;
            rightNode.parent = parent;
            parent.children.add(index, leftNode);
            parent.children.add(index + 1, rightNode);

            parent.keys.add(index, midKey);
            parent.checkAfterInsert(tree);
            parent = null;
        } else {
            // root
            InternalNode newRoot = new InternalNodeImpl();
            tree.root = newRoot;
//                tree.height++;

            leftNode.parent = newRoot;
            rightNode.parent = newRoot;

            newRoot.children.add(leftNode);
            newRoot.children.add(rightNode);
            newRoot.keys.add(midKey);
        }
    }


    void insertOrUpdate(long key, byte[] value, Tree tree) {
        getChildMayContainKey(key).insertOrUpdate(key, value, tree);
    }

    void borrowOneFromPreviousBrother() {
        InternalNode previousBrother = (InternalNode) getPreviousBrother();
        int borrowIndex = previousBrother.children.size() - 1;
        Node borrowChild = previousBrother.children.remove(borrowIndex);
        children.add(borrowChild);
        borrowChild.parent = this;

        // key change
        long brotherKeyUp = previousBrother.keys.remove(borrowIndex - 1);
        int keyIndexInParent = parent.getKeyIndexOfChild(this);
        long parentKeyDown = parent.keys.remove(keyIndexInParent);
        parent.keys.add(keyIndexInParent, brotherKeyUp);
        keys.add(0, parentKeyDown);
        return;
    }

    void borrowOneFromNextBrother() {
        InternalNode nextBrother = (InternalNode) getNextBrother();
        Node borrowChild = nextBrother.children.remove(0);
        children.add(borrowChild);
        borrowChild.parent = this;

        long brotherKeyUp = nextBrother.keys.remove(0);
        int keyIndexInParent = parent.getKeyIndexOfChild(nextBrother);
        long parentKeyDown = parent.keys.remove(keyIndexInParent);
        parent.keys.add(keyIndexInParent, brotherKeyUp);
        keys.add(parentKeyDown);
        return;
    }

    void mergeIntoPreviousBrother(Tree tree) {
        InternalNode previousBrother = (InternalNode) getPreviousBrother();
        // children
        for (int i = 0; i < children.size(); i++) {
            children.get(i).parent = previousBrother;
            previousBrother.children.add(children.get(i));
        }
        children = null;

        // keys
        int indexInParentChildren = parent.getChildIndex(this);
        int keyIndexInParent = parent.getKeyIndexOfChild(this);
        long keyInParent = parent.keys.remove(keyIndexInParent);
        parent.children.remove(this);

        previousBrother.keys.add(keyInParent);
        for (int i = 0; i < keys.size(); i++) {
            previousBrother.keys.add(keys.get(i));
        }
        keys = null;

        parent.checkAfterRemove(tree);
        parent = null;
        return;
    }

    int getTreeHeight() {
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

    void walk(int depth, int thisDepth) {
        if (thisDepth == depth) {
            System.out.print("(" + (isLeaf ? "L " : "I "));
            for (long d : keys) {
                System.out.print(d + " ");
            }
            System.out.print(")");

        } else if (thisDepth < depth) {
            for (Node child : children) {
                child.walk(depth, thisDepth + 1);
            }
        }
    }

    @Override
    void checkRelationship() {
        if (!isLeaf) {
            for (Node node : children) {
                node.parent = this;
                node.checkRelationship();
            }
        }
    }

    @Override
    LeafNode buildLeavesChain() {

        LeafNode pre = null;
        LeafNode head = null;
        for (Node node : children) {
            LeafNode now = node.buildLeavesChain();
            if (head != null) {
                pre.next = now;
                now.previous = pre;
            } else {
                head = now;
            }

            LeafNode tail = now;
            while (tail.next != null) {
                tail = tail.next;
            }
            pre = tail;
        }
        return head;

    }


    //    @Override
//    void mergeTwoNode(Node pre, Node node) {
//
//    }


}
