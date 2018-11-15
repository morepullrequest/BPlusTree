import com.google.gson.annotations.Expose;

public class BPlusData {
    @Expose
    public long key;

    public byte[] value;


    public BPlusData(long key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public BPlusData(long key) {
        this.key = key;
    }
}
