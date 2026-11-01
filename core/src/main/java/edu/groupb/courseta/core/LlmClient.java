package edu.groupb.courseta.core;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Non-streaming Chat Completions client. No prompt, key or provider error-body logging. */
public final class LlmClient {
    public record Config(String endpoint, String model, String apiKey) { }
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    public static URI validateEndpoint(String endpoint) {
        URI uri=URI.create(endpoint.strip());
        boolean local=Set.of("localhost","127.0.0.1","[::1]","::1").contains(Objects.toString(uri.getHost(),""));
        if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null || uri.getQuery()!=null
            || !("https".equalsIgnoreCase(uri.getScheme()) || (local && "http".equalsIgnoreCase(uri.getScheme()))))
            throw new IllegalArgumentException("Use a full HTTPS chat-completions endpoint, or HTTP on localhost.");
        return uri;
    }
    public String complete(Config config, String system, String user) throws IOException, InterruptedException {
        URI uri=validateEndpoint(config.endpoint);
        if(config.model==null || config.model.isBlank()) throw new IOException("Configure an LLM model first.");
        String body=new Gson().toJson(Map.of("model",config.model,"stream",false,"temperature",0.2,
            "messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",user))));
        var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(90)).header("Content-Type","application/json");
        if(config.apiKey!=null && !config.apiKey.isBlank()) builder.header("Authorization","Bearer " + config.apiKey.strip());
        HttpResponse<String> response=http.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200) throw new IOException(switch(response.statusCode()) {
            case 401,403 -> "LLM authentication failed. Check API key and permissions.";
            case 429 -> "LLM rate limit or quota exceeded. Try again later.";
            default -> "LLM request failed (HTTP " + response.statusCode() + "). Check the endpoint and provider status.";
        });
        try {
            String answer=JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("choices")
                .get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
            if(answer.isBlank()) throw new IllegalStateException();
            return answer;
        } catch(RuntimeException ex) { throw new IOException("The LLM returned no usable answer. Check model compatibility."); }
    }
}
