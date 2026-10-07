package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.port.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Minimal OpenAI-compatible {@code /chat/completions} client. The reasoning-effort defaults and the temperature rule
 * follow DeepTutor v1.6.9 ({@code deeptutor/services/llm/reasoning_params.py} and
 * {@code provider_core/openai_compat_provider.py}, Apache-2.0), as the retired Python service did. No request or
 * response text is ever logged, and provider error bodies are dropped.
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {
    static final String RETRY_REASONING_EFFORT = "low";
    private static final int MAX_TOKENS = 4096;
    /** Families that think by default and may spend the whole budget on it unless told not to. */
    private static final List<String> THINKING_DEFAULT_OFF = List.of("gemini-2.5", "gemini-3");
    /** The subset that rejects "none" and accepts "minimal" as the lowest level. */
    private static final List<String> MINIMAL_NOT_OFF = List.of("gemini-3", "gemini-2.5-pro");

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;

    @Autowired
    public OpenAiCompatibleLlmClient(RestClient.Builder builder,
                                     @Value("${learning.llm.base-url:}") String baseUrl,
                                     @Value("${learning.llm.api-key:}") String apiKey,
                                     @Value("${learning.llm.model:}") String model,
                                     @Value("${learning.llm.reasoning-effort:}") String reasoningEffort,
                                     @Value("${learning.writing.grading-timeout-seconds:20}") int timeoutSeconds) {
        this(baseUrl.isBlank() ? null : builder.requestFactory(requestFactory(timeoutSeconds))
                .baseUrl(baseUrl.replaceAll("/+$", "")).build(), apiKey, model, reasoningEffort);
    }

    /** {@code restClient} is null when no endpoint is configured. */
    OpenAiCompatibleLlmClient(RestClient restClient, String apiKey, String model, String reasoningEffort) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
    }

    private static SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return requestFactory;
    }

    @Override
    public boolean configured() {
        return restClient != null && !apiKey.isBlank() && !model.isBlank();
    }

    @Override
    public String completeJson(String systemPrompt, String userPrompt, boolean lowReasoning) {
        if (!configured()) throw new LlmUnavailableException(null);
        String requested = lowReasoning ? RETRY_REASONING_EFFORT : reasoningEffort;
        Request body = new Request(model,
                List.of(new Message("system", systemPrompt), new Message("user", userPrompt)), MAX_TOKENS,
                supportsTemperature(model, requested) ? 0.0 : null, new ResponseFormat("json_object"),
                resolveReasoningEffort(model, requested));
        try {
            JsonNode response = restClient.post().uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(body).retrieve().body(JsonNode.class);
            JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
            if (content == null || !content.isTextual()) throw new LlmUnavailableException(null);
            return content.asText();
        } catch (RestClientResponseException exception) {
            throw new LlmUnavailableException(exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new LlmUnavailableException(null);
        }
    }

    /** The {@code reasoning_effort} to send, or null to leave the field out. */
    static String resolveReasoningEffort(String model, String requested) {
        String effort = requested == null ? "" : requested.strip().toLowerCase(Locale.ROOT);
        if (effort.isEmpty()) {
            if (!matches(model, THINKING_DEFAULT_OFF)) return null;
            return matches(model, MINIMAL_NOT_OFF) ? "minimal" : "none";
        }
        if (effort.equals("none")) return matches(model, MINIMAL_NOT_OFF) ? "minimal" : "none";
        return effort;
    }

    /** No temperature with an explicit reasoning effort, nor for o-series or gpt-5 models. */
    static boolean supportsTemperature(String model, String requested) {
        String effort = requested == null ? "" : requested.strip().toLowerCase(Locale.ROOT);
        if (!effort.isEmpty() && !effort.equals("none")) return false;
        return !matches(model, List.of("gpt-5", "o1", "o3", "o4"));
    }

    private static boolean matches(String model, List<String> patterns) {
        String lowered = model.toLowerCase(Locale.ROOT);
        return patterns.stream().anyMatch(lowered::contains);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Request(String model, List<Message> messages, @JsonProperty("max_tokens") int maxTokens,
                   Double temperature, @JsonProperty("response_format") ResponseFormat responseFormat,
                   @JsonProperty("reasoning_effort") String reasoningEffort) {}

    record Message(String role, String content) {}

    record ResponseFormat(String type) {}
}
