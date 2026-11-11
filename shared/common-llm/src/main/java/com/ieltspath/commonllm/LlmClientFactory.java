package com.ieltspath.commonllm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared OpenAI-compatible transport. Model rules follow DeepTutor v1.6.9
 * (deeptutor/services/llm/reasoning_params.py and provider_core/openai_compat_provider.py, Apache-2.0).
 * Prompts, replies, credentials and provider error messages never enter application logs.
 */
public class LlmClientFactory {
    private static final Logger log = LoggerFactory.getLogger(LlmClientFactory.class);
    private static final ObjectMapper ERROR_BODY = new ObjectMapper();
    private static final List<String> REQUEST_FIELDS = List.of("max_tokens", "max_completion_tokens",
            "temperature", "response_format", "reasoning_effort", "messages", "model");
    private static final List<String> OPENAI_REASONING_MODELS = List.of("gpt-5", "o1", "o3", "o4");
    private static final List<String> THINKING_DEFAULT_OFF = List.of("gemini-2.5", "gemini-3");
    private static final List<String> MINIMAL_NOT_OFF = List.of("gemini-3", "gemini-2.5-pro");
    private final RestClient.Builder builder;
    private final ResilientCall resilientCall;
    private final MeterRegistry registry;

    public LlmClientFactory(RestClient.Builder builder, ResilientCall resilientCall, MeterRegistry registry) {
        this.builder = Objects.requireNonNull(builder);
        this.resilientCall = Objects.requireNonNull(resilientCall);
        this.registry = Objects.requireNonNull(registry);
    }

