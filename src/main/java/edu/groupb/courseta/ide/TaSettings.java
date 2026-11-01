package edu.groupb.courseta.ide;

import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

@State(name="CourseTaSettings",storages=@Storage("course-ta.xml"))
@Service(Service.Level.PROJECT)
public final class TaSettings implements PersistentStateComponent<TaSettings.Data> {
    public static final class Data {
        // Retain the persisted field name; it can now hold a single file or a folder.
        public String folder="";
        public String endpoint="https://api.deepseek.com/chat/completions";
        public String model="deepseek-chat";
    }
    private Data state=new Data();
    public Data getState() { return state; }
    public void loadState(@NotNull Data value) { state=value; }
}
