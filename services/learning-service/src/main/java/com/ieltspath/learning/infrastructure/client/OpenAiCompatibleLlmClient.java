package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * response text is ever logged, and provider error bodies are dropped: only the identifier-shaped {@code type},
 * {@code code}, {@code status} and {@code param} of an error are kept for the log, plus a short excerpt of the message
 * of a 400 (see {@link #errorDetail}).
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);
    /** Output budget of a call that names none, as {@code learning.llm.max-tokens} defaults to. */
    static final int DEFAULT_MAX_TOKENS = 8192;
    private static final ObjectMapper ERROR_READER = new ObjectMapper();
    private static final int MESSAGE_EXCERPT = 300;
    /** Families that think by default and may spend the whole budget on it unless told not to. */
    private static final List<String> THINKING_DEFAULT_OFF = List.of("gemini-2.5", "gemini-3");
    /** The subset that rejects "none" and accepts "minimal" as the lowest level. */
    private static final List<String> MINIMAL_NOT_OFF = List.of("gemini-3", "gemini-2.5-pro");

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;
    private final ResilientCall resilientCall;
    private final int maxTokens;

    @Autowired
    public OpenAiCompatibleLlmClient(RestClient.Builder builder,
                                     @Value("${learning.llm.base-url:}") String baseUrl,
                                     @Value("${learning.llm.api-key:}") String apiKey,
                                     @Value("${learning.llm.model:}") String model,
                                     @Value("${learning.llm.reasoning-effort:}") String reasoningEffort,
                                     @Value("${learning.writing.grading-timeout-seconds:20}") int timeoutSeconds,
                                     @Value("${learning.llm.max-tokens:8192}") int maxTokens,
                                     ResilientCall resilientCall) {
        String normalizedBaseUrl = baseUrl.isBlank() ? null : baseUrl.replaceAll("/+$", "");
        this.restClient = normalizedBaseUrl == null ? null : builder.clone()
                .requestFactory(requestFactory(timeoutSeconds)).baseUrl(normalizedBaseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.resilientCall = resilientCall;
        this.maxTokens = maxTokens;
    }

    OpenAiCompatibleLlmClient(RestClient.Builder builder, String baseUrl, String apiKey, String model,
                              String reasoningEffort, int timeoutSeconds) {
        this(builder, baseUrl, apiKey, model, reasoningEffort, timeoutSeconds, DEFAULT_MAX_TOKENS, null);
    }

    /** {@code restClient} is null when no endpoint is configured. */
    OpenAiCompatibleLlmClient(RestClient restClient, String apiKey, String model, String reasoningEffort) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.resilientCall = null;
        this.maxTokens = DEFAULT_MAX_TOKENS;
    }

    OpenAiCompatibleLlmClient(RestClient restClient, String apiKey, String model, String reasoningEffort,
                              ResilientCall resilientCall) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.resilientCall = resilientCall;
        this.maxTokens = DEFAULT_MAX_TOKENS;
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

    /**
     * Essay grading uses this. A template-v2 grade of a 200-word Task 1 already takes 4,200 to 4,700 output tokens, so a
     * smaller fixed budget cut the JSON off and the grade failed as INVALID_GRADE.
     */
    @Override
    public String completeJson(String systemPrompt, String userPrompt) {
        return completeJson(systemPrompt, userPrompt, new Budget(maxTokens, 0));
    }

    /** A {@code timeoutSeconds} of 0 keeps the client's own read timeout. */
    @Override
    public String completeJson(String systemPrompt, String userPrompt, Budget budget) {
        if (!configured()) throw new LlmUnavailableException(null);
        try {
            Request request = request(systemPrompt, userPrompt, budget.maxTokens());
            RestClient client = budget.timeoutSeconds() > 0
                    ? restClient.mutate().requestFactory(requestFactory(budget.timeoutSeconds())).build() : restClient;
            CallKind kind = budget.retryTransient() ? CallKind.IDEMPOTENT_WRITE : CallKind.WRITE;
            return resilientCall == null ? requestContent(client, request)
                    : resilientCall.call("llm", kind, () -> requestContent(client, request));
        } catch (DownstreamUnavailableException exception) {
            if ("bulkhead-full".equals(exception.reason())) {
                throw new LearningRequestException(503, "AI_BUSY", "AI grading is busy; try again shortly");
            }
            throw new LlmUnavailableException(null);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("LLM request failed: status={}, {}", status, errorDetail(exception));
            throw new LlmUnavailableException(status);
        } catch (RestClientException exception) {
            log.warn("LLM request failed: {}", exception.getClass().getSimpleName());
            throw new LlmUnavailableException(null);
        }
    }

    /**
     * The {@code type}, {@code code}, {@code status} and {@code param} of a provider error, so a failure can be told apart
     * (an unsupported parameter, a limit that is too high). Each value is kept only when it looks like an identifier.
     * Only a 400 also keeps the start of the provider's message: it says which part of the request was refused, and the
     * other statuses (busy, unavailable, refused key) are understood from the code alone.
     */
    static String errorDetail(RestClientResponseException exception) {
        try {
            JsonNode root = ERROR_READER.readTree(exception.getResponseBodyAsString());
            JsonNode error = (root.isArray() ? root.path(0) : root).path("error");
            String detail = "providerType=" + identifier(error.path("type")) + ", providerCode=" + identifier(error.path("code"))
                    + ", providerStatus=" + identifier(error.path("status")) + ", providerParam=" + identifier(error.path("param"));
            if (exception.getStatusCode().value() == 400) detail += ", providerMessage=" + excerpt(error.path("message"));
            return detail;
        } catch (Exception unreadable) {
            return "providerType=-, providerCode=-, providerStatus=-, providerParam=-";
        }
    }

    private static String excerpt(JsonNode value) {
        String text = value.asText("").replaceAll("\\s+", " ").strip();
        return text.length() <= MESSAGE_EXCERPT ? text : text.substring(0, MESSAGE_EXCERPT) + "...";
    }

    private static String identifier(JsonNode value) {
        String text = value.asText("");
        return text.matches("[A-Za-z0-9_.\\-]{1,64}") ? text : "-";
    }

    private Request request(String systemPrompt, String userPrompt, int maxTokens) {
        return new Request(model, List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
                maxTokens, supportsTemperature(model, reasoningEffort) ? 0.0 : null, new ResponseFormat("json_object"),
                resolveReasoningEffort(model, reasoningEffort));
    }

    private String requestContent(RestClient client, Request body) {
        JsonNode response = client.post().uri("/chat/completions")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .body(body).retrieve().body(JsonNode.class);
        JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
        if (content == null || !content.isTextual()) {
            log.warn("LLM reply carried no text content");
            throw new LlmUnavailableException(null);
        }
        return content.asText();
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
                   @JsonProperty("reasoning_effort") String reasoningEffort) {
        /** Spring's HTTP debug log renders the body with toString; message content must stay private. */
        @Override
        public String toString() {
            return "LLM request[model=" + model + ", maxTokens=" + maxTokens + "]";
        }
    }

    record Message(String role, String content) {}

    record ResponseFormat(String type) {}
}
