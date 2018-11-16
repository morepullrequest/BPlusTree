import com.google.gson.annotations.Expose;

import java.util.LinkedList;
import java.util.List;

abstract public class Node {
    @Expose
    public boolean isLeaf;

    public InternalNode parent; // root : parent is null

    @Expose
    public List<Long> keys;

    abstract byte[] get(long key);

    public Node() {
        keys = new LinkedList<Long>();
        parent = null;
    }

    int getKeyIndexOf(long key) {
        int leftIndex = 0, rightIndex = keys.size() - 1, midIndex;
        while (leftIndex <= rightIndex) {
            midIndex = (leftIndex + rightIndex) / 2;
            long midKey = keys.get(midIndex);
            if (midKey == key) {
                return midIndex;
            } else if (midKey > key) {
                rightIndex = midIndex - 1;
            } else {
                leftIndex = midIndex + 1;
            }
        }
        return -1;
    }

    abstract byte[] remove(long key, Tree tree);

//    abstract protected void treeRemove();

    abstract void checkAfterInsert(Tree tree);

    abstract void checkAfterRemove(Tree tree);

    abstract void split(Tree tree);

    abstract void insertOrUpdate(long key, byte[] value, Tree tree);

    abstract void borrowOneFromPreviousBrother();

    abstract void borrowOneFromNextBrother();

    abstract void mergeIntoPreviousBrother(Tree tree);


    Node getPreviousBrother() {
        if (parent != null) {
            return parent.getPreviousChildOf(this);
        } else {
            return null;
        }
    }

    Node getNextBrother() {
        if (parent != null) {
            return parent.getNextChildOf(this);
        } else {
            return null;
        }
    }

    abstract int getTreeHeight();

    public int getNodeDepth() {
        if (parent == null) {
            return 1;
        } else {
            return parent.getNodeDepth() + 1;
        }
    }

    abstract void walk(int depth, int thisDepth);


    void checkRelationship() {

    }

    abstract LeafNode buildLeavesChain();
//    abstract void mergeIntoNextBrother();

//    abstract void mergeTwoNode(Node pre, Node node);
}
