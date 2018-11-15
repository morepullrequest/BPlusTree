import java.util.Map;

public class BPlusData {
    public long key;
    public byte[] value;
    public String fileName;
    // public Map<byte[],byte[]> filedata;


    public BPlusData(long key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public BPlusData(long key) {
        this.key = key;
    }
}
