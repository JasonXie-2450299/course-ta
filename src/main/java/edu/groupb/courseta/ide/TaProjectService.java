package edu.groupb.courseta.ide;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import edu.groupb.courseta.core.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class TaProjectService {
    private final Project project;
    private volatile Bm25Index index=new Bm25Index(List.of());
    private volatile String loadedFolder="";
    private volatile boolean loaded;
    public TaProjectService(Project project) { this.project=project; }
    public TaSettings.Data settings() { return project.getService(TaSettings.class).getState(); }
    private Path indexFile() {
        String base=project.getBasePath();
        if(base==null) throw new IllegalStateException("Open a local project first.");
        return Path.of(base,".idea","course-ta","index.json");
    }
    public synchronized Bm25Index index() throws IOException {
        if(!loaded) { index=IndexStore.load(indexFile()); loadedFolder=settings().folder; loaded=true; }
        return index;
    }
    public synchronized DocumentIngestor.ImportResult rebuild(String source, Runnable cancellation) throws IOException {
        var result=new DocumentIngestor().read(Path.of(source),cancellation);
        if(result.chunks().isEmpty()) throw new IOException("No usable text found. The previous index was retained. " + String.join(" ",result.warnings()));
        cancellation.run();
        IndexStore.save(indexFile(),result.chunks());
        index=new Bm25Index(result.chunks()); loadedFolder=source; loaded=true;
        settings().folder=source;
        return result;
    }
    public String sourceFolder() { return loadedFolder; }
    private CredentialAttributes attributes() { return new CredentialAttributes("Course TA API | " + settings().endpoint); }
    public String apiKey() { return PasswordSafe.getInstance().getPassword(attributes()); }
    public void saveKey(String value) { PasswordSafe.getInstance().setPassword(attributes(),value.isBlank()?null:value); }
}
