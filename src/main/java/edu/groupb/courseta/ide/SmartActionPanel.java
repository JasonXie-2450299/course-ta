package edu.groupb.courseta.ide;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.ToolWindowManager;
import edu.groupb.courseta.core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Review F1 drafts; comment insertions can be applied explicitly as an undoable edit. */
public final class SmartActionPanel implements Disposable {
    private final Project project;
    private final JPanel root = new JPanel(new BorderLayout(4, 4));
    private final JTextField repository = new JTextField();
    private final JTextArea context = area(false), result = area(true);
    private final JLabel status = new JLabel("Use Course Q&A > Settings to configure the model.");
    private final JButton tests = UiStyle.button("JUnit from selection", UiStyle.Tone.PRIMARY), commit = UiStyle.button("Generate commit message", UiStyle.Tone.PRIMARY);
    private final JButton preview = UiStyle.button("Preview staged diff", UiStyle.Tone.NEUTRAL), browse = UiStyle.button("Choose repository", UiStyle.Tone.NEUTRAL);
    private final JButton copy = UiStyle.button("Copy result", UiStyle.Tone.NEUTRAL), save = UiStyle.button("Save test as...", UiStyle.Tone.NEUTRAL), cancel = UiStyle.button("Cancel", UiStyle.Tone.NEUTRAL);
    private final JButton comments = UiStyle.button("Comments from selection", UiStyle.Tone.PRIMARY), apply = UiStyle.button("Apply comments", UiStyle.Tone.SUCCESS);
    private CommentGenerator.Draft commentDraft;
    private Document commentDocument;
    private VirtualFile commentFile;
    private Future<?> running;
    private boolean busy, disposed, testResult;
    private long generation;
    private record Result(String input, String output, String status, boolean test) { }

