package edu.groupb.courseta.core;

import com.sun.net.httpserver.HttpServer;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @TempDir Path dir;
    private Bm25Index fixture() { return new Bm25Index(List.of(
        new Chunk("oop.pdf","p. 2","Polymorphism enables dynamic dispatch of overridden methods."),
        new Chunk("errors.pdf","p. 3","Checked exceptions require handling with catch or a throws declaration."),
        new Chunk("arrays.pdf","p. 4","An array stores elements accessible by a numeric index."),
        new Chunk("other.pdf","p. 1","Constructors initialize fields of new objects.")));
    }
    @Test void pdfExtractionPreservesEveryPage() throws Exception {
        try(var pdf=new PDDocument()) {
            for(String text:List.of("Polymorphism and inheritance.","Checked exceptions and throws.")) {
                var page=new PDPage(); pdf.addPage(page);
                try(var c=new PDPageContentStream(pdf,page)) { c.beginText(); c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12); c.newLineAtOffset(50,700); c.showText(text); c.endText(); }
            }
            pdf.save(dir.resolve("lecture.pdf").toFile());
        }
        var r=new DocumentIngestor().read(dir,()->{});
        assertEquals(2,r.pagesRead()); assertEquals(1,r.filesRead()); assertEquals(2,r.chunks().size());
        assertEquals("p. 2",r.chunks().get(1).location()); assertTrue(r.chunks().get(1).text().contains("throws"));
    }
    @Test void selectedPdfImportsOnlyThatFileAndPreservesPageReferences() throws Exception {
        Path selected=dir.resolve("课程 notes.PDF");
        try(var pdf=new PDDocument()) {
            for(String text:List.of("Polymorphism and inheritance.","Checked exceptions and throws.")) {
                var page=new PDPage(); pdf.addPage(page);
                try(var c=new PDPageContentStream(pdf,page)) {
                    c.beginText(); c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                    c.newLineAtOffset(50,700); c.showText(text); c.endText();
                }
            }
            pdf.save(selected.toFile());
        }
        Files.writeString(dir.resolve("unselected.txt"),"This file must not be imported.");
        var r=new DocumentIngestor().read(selected,()->{});
        assertEquals(1,r.filesRead()); assertEquals(2,r.pagesRead()); assertEquals(2,r.chunks().size());
        assertTrue(r.warnings().isEmpty());
        assertTrue(r.chunks().stream().allMatch(c->c.document().equals("课程 notes.PDF")));
        assertEquals("p. 2",r.chunks().get(1).location());
        assertTrue(r.chunks().get(1).text().contains("throws"));
    }
    @Test void selectedMarkdownImportsWithLineReferences() throws Exception {
        Path selected=dir.resolve("notes.md");
        Files.writeString(selected,"第一行\nChecked exceptions need handling.\n",StandardCharsets.UTF_8);
        var r=new DocumentIngestor().read(selected,()->{});
        assertEquals(1,r.filesRead()); assertEquals(0,r.pagesRead());
        assertEquals("notes.md",r.chunks().getFirst().document());
        assertEquals("lines 1-2",r.chunks().getFirst().location());
        assertTrue(r.chunks().getFirst().text().contains("第一行"));
    }
    @Test void selectedUnsupportedFileExplainsAcceptedFormats() throws Exception {
        Path selected=dir.resolve("image.png"); Files.writeString(selected,"not course text");
        var ex=assertThrows(IOException.class,()->new DocumentIngestor().read(selected,()->{}));
        assertTrue(ex.getMessage().contains("PDF, TXT or Markdown"));
    }
    @Test void chunksNeverExceedBudgetOrLoseTail() {
        List<Chunk> chunks=new ArrayList<>();
        DocumentIngestor.splitPage("long.pdf","p. 5","abc ".repeat(1200)+"THE_END",chunks);
        assertTrue(chunks.size()>1); assertTrue(chunks.stream().allMatch(c->c.text().length()<=1400 && c.location().equals("p. 5")));
        assertTrue(chunks.getLast().text().endsWith("THE_END"));
        assertTrue(chunks.get(1).text().startsWith(chunks.getFirst().text().substring(1200).strip()));
    }
    @Test void utf8TextUsesLineScopeAndReportsCorruptPdf() throws Exception {
        Files.writeString(dir.resolve("notes.md"),"第一行\nExceptions must be handled.\n",StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("broken.pdf"),"not a PDF");
        var r=new DocumentIngestor().read(dir,()->{});
        assertEquals(1,r.filesRead()); assertEquals("lines 1-2",r.chunks().getFirst().location());
        assertTrue(r.chunks().getFirst().text().contains("第一行")); assertEquals(1,r.warnings().size());
    }
    @Test void cancelledImportDoesNotReturnPartialResult() throws Exception {
        Files.writeString(dir.resolve("a.txt"),"hello");
        assertThrows(CancellationException.class,()->new DocumentIngestor().read(dir,()->{throw new CancellationException();}));
    }
    @Test void retrievalRanksRelevantPage() {
        var hits=fixture().search("Explain checked exceptions",4);
        assertEquals("errors.pdf",hits.getFirst().chunk().document()); assertEquals("p. 3",hits.getFirst().chunk().location());
    }
    @Test void unrelatedQuestionReturnsNoEvidence() { assertTrue(fixture().search("astronomy galaxy telescope",4).isEmpty()); }
    @Test void chineseTopicAliasFindsEnglishMaterial() { assertEquals("oop.pdf",fixture().search("什么是多态",4).getFirst().chunk().document()); }
    @Test void indexSurvivesRoundTrip() throws Exception {
        Path path=dir.resolve("index.json"); IndexStore.save(path,fixture().chunks());
        assertEquals(fixture().search("polymorphism",4),IndexStore.load(path).search("polymorphism",4));
    }
    @Test void corruptIndexRaisesActionableError() throws Exception {
        Path path=dir.resolve("index.json"); Files.writeString(path,"broken");
        assertThrows(IOException.class,()->IndexStore.load(path));
    }
    @Test void promptIncludesCodeAndBoundedEvidence() {
        var p=new RagAssistant().prompt(fixture(),"checked exceptions",new RagAssistant.Context("Demo.java",10,11,"throw new Exception();"));
        assertTrue(p.user().contains("Demo.java lines 10-11")); assertTrue(p.user().contains("throw new Exception();"));
        assertTrue(p.user().contains("errors.pdf | p. 3")); assertTrue(p.sources().size()<=4);
    }
    @Test void oversizedInputRejected() {
        assertThrows(IllegalArgumentException.class,()->new RagAssistant().prompt(fixture(),"x".repeat(6001),RagAssistant.Context.empty()));
        assertThrows(IllegalArgumentException.class,()->new RagAssistant().prompt(fixture(),"explain",new RagAssistant.Context("X",1,2,"x".repeat(12001))));
    }
    @Test void invalidAndMissingCitationsWithholdAnswer() {
        var assistant=new RagAssistant(); var hits=fixture().search("checked exceptions",4);
        assertEquals("Citation validation failed",assistant.validate("Incorrect reference [S99]",hits).mode());
        assertEquals("Citation validation failed",assistant.validate("An uncited assertion",hits).mode());
        assertEquals("Citation validation failed",assistant.validate("Wrong [S99999999999999999]",hits).mode());
    }
    @Test void validCitationsResolveToDocumentAndPage() {
        var a=new RagAssistant().validate("Handle checked exceptions [S1].",fixture().search("checked exceptions",4));
        assertEquals("Course grounded",a.mode()); assertTrue(a.text().contains("errors.pdf | p. 3"));
    }
    @Test void emptyEvidenceAlwaysHasGeneralKnowledgeNotice() {
        var a=new RagAssistant().validate("An explanation.",List.of());
        assertTrue(a.text().startsWith(RagAssistant.GENERAL)); assertEquals("General knowledge",a.mode());
    }
    @Test void previewDoesNotPretendToBeLlmAnswer() { assertTrue(new RagAssistant().preview(fixture(),"polymorphism",RagAssistant.Context.empty()).text().contains("no LLM answer")); }
    @Test void insecureRemoteEndpointsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->LlmClient.validateEndpoint("http://example.com/chat/completions"));
        assertThrows(IllegalArgumentException.class,()->LlmClient.validateEndpoint("https://user:pass@example.com/chat/completions"));
        assertDoesNotThrow(()->LlmClient.validateEndpoint("http://127.0.0.1:1234/v1/chat/completions"));
    }
    @Test void httpIntegrationSendsGroundedPromptAndParsesResponse() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var request=new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/chat/completions",exchange->{
            request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] json="{\"choices\":[{\"message\":{\"content\":\"Checked exceptions require handling [S1].\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,json.length); exchange.getResponseBody().write(json); exchange.close();
        }); server.start();
        try {
            var cfg=new LlmClient.Config("http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions","test-model","");
            var a=new RagAssistant().ask(fixture(),"checked exceptions",RagAssistant.Context.empty(),cfg);
            assertEquals("Course grounded",a.mode()); assertTrue(request.get().contains("errors.pdf | p. 3")); assertTrue(request.get().contains("test-model"));
        } finally { server.stop(0); }
    }
    @Test void httpFailuresDoNotLeakProviderBody() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat/completions",e->{byte[] b="sensitive provider body".getBytes();e.sendResponseHeaders(401,b.length);e.getResponseBody().write(b);e.close();}); server.start();
        try {
            var cfg=new LlmClient.Config("http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions","test","");
            var ex=assertThrows(IOException.class,()->new LlmClient().complete(cfg,"system","question"));
            assertTrue(ex.getMessage().contains("authentication")); assertFalse(ex.getMessage().contains("sensitive"));
        } finally {server.stop(0);}
    }
}