    public LlmChatClient create(LlmEndpoint endpoint, String service) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(endpoint.timeout().compareTo(Duration.ofSeconds(5)) < 0
                ? endpoint.timeout() : Duration.ofSeconds(5));
        requests.setReadTimeout(endpoint.timeout());
        return create(endpoint, service, builder.clone().requestFactory(requests).build());
    }

    /** Allows adapters tested without a Spring context to use an already configured HTTP client. */
    public LlmChatClient create(LlmEndpoint endpoint, String service, RestClient restClient) {
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(service);
        Objects.requireNonNull(restClient);
        return request -> chat(restClient, endpoint, service, request);
    }

    private ChatReply chat(RestClient client, LlmEndpoint endpoint, String service, ChatRequest request) {
        Timer.Sample duration = Timer.start(registry);
        String outcome = "success";
        try {
            if (endpoint.baseUrl().isBlank() || endpoint.model().isBlank())
                throw new LlmCallException("UNAVAILABLE");
            ChatReply reply = resilientCall.call("llm", CallKind.WRITE,
                    () -> request(client, endpoint, request));
            Tags tags = tags(service, endpoint.model(), request.purpose(), outcome);
            if (reply.inputTokens() != null)
                registry.counter("llm.tokens", tags.and("direction", "input")).increment(reply.inputTokens());
            if (reply.outputTokens() != null)
                registry.counter("llm.tokens", tags.and("direction", "output")).increment(reply.outputTokens());
            return reply;
        } catch (DownstreamUnavailableException failure) {
            outcome = "UNAVAILABLE";
            throw failure(service, endpoint.model(), request.purpose(),
                    new LlmCallException(outcome, failure.reason()));
        } catch (RestClientResponseException failure) {
            outcome = "HTTP_" + failure.getStatusCode().value();
            log.warn("LLM request failed: service={}, model={}, purpose={}, error={}, status={}{}",
                    service, endpoint.model(), request.purpose(), outcome, failure.getStatusCode().value(),
                    rejectedField(failure));
            throw new LlmCallException(outcome);
        } catch (RestClientException failure) {
            outcome = isTimeout(failure) ? "TIMEOUT"
                    : failure instanceof org.springframework.web.client.ResourceAccessException ? "UNAVAILABLE" : "BAD_RESPONSE";
            throw failure(service, endpoint.model(), request.purpose(), new LlmCallException(outcome,
                    "BAD_RESPONSE".equals(outcome) ? "decode-failure" : null));
        } catch (LlmCallException failure) {
            outcome = failure.code();
            throw failure(service, endpoint.model(), request.purpose(), failure);
        } finally {
            duration.stop(registry.timer("llm.call.duration", tags(service, endpoint.model(), request.purpose(), outcome)));
        }
    }

    private static LlmCallException failure(String service, String model, String purpose, LlmCallException failure) {
        log.warn("LLM request failed: service={}, model={}, purpose={}, error={}", service, model, purpose, failure.code());
        return failure;
    }

    private static Tags tags(String service, String model, String purpose, String outcome) {
        return Tags.of("service", service, "model", model, "purpose", purpose, "outcome", outcome);
    }

    private static ChatReply request(RestClient client, LlmEndpoint endpoint, ChatRequest request) {
        List<Message> messages = new ArrayList<>();
        if (request.systemPrompt() != null) messages.add(new Message("system", request.systemPrompt()));
        messages.add(new Message("user", request.userPrompt()));
        int budget = request.maxTokensOverride() == null ? endpoint.maxTokens() : request.maxTokensOverride();
        boolean completionTokens = usesCompletionTokens(endpoint.model());
        Request body = new Request(endpoint.model(), messages, completionTokens ? null : budget,
                completionTokens ? budget : null, supportsTemperature(endpoint.model(), endpoint.reasoningEffort()) ? 0.0 : null,
                request.jsonMode() ? new ResponseFormat("json_object") : null,
                resolveReasoningEffort(endpoint.model(), endpoint.reasoningEffort()));
        JsonNode response = client.post().uri(endpoint.baseUrl() + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> { if (!endpoint.apiKey().isBlank()) headers.setBearerAuth(endpoint.apiKey()); })
                .body(body).retrieve().body(JsonNode.class);
        JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
        if (content == null || !content.isTextual())
            throw new LlmCallException("BAD_RESPONSE");
        JsonNode usage = response.path("usage");
        JsonNode model = response.path("model");
        return new ChatReply(content.asText(), tokens(usage.path("prompt_tokens")),
                tokens(usage.path("completion_tokens")), model.isTextual() ? model.asText() : endpoint.model());
    }

    private static Long tokens(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToLong() && node.longValue() >= 0 ? node.longValue() : null;
    }

    private static boolean isTimeout(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause())
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) return true;
        return false;
    }

    /** Error-field hints only; free text and unknown fields are discarded, including Gemini array envelopes. */
    private static String rejectedField(RestClientResponseException exception) {
        try {
            JsonNode body = ERROR_BODY.readTree(exception.getResponseBodyAsString());
            JsonNode error = (body.isArray() ? body.path(0) : body).path("error");
            StringBuilder hint = new StringBuilder();
            for (String field : List.of("code", "param", "type", "status")) {
                String value = error.path(field).asText("");
                if (value.matches("[A-Za-z0-9_.\\-\\[\\]]{1,60}"))
                    hint.append(", ").append(field).append('=').append(value);
            }
            String message = error.path("message").asText("");
            List<String> mentioned = REQUEST_FIELDS.stream().filter(message::contains).toList();
            if (!mentioned.isEmpty()) hint.append(", mentions=").append(String.join("|", mentioned));
            return hint.toString();
        } catch (Exception unreadable) {
            return "";
        }
    }

    static String resolveReasoningEffort(String model, String requested) {
        String effort = requested == null ? "" : requested.strip().toLowerCase(Locale.ROOT);
        if (effort.isEmpty()) {
            if (!matches(model, THINKING_DEFAULT_OFF)) return null;
            return matches(model, MINIMAL_NOT_OFF) ? "minimal" : "none";
        }
        if (effort.equals("none")) return matches(model, MINIMAL_NOT_OFF) ? "minimal" : "none";
        return effort;
    }

    static boolean supportsTemperature(String model, String requested) {
        String effort = requested == null ? "" : requested.strip().toLowerCase(Locale.ROOT);
        if (!effort.isEmpty() && !effort.equals("none")) return false;
        return !matches(model, OPENAI_REASONING_MODELS);
    }

    static boolean usesCompletionTokens(String model) { return matches(model, OPENAI_REASONING_MODELS); }

    private static boolean matches(String model, List<String> patterns) {
        String lowered = model.toLowerCase(Locale.ROOT);
        return patterns.stream().anyMatch(lowered::contains);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Request(String model, List<Message> messages, @JsonProperty("max_tokens") Integer maxTokens,
                   @JsonProperty("max_completion_tokens") Integer maxCompletionTokens, Double temperature,
                   @JsonProperty("response_format") ResponseFormat responseFormat,
                   @JsonProperty("reasoning_effort") String reasoningEffort) {
        @Override public String toString() { return "LLM request[model=" + model + "]"; }
    }
    record Message(String role, String content) {
        @Override public String toString() { return "Message[role=" + role + "]"; }
    }
    record ResponseFormat(String type) {}
}
