import java.io.IOException;

public class Demo {
    public static String readUser(String id) throws IOException {
        if (id == null || id.isBlank()) {
            throw new IOException("User ID is required");
        }
        return "User: " + id;
    }
}
