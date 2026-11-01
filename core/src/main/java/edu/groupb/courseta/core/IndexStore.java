package edu.groupb.courseta.core;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class IndexStore {
    private static final Gson JSON = new Gson();
    private record Snapshot(int version, List<Chunk> chunks) { }
    public static void save(Path path, List<Chunk> chunks) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(path.toAbsolutePath().getParent(),"index-", ".tmp");
        try {
            Files.writeString(temp,JSON.toJson(new Snapshot(1,chunks)),StandardCharsets.UTF_8);
            try { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException ex) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static Bm25Index load(Path path) throws IOException {
        if(!Files.exists(path)) return new Bm25Index(List.of());
        try {
            Snapshot s=JSON.fromJson(Files.readString(path,StandardCharsets.UTF_8),Snapshot.class);
            if(s==null || s.version!=1 || s.chunks==null || s.chunks.stream().anyMatch(c->c==null || c.document()==null || c.location()==null || c.text()==null))
                throw new IOException("Invalid index. Rebuild the course index.");
            return new Bm25Index(s.chunks);
        } catch(RuntimeException ex) { throw new IOException("Cannot read index. Rebuild the course index.",ex); }
    }
    private IndexStore() { }
}
