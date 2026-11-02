package edu.groupb.courseta.core;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class CommentGeneratorTest {
    @TempDir Path temp;
    private final CommentGenerator generator = new CommentGenerator();
    private static final String SOURCE = "public class Sample {\n    public static int twice(int n) {\n        return n * 2;\n    }\n}";
    private CommentGenerator.Selection full(String source) { return new CommentGenerator.Selection("Sample.java", source, 0, source.length()); }
    private String response(int line, String kind, String text) {
        return new Gson().toJson(Map.of("comments", List.of(Map.of("line", line, "kind", kind, "text", text))));
    }
    @Test void promptContainsNumberedSelectionContextAndLanguage() {
        String source = "class X {\n    String value = \"\"\"\n        literal\n        \"\"\";\n}";
        var p = generator.prompt(full(source), "Chinese");
        assertTrue(p.system().contains("Chinese")); assertTrue(p.system().contains("untrusted data"));
        assertTrue(p.user().contains("2 [allowed]")); assertTrue(p.user().contains("3 [do not insert]"));
        assertTrue(p.user().contains(source));
        assertThrows(IllegalArgumentException.class, () -> generator.prompt(full(SOURCE), "Unknown"));
    }
    @Test void insertsJavadocAndBlockCommentsWhilePreservingEveryOriginalCharacter() throws Exception {
        String json = "{\"comments\":[{\"line\":2,\"kind\":\"javadoc\",\"text\":\"计算输入的两倍。\\n@param n 输入整数\\n@return 输入的两倍\"},"
            + "{\"line\":3,\"kind\":\"block\",\"text\":\"Multiply without changing the input.\"}]}";
        var draft = generator.validate(full(SOURCE), json);
        String expected = SOURCE.replace("    public static", "    /**\n     * 计算输入的两倍。\n     * @param n 输入整数\n     * @return 输入的两倍\n     */\n    public static")
            .replace("        return", "        /*\n         * Multiply without changing the input.\n         */\n        return");
        assertEquals(expected, draft.replacement()); assertEquals(expected, draft.applyTo(SOURCE)); assertEquals(2, draft.commentCount());
    }
    @Test void subsetUsesRelativeLinesAndPreservesCodeOutsideSelection() throws Exception {
        int start = SOURCE.indexOf("    public"), end = SOURCE.indexOf("\n}");
        var selection = new CommentGenerator.Selection("Sample.java", SOURCE, start, end);
        var draft = generator.validate(selection, response(1, "javadoc", "Returns twice the input."));
        assertEquals(SOURCE.substring(0, start) + draft.replacement() + SOURCE.substring(end), draft.applyTo(SOURCE));
        assertTrue(draft.replacement().endsWith(selection.code()));
    }
    @Test void staleDocumentIsRejectedIncludingEditsOutsideSelection() throws Exception {
        var selection = new CommentGenerator.Selection("Sample.java", SOURCE, SOURCE.indexOf("    public"), SOURCE.indexOf("\n}"));
        var draft = generator.validate(selection, response(1, "javadoc", "Double the number."));
        assertThrows(IllegalStateException.class, () -> draft.applyTo(SOURCE.replace("Sample", "Renamed")));
        assertThrows(IllegalStateException.class, () -> draft.applyTo(SOURCE.replace("n * 2", "n * 3")));
    }
    @Test void existingCommentsAreKeptAndInsertingInsideThemIsRejected() throws Exception {
        String source = "/* Existing\n * documentation\n */\nint n = 1; // retain this too";
        var draft = generator.validate(full(source), response(4, "block", "Initialize n."));
        assertTrue(draft.replacement().startsWith("/* Existing\n * documentation\n */\n"));
        assertTrue(draft.replacement().endsWith("int n = 1; // retain this too"));
        assertThrows(IOException.class, () -> generator.validate(full(source), response(2, "block", "unsafe")));
        assertThrows(IOException.class, () -> generator.validate(full(source), response(1, "block", "duplicate doc")));
    }
    @Test void commentsCannotBeInsertedInsideTextBlocks() {
        String source = "String s = \"\"\"\n    // this is literal text\n    \"\"\";\nint n = 1;";
        assertThrows(IOException.class, () -> generator.validate(full(source), response(2, "block", "would change string")));
        assertThrows(IOException.class, () -> generator.validate(full(source), response(3, "block", "would change string")));
        assertDoesNotThrow(() -> generator.validate(full(source), response(4, "block", "Initialize n.")));
    }
    @Test void commentLikeTextInStringsAndEscapedCharactersDoesNotConfuseBoundaryChecks() {
        String source = "String url = \"http://example.test/\\\"/*\";\nchar quote = '\\'';\nint n = 1;";
        assertDoesNotThrow(() -> generator.validate(full(source), response(3, "block", "Initialize n.")));
    }
    @Test void rejectsCommentBreakoutUnicodeAndControlCharacters() {
        for (String text : List.of("*/ System.exit(1); /*", "\\" + "u002a/ injected", "bad\u0000text", "bad\rtext"))
            assertThrows(IOException.class, () -> generator.validate(full(SOURCE), response(2, "block", text)));
    }
    @Test void rejectsRewrittenCodeInvalidJsonAndMissingComments() {
        for (String value : List.of("class Sample {}", "{\"comments\":[]}", "{\"comments\":[],\"code\":\"modified\"}", "{}"))
            assertThrows(IOException.class, () -> generator.validate(full(SOURCE), value));
    }
    @Test void validatesLineNumbersKindsAndDuplicateInsertions() {
        for (String value : List.of(response(0,"block","x"), response(99,"block","x"), response(2,"code","x"),
            "{\"comments\":[{\"line\":1.5,\"kind\":\"block\",\"text\":\"x\"}]}",
            "{\"comments\":[{\"line\":2,\"kind\":\"block\",\"text\":\"x\"},{\"line\":2,\"kind\":\"block\",\"text\":\"y\"}]}"))
            assertThrows(IOException.class, () -> generator.validate(full(SOURCE), value));
    }
    @Test void contextAndResponseLimitsRejectOversizeInsteadOfTruncating() {
        assertThrows(IllegalArgumentException.class, () -> generator.prompt(full("x".repeat(12001)), "Chinese"));
        assertThrows(IllegalArgumentException.class, () -> generator.prompt(
            new CommentGenerator.Selection("X.java", "x".repeat(60001), 0, 1), "Chinese"));
        assertThrows(IOException.class, () -> generator.validate(full(SOURCE), response(2, "block", "x".repeat(1501))));
        assertThrows(IOException.class, () -> generator.validate(full(SOURCE), "x".repeat(80001)));
    }
    @Test void partialLinesUnicodeEscapesAndBlankSelectionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> generator.prompt(new CommentGenerator.Selection("X.java", SOURCE, 3, 8), "Chinese"));
        assertThrows(IllegalArgumentException.class, () -> generator.prompt(full("String s = \"\\" + "u000a\";"), "Chinese"));
        assertThrows(IllegalArgumentException.class, () -> generator.prompt(full("  "), "Chinese"));
    }
    @Test void acceptsJsonFenceWithoutAddingItToSource() throws Exception {
        var draft = generator.validate(full(SOURCE), "```json\n" + response(2, "javadoc", "Double n.") + "\n```");
        assertFalse(draft.replacement().contains("```")); assertEquals(1, draft.commentCount());
    }
    @Test void generatedCommentsCompileAndKeepObservedBehavior() throws Exception {
        var draft = generator.validate(full(SOURCE), response(2, "javadoc", "Returns twice n.\n@param n input\n@return twice n"));
        for (String code : List.of(SOURCE, draft.applyTo(SOURCE))) {
            Path dir = Files.createDirectory(temp.resolve(UUID.randomUUID().toString()));
            Path file = dir.resolve("Sample.java"); Files.writeString(file, code, StandardCharsets.UTF_8);
            var diagnostics = new ByteArrayOutputStream();
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics, "-encoding", "UTF-8", "-d", dir.toString(), file.toString()), diagnostics.toString());
            try (var loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, null)) {
                var method = loader.loadClass("Sample").getMethod("twice", int.class);
                assertEquals(84, method.invoke(null, 42)); assertEquals(-6, method.invoke(null, -3));
            }
        }
    }
    @Test void httpGenerationSendsSelectionAndLanguageThenBuildsDraftLocally() throws Exception {
        var captured = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", e -> {
            captured.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = new Gson().toJson(Map.of("choices", List.of(Map.of("message", Map.of("content", response(2, "javadoc", "Double n.")))))).getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200, body.length); e.getResponseBody().write(body); e.close();
        }); server.start();
        try {
            var config = new LlmClient.Config("http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions", "fixture-model", "");
            var draft = generator.generate(full(SOURCE), "English", config);
            var messages = JsonParser.parseString(captured.get()).getAsJsonObject().getAsJsonArray("messages");
            assertTrue(messages.get(0).getAsJsonObject().get("content").getAsString().contains("English"));
            assertTrue(messages.get(1).getAsJsonObject().get("content").getAsString().contains(SOURCE));
            assertEquals(1, draft.commentCount()); assertTrue(draft.replacement().contains("Double n."));
        } finally { server.stop(0); }
    }
}
