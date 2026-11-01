package edu.groupb.courseta.ide;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowManager;
import edu.groupb.courseta.core.RagAssistant;
import org.jetbrains.annotations.NotNull;

public final class AskSelectedCodeAction extends DumbAwareAction {
    public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    public void update(@NotNull AnActionEvent e) {
        var editor=e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(e.getProject()!=null && editor!=null && editor.getSelectionModel().hasSelection());
    }
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project=e.getProject(); var editor=e.getData(CommonDataKeys.EDITOR); var file=e.getData(CommonDataKeys.VIRTUAL_FILE);
        if(project==null || editor==null) return;
        var selection=editor.getSelectionModel(); String code=selection.getSelectedText();
        if(code==null || code.isBlank()) return;
        if(code.length()>12000) { Messages.showInfoMessage(project,"Select at most 12,000 characters.","Course TA"); return; }
        String question=Messages.showInputDialog(project,"What would you like to understand about this code?","Ask Course TA",null,"Explain this code using the course material.",null);
        if(question==null || question.isBlank()) return;
        int start=editor.getDocument().getLineNumber(selection.getSelectionStart())+1;
        int end=editor.getDocument().getLineNumber(Math.max(selection.getSelectionStart(),selection.getSelectionEnd()-1))+1;
        var context=new RagAssistant.Context(file==null?"Selected code":file.getName(),start,end,code);
        var window=ToolWindowManager.getInstance(project).getToolWindow("Course TA");
        if(window!=null) window.activate(()->{ var panel=project.getUserData(TaToolWindowFactory.PANEL); if(panel!=null) {
            for(var content:window.getContentManager().getContents())
                if(content.getComponent()==panel.component()) window.getContentManager().setSelectedContent(content);
            panel.attach(context,question);
        } });
    }
}
