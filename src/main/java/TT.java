import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TT {
    public static void main(String[] args) {
        int order = 5;
        int size = 100;
        testInsert(order, size);
    }

    public static void testInsert(int order, int size) {
        Tree tree = new Tree(order);
        for (int i = 0; i < size; i++) {
            tree.insertOrUpdate((long) i, Util.long2Bytes((long) i));
            tree.walk();
        }
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();

        gson.toJson(tree, System.out);
    }
}
