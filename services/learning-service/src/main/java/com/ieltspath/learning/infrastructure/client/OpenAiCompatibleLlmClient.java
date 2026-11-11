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
 * response text is ever logged, and provider error bodies are dropped.
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);
    private static final int MAX_TOKENS = 4096;
    private static final ObjectMapper ERROR_BODY = new ObjectMapper();
    /** The request fields a provider error may name; only these names, never the message, reach the log. */
    private static final List<String> REQUEST_FIELDS = List.of("max_tokens", "max_completion_tokens", "temperature",
            "response_format", "reasoning_effort", "messages", "model");
    /** OpenAI families that take no temperature and name the output budget {@code max_completion_tokens}. */
    private static final List<String> OPENAI_REASONING_MODELS = List.of("gpt-5", "o1", "o3", "o4");
    /** Families that think by default and may spend the whole budget on it unless told not to. */
    private static final List<String> THINKING_DEFAULT_OFF = List.of("gemini-2.5", "gemini-3");
    /** The subset that rejects "none" and accepts "minimal" as the lowest level. */
    private static final List<String> MINIMAL_NOT_OFF = List.of("gemini-3", "gemini-2.5-pro");

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;
    private final ResilientCall resilientCall;

    @Autowired
    public OpenAiCompatibleLlmClient(RestClient.Builder builder,
                                     @Value("${learning.llm.base-url:}") String baseUrl,
                                     @Value("${learning.llm.api-key:}") String apiKey,
                                     @Value("${learning.llm.model:}") String model,
                                     @Value("${learning.llm.reasoning-effort:}") String reasoningEffort,
                                     @Value("${learning.writing.grading-timeout-seconds:20}") int timeoutSeconds,
                                     ResilientCall resilientCall) {
        String normalizedBaseUrl = baseUrl.isBlank() ? null : baseUrl.replaceAll("/+$", "");
        this.restClient = normalizedBaseUrl == null ? null : builder.clone()
                .requestFactory(requestFactory(timeoutSeconds)).baseUrl(normalizedBaseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.resilientCall = resilientCall;
    }

    OpenAiCompatibleLlmClient(RestClient.Builder builder, String baseUrl, String apiKey, String model,
                              String reasoningEffort, int timeoutSeconds) {
        this(builder, baseUrl, apiKey, model, reasoningEffort, timeoutSeconds, null);
    }

    /** {@code restClient} is null when no endpoint is configured. */
    OpenAiCompatibleLlmClient(RestClient restClient, String apiKey, String model, String reasoningEffort) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.resilientCall = null;
    }

    OpenAiCompatibleLlmClient(RestClient restClient, String apiKey, String model, String reasoningEffort,
                              ResilientCall resilientCall) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.resilientCall = resilientCall;
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
    public String completeJson(String systemPrompt, String userPrompt) {
        if (!configured()) throw new LlmUnavailableException(null);
        try {
            Request request = request(systemPrompt, userPrompt);
            return resilientCall == null ? requestContent(request)
                    : resilientCall.call("llm", CallKind.WRITE, () -> requestContent(request));
        } catch (DownstreamUnavailableException exception) {
            if ("bulkhead-full".equals(exception.reason())) {
                throw new LearningRequestException(503, "AI_BUSY", "AI grading is busy; try again shortly");
            }
            throw new LlmUnavailableException(null);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("LLM request failed: status={}{}, model={}, reasoningEffort={}", status, rejectedField(exception),
                    model, resolveReasoningEffort(model, reasoningEffort));
            throw new LlmUnavailableException(status);
        } catch (RestClientException exception) {
            log.warn("LLM request failed: {}", exception.getClass().getSimpleName());
            throw new LlmUnavailableException(null);
        }
    }

    private Request request(String systemPrompt, String userPrompt) {
        boolean completionTokens = usesCompletionTokens(model);
        return new Request(model, List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
                completionTokens ? null : MAX_TOKENS, completionTokens ? MAX_TOKENS : null,
                supportsTemperature(model, reasoningEffort) ? 0.0 : null, new ResponseFormat("json_object"),
                resolveReasoningEffort(model, reasoningEffort));
    }

    /**
     * What a provider's error body says about a rejected request, without its text: the short machine-readable
     * {@code error.code}, {@code error.param}, {@code error.type} and {@code error.status}, and which of the request's own
     * field names the error message mentions. The message itself and the rest of the body stay out of the log. Gemini
     * wraps the error object in an array; a body that is not a JSON error adds nothing.
     */
    private static String rejectedField(RestClientResponseException exception) {
        try {
            JsonNode body = ERROR_BODY.readTree(exception.getResponseBodyAsString());
            JsonNode error = (body.isArray() ? body.path(0) : body).path("error");
            StringBuilder hint = new StringBuilder();
            for (String field : List.of("code", "param", "type", "status")) {
                String value = error.path(field).asText("");
                if (value.matches("[A-Za-z0-9_.\\-\\[\\]]{1,60}")) hint.append(", ").append(field).append('=').append(value);
            }
            String message = error.path("message").asText("");
            List<String> mentioned = REQUEST_FIELDS.stream().filter(message::contains).toList();
            if (!mentioned.isEmpty()) hint.append(", mentions=").append(String.join("|", mentioned));
            return hint.toString();
        } catch (Exception unreadable) {
            return "";
        }
    }

    private String requestContent(Request body) {
        JsonNode response = restClient.post().uri("/chat/completions")
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
        return !matches(model, OPENAI_REASONING_MODELS);
    }

    /** These models reject {@code max_tokens} and take {@code max_completion_tokens} instead. */
    static boolean usesCompletionTokens(String model) {
        return matches(model, OPENAI_REASONING_MODELS);
    }

    private static boolean matches(String model, List<String> patterns) {
        String lowered = model.toLowerCase(Locale.ROOT);
        return patterns.stream().anyMatch(lowered::contains);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Request(String model, List<Message> messages, @JsonProperty("max_tokens") Integer maxTokens,
                   @JsonProperty("max_completion_tokens") Integer maxCompletionTokens,
                   Double temperature, @JsonProperty("response_format") ResponseFormat responseFormat,
                   @JsonProperty("reasoning_effort") String reasoningEffort) {
        /** Spring's HTTP debug log renders the body with toString; message content must stay private. */
        @Override
        public String toString() {
            return "LLM request[model=" + model + ", maxTokens=" + (maxTokens != null ? maxTokens : maxCompletionTokens) + "]";
        }
    }

    record Message(String role, String content) {}

    record ResponseFormat(String type) {}
}
