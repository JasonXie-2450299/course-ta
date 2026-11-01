package edu.groupb.courseta.ide;

import edu.groupb.courseta.core.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Renders the production Swing component offscreen, not an IDE screenshot. */
public final class UiSampleRenderer {
    public static void main(String[] args) throws Exception {
        var index=IndexStore.load(Path.of(args[0])); Path out=Path.of(args[1]); Files.createDirectories(out);
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        SwingUtilities.invokeAndWait(()->{
            try {
                var view=new ChatView(); view.setSize(720,920);
                view.status.setText("5 lecture PDFs | 115 pages | " + index.chunks().size() + " chunks");
                String q="What is polymorphism?";
                view.transcript.setText(""); view.append("You",q);
                view.append("TA | Retrieval preview",new RagAssistant().preview(index,q,RagAssistant.Context.empty()).text());
                view.transcript.setCaretPosition(0); view.question.setText("How does method overriding relate to polymorphism?");
                render(view,out.resolve("retrieval-preview.png"));
                view.context(new RagAssistant.Context("Demo.java",4,8,"public static String readUser(String id) throws IOException {\n    if (id == null || id.isBlank()) {\n        throw new IOException(\"User ID is required\");\n    }\n}"));
                view.question.setText("Why does this method declare throws IOException?");
                view.transcript.setText("CST3492 selected-code question\n\nThe attached selection is shown below.\nClick Ask TA to submit it with your question.\n\nOnly selected text, the filename and line range are attached.\n\nPreview retrieval can inspect related course excerpts without an LLM request.");
                render(view,out.resolve("selected-code.png"));
                System.out.println("Rendered two production Swing UI samples to " + out);
            } catch(Exception ex) {throw new RuntimeException(ex);}
        });
    }
    private static void layout(Container container) {container.doLayout(); for(Component c:container.getComponents()) if(c instanceof Container child)layout(child);}
    private static void render(ChatView view,Path path) throws Exception {
        layout(view); var image=new BufferedImage(view.getWidth()*2,view.getHeight()*2,BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics(); graphics.scale(2,2); view.printAll(graphics); graphics.dispose();
        ImageIO.write(image,"png",path.toFile());
    }
}
