package edu.groupb.courseta.ide;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public final class GenerateCommitAction extends DumbAwareAction {
    public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    public void update(@NotNull AnActionEvent e) { e.getPresentation().setEnabledAndVisible(e.getProject() != null); }
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (e.getProject() != null) SmartActionPanel.open(e.getProject(), SmartActionPanel::generateCommit);
    }
}
