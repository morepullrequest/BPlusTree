import com.google.gson.annotations.Expose;

import java.util.List;

abstract public class InternalNode extends Node {
    @Expose
    public List<Node> children;

    abstract public int getChildIndexMayContainKey(long key);

    abstract public Node getChildMayContainKey(long key);

    abstract int getChildIndex(Node child);

    abstract int getKeyIndexOfChild(Node child);

    abstract Node getPreviousChildOf(Node child);

    abstract Node getNextChildOf(Node child);


}
