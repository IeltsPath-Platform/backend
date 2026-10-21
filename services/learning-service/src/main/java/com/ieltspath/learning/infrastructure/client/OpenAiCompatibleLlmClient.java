package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
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
    static final String RETRY_REASONING_EFFORT = "low";
    private static final int MAX_TOKENS = 4096;
    /** One request is repeated once when the provider answers 429 or 503. */
    private static final int MAX_ATTEMPTS = 2;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);
    /**
     * Wall-clock ceiling of one call including its repeat: a submission makes up to two calls and then debits points
     * within the 60 s life of the forwarded internal JWT, so a call must stay under 25 s (see WritingSettings).
     */
    private static final int CALL_BUDGET_SECONDS = 25;
    /** Used when the read timeout is not known: leaves room for the repeat inside {@link #CALL_BUDGET_SECONDS}. */
    private static final Duration DEFAULT_RETRY_WINDOW = Duration.ofSeconds(4);
    /** Families that think by default and may spend the whole budget on it unless told not to. */
    private static final List<String> THINKING_DEFAULT_OFF = List.of("gemini-2.5", "gemini-3");
    /** The subset that rejects "none" and accepts "minimal" as the lowest level. */
    private static final List<String> MINIMAL_NOT_OFF = List.of("gemini-3", "gemini-2.5-pro");

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;
    private final Duration retryDelay;
    /** A busy answer is repeated only when it came back faster than this, so both attempts fit in the call budget. */
    private final Duration retryWindow;

    @Autowired
    public OpenAiCompatibleLlmClient(RestClient.Builder builder,
                                     @Value("${learning.llm.base-url:}") String baseUrl,
                                     @Value("${learning.llm.api-key:}") String apiKey,
                                     @Value("${learning.llm.model:}") String model,
                                     @Value("${learning.llm.reasoning-effort:}") String reasoningEffort,
                                     @Value("${learning.writing.grading-timeout-seconds:20}") int timeoutSeconds) {
        this(baseUrl.isBlank() ? null : builder.requestFactory(requestFactory(timeoutSeconds))
                        .baseUrl(baseUrl.replaceAll("/+$", "")).build(), apiKey, model, reasoningEffort, RETRY_DELAY,
                Duration.ofSeconds(Math.max(0, CALL_BUDGET_SECONDS - timeoutSeconds - RETRY_DELAY.toSeconds())));
    }

    /** {@code restClient} is null when no endpoint is configured. */
    OpenAiCompatibleLlmClient(RestClient restClient, String apiKey, String model, String reasoningEffort) {
        this(restClient, apiKey, model, reasoningEffort, RETRY_DELAY, DEFAULT_RETRY_WINDOW);
    }

    OpenAiCompatibleLlmClient(RestClient restClient, String apiKey, String model, String reasoningEffort,
                              Duration retryDelay, Duration retryWindow) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.retryDelay = retryDelay;
        this.retryWindow = retryWindow;
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
        for (int attempt = 1; ; attempt++) {
            long started = System.nanoTime();
            try {
                return requestContent(body);
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                Duration answeredIn = Duration.ofNanos(System.nanoTime() - started);
                log.warn("LLM request failed: status={}, attempt={}/{}, answeredInMs={}", status, attempt,
                        MAX_ATTEMPTS, answeredIn.toMillis());
                // A late busy answer leaves no room for a second attempt inside the call budget.
                boolean worthRepeating = isOverloaded(status) && answeredIn.compareTo(retryWindow) < 0;
                if (attempt >= MAX_ATTEMPTS || !worthRepeating) throw new LlmUnavailableException(status);
                pauseBeforeRetry();
            } catch (RestClientException exception) {
                // A timeout or broken connection has already used the whole read timeout, so it is not tried again.
                log.warn("LLM request failed: {}", exception.getClass().getSimpleName());
                throw new LlmUnavailableException(null);
            }
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

    /** The provider is busy or limiting the rate: a second request moments later often succeeds. */
    private static boolean isOverloaded(int status) {
        return status == 429 || status == 503;
    }

    private void pauseBeforeRetry() {
        if (retryDelay.isZero()) return;
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
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
