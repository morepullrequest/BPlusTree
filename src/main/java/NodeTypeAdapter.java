import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class NodeTypeAdapter extends TypeAdapter<Node> {

    public void write(JsonWriter jsonWriter, Node node) throws IOException {

    }

    public Node read(JsonReader jsonReader) throws IOException {
        return null;
    }
}
