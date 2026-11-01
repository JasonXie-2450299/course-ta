package edu.groupb.courseta.core;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class SmartActionsTest {
    private final SmartActions actions = new SmartActions();
    private final SmartActions.JavaContext context = new SmartActions.JavaContext("Demo.java", 3, 5,
        "public String readUser(String id) { return id.strip(); }",
        "package sample;\npublic class Demo { public String readUser(String id) { return id.strip(); } }");
    private static final String JAVA = "package sample;\nimport org.junit.jupiter.api.Test;\nimport static org.junit.jupiter.api.Assertions.*;\n"
        + "class DemoTest { @Test void blankId() { assertEquals(\"\", new Demo().readUser(\" \")); } }";

    @Test void testPromptIncludesScenarioSelectionAndFullUnsavedSource() {
        var p = actions.testPrompt(context, "验证空白字符串输入");
        assertTrue(p.user().contains("验证空白字符串输入"));
        assertTrue(p.user().contains(context.selectedCode())); assertTrue(p.user().contains(context.sourceCode()));
        assertTrue(p.user().contains("Demo.java lines 3-5"));
        assertTrue(p.system().contains("JUnit 5")); assertTrue(p.system().contains("untrusted data"));
    }
    @Test void blankScenarioAndSelectionAreRejectedBeforeModelCall() {
        assertThrows(IllegalArgumentException.class, () -> actions.testPrompt(context, " "));
        var missing = new SmartActions.JavaContext("Demo.java", 1, 1, "", context.sourceCode());
        assertThrows(IllegalArgumentException.class, () -> actions.testPrompt(missing, "empty input"));
    }
    @Test void oversizeJavaInputIsRejectedWithoutTruncatingContext() {
        assertThrows(IllegalArgumentException.class, () -> actions.testPrompt(context, "x".repeat(6001)));
        assertThrows(IllegalArgumentException.class, () -> actions.testPrompt(
            new SmartActions.JavaContext("D.java", 1, 1, "x".repeat(12001), "class D {}"), "empty"));
        assertThrows(IllegalArgumentException.class, () -> actions.testPrompt(
            new SmartActions.JavaContext("D.java", 1, 1, "class D {}", "x".repeat(60001)), "empty"));
    }
    @Test void acceptsJavaSourceAndRemovesOnlyOuterFence() throws Exception {
        assertEquals(JAVA, actions.validateTests(JAVA));
        assertEquals(JAVA, actions.validateTests("```java\n" + JAVA + "\n```"));
        assertEquals("DemoTest.java", SmartActions.suggestedTestFile(JAVA));
    }
    @Test void rejectsExplanationOrMissingJunitClass() {
        assertThrows(IOException.class, () -> actions.validateTests("You should write some tests."));
        assertThrows(IOException.class, () -> actions.validateTests("class Test {}"));
        assertThrows(IOException.class, () -> actions.validateTests("import org.junit.jupiter.api.Test; class DemoTest {}"));
    }
    @Test void commitPromptPreservesEveryFileAndTellsModelNotToInventTests() {
        String diff = "diff --git a/A.java b/A.java\n+first\ndiff --git a/B.java b/B.java\n+second\n";
        var p = actions.commitPrompt(diff);
        assertTrue(p.user().contains(diff)); assertTrue(p.system().contains("ALL"));
        assertTrue(p.system().contains("test results")); assertTrue(p.system().contains("untrusted data"));
    }
    @Test void emptyOrOversizedDiffIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> actions.commitPrompt("\n"));
        assertThrows(IllegalArgumentException.class, () -> actions.commitPrompt("x".repeat(120001)));
    }
    @Test void conventionalCommitSupportsScopeBodyAndBreakingChange() throws Exception {
        String message = "feat(api)!: change lookup result\n\nReturn an Optional for missing users.\n\nBREAKING CHANGE: callers must unwrap results.";
        assertEquals(message, actions.validateCommit(message));
        assertEquals("fix: handle empty input", actions.validateCommit("```text\nfix: handle empty input\n```"));
    }
    @Test void rejectsNonConventionalModelResponse() {
        assertThrows(IOException.class, () -> actions.validateCommit("Here is a message:\nfix: handle empty input"));
        assertThrows(IOException.class, () -> actions.validateCommit("fix: "));
    }
    @Test void bothActionsUseConfiguredHttpServiceAndSendCorrectContexts() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var request = new AtomicReference<String>(); var reply = new AtomicReference<>(JAVA);
        server.createContext("/chat/completions", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = new com.google.gson.Gson().toJson(java.util.Map.of("choices", java.util.List.of(
                java.util.Map.of("message", java.util.Map.of("content", reply.get())))));
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        }); server.start();
        try {
            var cfg = new LlmClient.Config("http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions", "fixture-model", "");
            assertEquals(JAVA, actions.generateTests(context, "test blank input", cfg));
            var json = JsonParser.parseString(request.get()).getAsJsonObject();
            String prompt = json.getAsJsonArray("messages").get(1).getAsJsonObject().get("content").getAsString();
            assertTrue(prompt.contains("test blank input")); assertTrue(prompt.contains(context.sourceCode()));
            assertEquals("fixture-model", json.get("model").getAsString());
            reply.set("fix: reject empty IDs\n\nHandle the empty ID case.");
            String diff = "diff --git a/Demo.java b/Demo.java\n+if(id.isBlank()) throw new Exception();\n";
            assertEquals(reply.get(), actions.generateCommit(diff, cfg));
            assertTrue(JsonParser.parseString(request.get()).getAsJsonObject().getAsJsonArray("messages")
                .get(1).getAsJsonObject().get("content").getAsString().contains(diff));
        } finally { server.stop(0); }
    }
}
