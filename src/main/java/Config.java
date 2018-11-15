import java.io.File;

public class Config {
    public static final int order = 250; // file max size around 1MB
    public static final String dataDir = "data";
    public static final String indexDir = "index";
    public static final String seperator = "/";
    public static final String indexFilename = indexDir + seperator + "index.json";

    static {
        init();
    }

    public static void init() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        dir = new File(indexDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
}