    public SmartActionPanel(Project project) {
        this.project = project;
        Disposer.register(project, this);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        repository.setText(project.getBasePath());
        repository.setToolTipText("Directory inside the repository. All staged changes in that repository are used.");
        // Each header row uses its own preferred height; the three-row button
        // group must not determine the height of labels or the repository field.
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0; row.gridy = GridBagConstraints.RELATIVE;
        row.weightx = 1; row.fill = GridBagConstraints.HORIZONTAL;
        row.insets = new Insets(0, 0, 3, 0);
        JLabel heading = new JLabel("Smart actions");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        top.add(heading, row);
        top.add(new JLabel("Git repository directory"), row); top.add(repository, row);
        JPanel actions = new JPanel(new GridLayout(0, 2, 4, 4));
        actions.add(tests); actions.add(commit); actions.add(browse); actions.add(preview); actions.add(comments);
        top.add(actions, row); top.add(status, row); root.add(top, BorderLayout.NORTH);
        context.setText("JUnit: select a Java class or method, then describe a test scenario.\n"
            + "Commit: stage your changes in Git, then preview or generate a message.\n"
            + "Comments: select Java code, generate a draft, then copy or apply the comments.\n\n"
            + "Generate sends the selected Java code plus its current file, or the complete staged diff, to your configured model.");
        context.setPreferredSize(new Dimension(640, 280));
        result.setPreferredSize(new Dimension(640, 360));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, framed("Input context", context), framed("Generated draft - review before use", result));
        split.setResizeWeight(0.4); split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder()); split.setDividerSize(8);
        root.add(split, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 2, 4, 4));
        bottom.add(copy); bottom.add(save); bottom.add(apply); bottom.add(cancel); root.add(bottom, BorderLayout.SOUTH);
        tests.addActionListener(e -> {
            var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            var file = editor == null ? null : FileDocumentManager.getInstance().getFile(editor.getDocument());
            GenerateJUnitAction.request(project, editor, file);
        });
        commit.addActionListener(e -> git(false)); preview.addActionListener(e -> git(true));
        comments.addActionListener(e -> {
            var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            var file = editor == null ? null : FileDocumentManager.getInstance().getFile(editor.getDocument());
            GenerateCommentsAction.request(project, editor, file);
        });
        apply.addActionListener(e -> applyComments());
        browse.addActionListener(e -> {
            var folder = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null);
            if (folder != null) repository.setText(folder.getPath());
        });
        copy.addActionListener(e -> CopyPasteManager.getInstance().setContents(new StringSelection(result.getText())));
        save.addActionListener(e -> saveTest());
        cancel.addActionListener(e -> { generation++; if (running != null) running.cancel(true); setBusy(false); status.setText("Cancelled"); });
        setBusy(false);
    }

    public JComponent component() { return root; }
    public static void open(Project project, Consumer<SmartActionPanel> action) {
        var window = ToolWindowManager.getInstance(project).getToolWindow("Course TA");
        if (window != null) window.activate(() -> {
            var panel = project.getUserData(TaToolWindowFactory.SMART_PANEL);
            if (panel != null) {
                for (var content : window.getContentManager().getContents())
                    if (content.getComponent() == panel.component()) window.getContentManager().setSelectedContent(content);
                action.accept(panel);
            }
        });
    }

    public void requestTest(SmartActions.JavaContext code) {
        if (busy) { Messages.showInfoMessage(project, "Wait for the current F1 action or cancel it first.", "Course TA"); return; }
        JTextArea scenario = new JTextArea(7, 56); scenario.setLineWrap(true); scenario.setWrapStyleWord(true);
        JPanel fields = new JPanel(new BorderLayout(6, 6));
        fields.add(new JLabel("Describe the scenario to test (JUnit 5):"), BorderLayout.NORTH);
        fields.add(new JScrollPane(scenario), BorderLayout.CENTER);
        fields.add(new JLabel("The selection and current Java file will be sent to the configured model."), BorderLayout.SOUTH);
        DialogWrapper dialog = new DialogWrapper(project) {
            { setTitle("Generate JUnit Tests - " + code.file()); setOKButtonText("Generate"); init(); }
            protected JComponent createCenterPanel() { return fields; }
            public JComponent getPreferredFocusedComponent() { return scenario; }
            protected ValidationInfo doValidate() {
                try { new SmartActions().testPrompt(code, scenario.getText()); return null; }
                catch (IllegalArgumentException ex) { return new ValidationInfo(ex.getMessage(), scenario); }
            }
        };
        if (!dialog.showAndGet()) return;
        String requirement = scenario.getText().strip();
        String input = "Scenario: " + requirement + "\n\n" + code.file() + " lines " + code.startLine() + "-" + code.endLine()
            + "\n" + code.selectedCode() + "\n\nCurrent file context:\n" + code.sourceCode();
        clearComments(); context.setText(input); result.setText(""); testResult = false;
        background(() -> new Result(input, new SmartActions().generateTests(code, requirement, config()),
            "JUnit draft generated. Review dependencies and run it in your project.", true), this::display);
    }

    public void generateCommit() { if (!busy) git(false); }
    private void git(boolean previewOnly) {
        String folder = repository.getText().strip();
        clearComments(); context.setText(""); result.setText(""); testResult = false;
        background(() -> {
            if (folder.isBlank()) throw new IllegalArgumentException("Choose a Git repository directory first.");
            var snapshot = new GitStagedChanges().read(Path.of(folder));
            String input = "Repository: " + snapshot.repository() + "\nComplete staged diff snapshot:\n" + snapshot.diff();
            String output = previewOnly ? "" : new SmartActions().generateCommit(snapshot.diff(), config());
            return new Result(input, output, previewOnly ? "Staged diff preview - no model request." : "Commit message draft generated. Re-generate if staged changes change.", false);
        }, this::display);
    }

    private LlmClient.Config config() {
        var service = project.getService(TaProjectService.class); var s = service.settings();
        return new LlmClient.Config(s.endpoint, s.model, service.apiKey());
    }
    public void requestComments(CommentGenerator.Selection selection, Document document, VirtualFile file) {
        if (busy) { Messages.showInfoMessage(project, "Wait for the current F1 action or cancel it first.", "Course TA"); return; }
        JComboBox<String> language = new JComboBox<>(new String[]{"Chinese", "English"});
        JPanel fields = new JPanel(new GridLayout(0, 1, 4, 4));
        fields.add(new JLabel("Comment language")); fields.add(language);
        fields.add(new JLabel("Complete selected lines and the current Java file are sent to the model."));
        fields.add(new JLabel("Review the draft, then copy it or click Apply comments. Undo is supported."));
        DialogWrapper dialog = new DialogWrapper(project) {
            { setTitle("Generate Comments - " + selection.file()); setOKButtonText("Generate"); init(); }
            protected JComponent createCenterPanel() { return fields; }
            protected ValidationInfo doValidate() {
                try { new CommentGenerator().prompt(selection, (String) language.getSelectedItem()); return null; }
                catch (IllegalArgumentException ex) { return new ValidationInfo(ex.getMessage()); }
            }
        };
        if (!dialog.showAndGet()) return;
        String selectedLanguage = (String) language.getSelectedItem();
        String input = selection.file() + " | " + selectedLanguage + " comments\nComplete selected lines:\n" + selection.code();
        clearComments(); context.setText(input); result.setText(""); testResult = false;
        background(() -> new CommentGenerator().generate(selection, selectedLanguage, config()), draft -> {
            commentDraft = draft; commentDocument = document; commentFile = file;
            display(new Result(input, draft.replacement(), draft.commentCount() + " comments generated. Review, then copy or apply.", false));
        });
    }
    private void applyComments() {
        if (busy || commentDraft == null || commentDocument == null) return;
        try {
            if (commentFile == null || !commentFile.isValid()) throw new IllegalStateException("The source file is no longer available.");
            // Check before asking for write access, then again atomically inside the write command.
            commentDraft.applyTo(commentDocument.getText());
            if (!FileDocumentManager.getInstance().requestWriting(commentDocument, project)) return;
            var draft = commentDraft; var document = commentDocument; var file = commentFile;
            WriteCommandAction.runWriteCommandAction(project, "Apply Course TA Comments", null, () -> {
                draft.applyTo(document.getText());
                document.replaceString(draft.selection().start(), draft.selection().end(), draft.replacement());
            });
            clearComments(); setBusy(false);
            status.setText("Comments applied. Use Undo in the source editor to revert.");
            FileEditorManager.getInstance(project).openFile(file, true);
        } catch (RuntimeException ex) {
            Messages.showInfoMessage(project, ex.getMessage() == null ? "Could not apply comments." : ex.getMessage(), "Course TA Comments");
        }
    }
    private void clearComments() { commentDraft = null; commentDocument = null; commentFile = null; apply.setEnabled(false); }
    private void display(Result value) {
        context.setText(value.input()); context.setCaretPosition(0);
        result.setText(value.output()); result.setCaretPosition(0);
        testResult = value.test(); status.setText(value.status()); status.setToolTipText(value.status()); setBusy(false);
    }
    private void saveTest() {
        var dialog = FileChooserFactory.getInstance().createSaveFileDialog(
            new FileSaverDescriptor("Save JUnit Test", "Choose a new .java file in your test source directory.", "java"), project);
        var selected = dialog.save(SmartActions.suggestedTestFile(result.getText()));
        if (selected == null) return;
        Path path = selected.getFile().toPath(); String text = result.getText();
        if (!path.getFileName().toString().endsWith(".java")) { status.setText("Choose a filename ending in .java."); return; }
        background(() -> {
            try { Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW); }
            catch (FileAlreadyExistsException ex) { throw new java.io.IOException("That file already exists. Choose a new filename."); }
            return path;
        }, saved -> {
            status.setText("Saved: " + saved); status.setToolTipText(saved.toString());
            LocalFileSystem.getInstance().refreshIoFiles(java.util.List.of(saved.toFile()), true, false, () -> {
                if (!project.isDisposed()) {
                    var file = LocalFileSystem.getInstance().findFileByIoFile(saved.toFile());
                    if (file != null) FileEditorManager.getInstance(project).openFile(file, true);
                }
            });
        });
    }
    private <T> void background(Callable<T> work, Consumer<T> success) {
        long ticket = ++generation; setBusy(true); status.setText("Working...");
        running = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                T value = work.call();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (disposed || project.isDisposed() || ticket != generation) return;
                    setBusy(false); success.accept(value);
                });
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (disposed || project.isDisposed() || ticket != generation) return;
                    setBusy(false); String message = ex.getMessage() == null ? "Operation did not complete." : ex.getMessage();
                    status.setText(message); status.setToolTipText(message);
                    Messages.showInfoMessage(project, message, "Course TA F1");
                });
            }
        });
    }
    private void setBusy(boolean value) {
        busy = value; for (JButton button : new JButton[]{tests, commit, preview, browse, comments}) button.setEnabled(!value);
        repository.setEnabled(!value); result.setEditable(!value && commentDraft == null); cancel.setEnabled(value);
        apply.setEnabled(!value && commentDraft != null);
        copy.setEnabled(!value && !result.getText().isBlank()); save.setEnabled(!value && testResult);
    }
    private static JTextArea area(boolean editable) {
        JTextArea text = new JTextArea(); text.setEditable(editable); text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setMargin(new Insets(10, 12, 10, 12));
        return text;
    }
    private static JPanel framed(String title, JTextArea text) {
        return UiStyle.section(title, text);
    }
    public void dispose() { disposed = true; if (running != null) running.cancel(true); clearComments(); }
}
