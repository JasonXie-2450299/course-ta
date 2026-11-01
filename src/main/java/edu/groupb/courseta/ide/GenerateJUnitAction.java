package edu.groupb.courseta.ide;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import edu.groupb.courseta.core.SmartActions;
import org.jetbrains.annotations.NotNull;

public final class GenerateJUnitAction extends DumbAwareAction {
    public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    public void update(@NotNull AnActionEvent e) {
        var editor = e.getData(CommonDataKeys.EDITOR); var file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && editor != null && file != null
            && "java".equalsIgnoreCase(file.getExtension()) && editor.getSelectionModel().hasSelection());
    }
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (e.getProject() != null) request(e.getProject(), e.getData(CommonDataKeys.EDITOR), e.getData(CommonDataKeys.VIRTUAL_FILE));
    }
    static void request(Project project, Editor editor, VirtualFile file) {
        if (editor == null || file == null || !"java".equalsIgnoreCase(file.getExtension()) || !editor.getSelectionModel().hasSelection()) {
            Messages.showInfoMessage(project, "Select a Java class or method in the editor first.", "Generate JUnit Tests"); return;
        }
        var selection = editor.getSelectionModel(); String code = selection.getSelectedText();
        var document = editor.getDocument();
        if (code == null || code.isBlank()) return;
        if (code.length() > SmartActions.MAX_SELECTION || document.getTextLength() > SmartActions.MAX_SOURCE) {
            Messages.showInfoMessage(project, "Select at most 12,000 characters in a Java file of at most 60,000 characters.", "Generate JUnit Tests"); return;
        }
        var context = new SmartActions.JavaContext(file.getName(), document.getLineNumber(selection.getSelectionStart()) + 1,
            document.getLineNumber(selection.getSelectionEnd() - 1) + 1, code, document.getText());
        SmartActionPanel.open(project, panel -> panel.requestTest(context));
    }
}
