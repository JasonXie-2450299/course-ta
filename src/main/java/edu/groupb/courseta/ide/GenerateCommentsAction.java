package edu.groupb.courseta.ide;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import edu.groupb.courseta.core.*;
import org.jetbrains.annotations.NotNull;

public final class GenerateCommentsAction extends DumbAwareAction {
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
            Messages.showInfoMessage(project, "Select Java code in the editor first.", "Generate Comments"); return;
        }
        var document = editor.getDocument(); var selection = editor.getSelectionModel();
        // Include complete physical lines so comments cannot split a Java token.
        int firstLine = document.getLineNumber(selection.getSelectionStart());
        int lastLine = document.getLineNumber(selection.getSelectionEnd() - 1);
        int start = document.getLineStartOffset(firstLine), end = document.getLineEndOffset(lastLine);
        if (end - start > SmartActions.MAX_SELECTION || document.getTextLength() > SmartActions.MAX_SOURCE) {
            Messages.showInfoMessage(project, "Select at most 12,000 characters in a Java file of at most 60,000 characters.", "Generate Comments"); return;
        }
        var code = new CommentGenerator.Selection(file.getName(), document.getText(), start, end);
        SmartActionPanel.open(project, panel -> panel.requestComments(code, document, file));
    }
}
