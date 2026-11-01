package edu.groupb.courseta.ide;

import com.intellij.openapi.project.*;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.*;
import org.jetbrains.annotations.NotNull;

public final class TaToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final Key<TaPanel> PANEL=Key.create("course.ta.panel");
    public static final Key<SmartActionPanel> SMART_PANEL=Key.create("course.ta.smart.panel");
    public void createToolWindowContent(@NotNull Project project,@NotNull ToolWindow toolWindow) {
        TaPanel panel=new TaPanel(project); project.putUserData(PANEL,panel);
        var content=toolWindow.getContentManager().getFactory().createContent(panel.component(),"Course Q&A",false);
        content.setDisposer(panel); toolWindow.getContentManager().addContent(content);
        SmartActionPanel smart=new SmartActionPanel(project); project.putUserData(SMART_PANEL,smart);
        var smartContent=toolWindow.getContentManager().getFactory().createContent(smart.component(),"F1 Actions",false);
        smartContent.setDisposer(smart); toolWindow.getContentManager().addContent(smartContent);
    }
}
