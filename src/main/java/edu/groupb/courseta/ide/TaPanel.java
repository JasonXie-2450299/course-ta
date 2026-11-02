package edu.groupb.courseta.ide;
//TaPanel
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import edu.groupb.courseta.core.*;
import java.awt.*;
import java.util.concurrent.*;
import javax.swing.*;

public final class TaPanel implements Disposable {
    private final Project project;
    private final TaProjectService service;
    final ChatView view=new ChatView();
    private RagAssistant.Context context=RagAssistant.Context.empty();
    private Future<?> running;
    private volatile boolean disposed;
    private long generation;
    public TaPanel(Project project) {
        this.project=project; service=project.getService(TaProjectService.class);
        Disposer.register(project,this);
        view.configure.addActionListener(e->configure());
        view.index.addActionListener(e->importMaterials());
        view.send.addActionListener(e->ask(false)); view.preview.addActionListener(e->ask(true));
        view.detach.addActionListener(e->setContext(RagAssistant.Context.empty()));
        view.clear.addActionListener(e->{ view.transcript.setText(""); setContext(RagAssistant.Context.empty()); });
        view.cancel.addActionListener(e->{ generation++; if(running!=null) running.cancel(true); view.busy(false); view.status.setText("Cancelled"); });
        background(()->"Index loaded: " + service.index().chunks().size() + " chunks",s->view.status.setText(s));
    }
    public JComponent component() { return view; }
    public void setContext(RagAssistant.Context value) { context=value; view.context(value); view.question.requestFocusInWindow(); }
    public void attach(RagAssistant.Context value,String question) { setContext(value); view.question.setText(question); }

    private void ask(boolean preview) {
        String question=view.question.getText().strip();
        RagAssistant.Context selected=context;
        if(question.isBlank()) { view.status.setText("Enter a question first."); return; }
        view.append("You",question + (selected.code().isBlank()?"":"\nAttached: " + selected.file()+" lines " +selected.startLine()+"-"+selected.endLine()));
        background(()->{
            var assistant=new RagAssistant();
            if(preview) return assistant.preview(service.index(),question,selected);
            var s=service.settings();
            return assistant.ask(service.index(),question,selected,new LlmClient.Config(s.endpoint,s.model,service.apiKey()));
        },answer->{view.append("TA | " + answer.mode(),answer.text()); view.status.setText(answer.mode()+" | "+answer.sources().size()+" retrieved sources");});
    }

    private void importMaterials() {
        var descriptor=new FileChooserDescriptor(true,true,false,false,false,false)
            .withTitle("Import Course Materials")
            .withDescription("Choose a PDF, TXT or Markdown file, or a folder. This rebuilds the course index from your selection.")
            .withFileFilter(file->{
                String name=file.getName().toLowerCase(java.util.Locale.ROOT);
                return file.isDirectory() || name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".md");
            });
        VirtualFile source=FileChooser.chooseFile(descriptor,project,null);
        if(source==null) return;
        background(()->service.rebuild(source.getPath(),()->{if(Thread.currentThread().isInterrupted()) throw new CancellationException();}),result->{
            String status=result.filesRead()+" files | "+result.pagesRead()+" PDF pages | "+result.chunks().size()+" chunks";
            view.status.setText(status);
            view.append("Index",status + (result.warnings().isEmpty()?"":"\nWarnings\n" +String.join("\n",result.warnings())));
        });
    }

    private void configure() {
        var s=service.settings();
        JTextField endpoint=new JTextField(s.endpoint,38), model=new JTextField(s.model,26);
        JPasswordField key=new JPasswordField(26);
        JCheckBox remove=new JCheckBox("Remove stored key");
        JPanel fields=new JPanel(new GridLayout(0,1,0,6));
        fields.add(new JLabel("Full chat-completions endpoint")); fields.add(endpoint);
        fields.add(new JLabel("Model name")); fields.add(model);
        fields.add(new JLabel("New API key (leave blank to keep the stored key)")); fields.add(key); fields.add(remove);
        fields.add(new JLabel("Questions, selected code and retrieved excerpts are sent on Ask TA."));
        DialogWrapper dialog=new DialogWrapper(project) {
            { setTitle("Course TA Settings"); init(); }
            protected JComponent createCenterPanel() { return fields; }
            protected ValidationInfo doValidate() {
                try { LlmClient.validateEndpoint(endpoint.getText()); }
                catch(IllegalArgumentException ex) { return new ValidationInfo(ex.getMessage(),endpoint); }
                if(model.getText().isBlank()) return new ValidationInfo("Enter a model name.",model);
                return null;
            }
        };
        if(dialog.showAndGet()) {
            s.endpoint=endpoint.getText().strip(); s.model=model.getText().strip();
            char[] secret=key.getPassword(); String value=new String(secret); java.util.Arrays.fill(secret,'\0'); key.setText("");
            background(()->{if(remove.isSelected()) service.saveKey(""); else if(!value.isBlank()) service.saveKey(value); return "Settings saved";},text->view.status.setText(text));
        }
    }

    private <T> void background(Callable<T> action, java.util.function.Consumer<T> success) {
        long ticket=++generation; view.busy(true); view.status.setText("Working...");
        running=ApplicationManager.getApplication().executeOnPooledThread(()->{
            try {
                T value=action.call();
                ApplicationManager.getApplication().invokeLater(()->{ if(disposed || project.isDisposed() || ticket!=generation) return; view.busy(false); success.accept(value); });
            } catch(Exception ex) {
                String message=ex instanceof InterruptedException || ex instanceof CancellationException?"Cancelled":ex.getMessage();
                ApplicationManager.getApplication().invokeLater(()->{
                    if(disposed || project.isDisposed() || ticket!=generation) return;
                    view.busy(false); view.status.setText("Request did not complete"); view.append("Notice",message==null?"Operation failed. Check settings and try again.":message);
                });
                if(ex instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        });
    }
    @Override public void dispose() { disposed=true; if(running!=null) running.cancel(true); }
}
