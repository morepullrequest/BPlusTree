import com.google.gson.annotations.Expose;

import java.util.List;

abstract public class LeafNode extends Node {
    public LeafNode previous; // 前一个节点 leaf

    public LeafNode next; // 后一个节点 leaf
    @Expose
    public String filename;

    abstract int getInsertIndexOf(long key);

    abstract KeyValue getOneByKey(long key);

    abstract KeyValue getOneByKeyIndex(int keyIndex);

    abstract KeyValue removeOneByKeyIndex(int keyIndex);

    abstract KeyValue removeOneByKey(long key);

    abstract protected void saveOne(long key, byte[] value);

    abstract void saveOne(KeyValue kv);

    abstract DatabaseSliceOuterClass.DatabaseSlice readSliceFromStorage();

    abstract void saveSliceToStorage(DatabaseSliceOuterClass.DatabaseSlice slice);


    abstract List<KeyValue> slice(int startIndex, int endIndex);

    abstract void saveAll(List<KeyValue> keyValueList);

    // destroty() // delete file

}
