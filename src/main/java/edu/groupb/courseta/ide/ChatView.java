package edu.groupb.courseta.ide;

import edu.groupb.courseta.core.RagAssistant;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Plain Swing view, shared by the IDE and the reproducible UI sample renderer. */
public final class ChatView extends JPanel {
    final JTextArea transcript=new JTextArea();
    final JTextArea question=new JTextArea(4,30);
    final JTextArea codePreview=new JTextArea(3,30);
    final JLabel status=new JLabel("Import course materials to begin");
    final JLabel contextLabel=new JLabel("No selected code");
    final JButton configure=UiStyle.button("Settings", UiStyle.Tone.NEUTRAL);
    final JButton index=UiStyle.button("Import / Rebuild", UiStyle.Tone.NEUTRAL);
    final JButton send=UiStyle.button("Ask TA", UiStyle.Tone.PRIMARY);
    final JButton preview=UiStyle.button("Preview retrieval", UiStyle.Tone.NEUTRAL);
    final JButton cancel=UiStyle.button("Cancel", UiStyle.Tone.NEUTRAL);
    final JButton clear=UiStyle.button("Clear chat", UiStyle.Tone.NEUTRAL);
    final JButton detach=UiStyle.button("Clear code", UiStyle.Tone.NEUTRAL);

    public ChatView() {
        super(new BorderLayout(0,10));
        setBorder(new EmptyBorder(12,12,12,12));
        JPanel header=new JPanel(new BorderLayout(0,8));
        JLabel title=new JLabel("Course TA"); title.setFont(title.getFont().deriveFont(Font.BOLD,22));
        header.add(title,BorderLayout.NORTH);
        JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        index.setToolTipText("Import a PDF, TXT or Markdown file, or rebuild from a course-material folder.");
        tools.add(configure); tools.add(index); tools.add(clear); header.add(tools,BorderLayout.CENTER);
        header.add(status,BorderLayout.SOUTH); add(header,BorderLayout.NORTH);
        transcript.setEditable(false); transcript.setLineWrap(true); transcript.setWrapStyleWord(true);
        transcript.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14)); transcript.setMargin(new Insets(12,12,12,12));
        transcript.setText("CST3492 Java Enterprise Application Development\n\nAsk about the course or right-click selected code in the editor.\n\nCourse answers include document and page references. Preview retrieval works without an API key.\n\nAsk TA sends your question, attached code and up to four course excerpts to the configured LLM endpoint.\n");
        add(UiStyle.section("Conversation", transcript),BorderLayout.CENTER);
        JPanel compose=new JPanel(new BorderLayout(0,6));
        JPanel context=new JPanel(new BorderLayout()); context.add(contextLabel,BorderLayout.CENTER); context.add(detach,BorderLayout.EAST);
        JPanel entry=new JPanel(new BorderLayout(0,6));
        codePreview.setEditable(false); codePreview.setLineWrap(true); codePreview.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        codePreview.setToolTipText("Only the selected code is attached; the rest of the file is not sent.");
        entry.add(new JScrollPane(codePreview),BorderLayout.NORTH);
        question.setLineWrap(true); question.setWrapStyleWord(true); question.setMargin(new Insets(8,8,8,8));
        question.setToolTipText("Ask a course question in English or Chinese.");
        entry.add(UiStyle.section("Your question", question),BorderLayout.CENTER);
        compose.add(context,BorderLayout.NORTH); compose.add(entry,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));
        buttons.add(preview); buttons.add(cancel); buttons.add(send); cancel.setEnabled(false);
        compose.add(buttons,BorderLayout.SOUTH); add(compose,BorderLayout.SOUTH);
        transcript.getAccessibleContext().setAccessibleName("Conversation and source citations");
        question.getAccessibleContext().setAccessibleName("Question to Course TA");
    }
    public void append(String role,String text) { transcript.append("\n" + role + "\n" + text + "\n"); transcript.setCaretPosition(transcript.getDocument().getLength()); }
    public void context(RagAssistant.Context context) {
        contextLabel.setText(context.code().isBlank()?"No selected code":context.file()+" : "+context.startLine()+"-"+context.endLine());
        codePreview.setText(context.code()); codePreview.setCaretPosition(0);
    }
    public void busy(boolean busy) {
        send.setEnabled(!busy); preview.setEnabled(!busy); index.setEnabled(!busy); configure.setEnabled(!busy);
        clear.setEnabled(!busy); detach.setEnabled(!busy); question.setEnabled(!busy); cancel.setEnabled(busy);
    }
}
