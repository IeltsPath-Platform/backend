package com.ieltspath.content.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chat-completion call to an OpenAI-compatible endpoint. Failures carry a code only: the prompt and the reply are never
 * copied into a message or a log line.
 */
@Component
public class OpenAiCompatibleContentGenerator implements ContentGenerationPort {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Waits before the second and third attempt when the model is busy (429, 502, 503, 504). */
    static final List<Duration> DEFAULT_RETRY_DELAYS = List.of(Duration.ofSeconds(5), Duration.ofSeconds(15));
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 502, 503, 504);
    private final RestClient restClient;
    private final ContentAiProperties.Llm llm;
    private final ResilientCall resilientCall;
    private final List<Duration> retryDelays;

    @Autowired
    public OpenAiCompatibleContentGenerator(RestClient.Builder builder, ContentAiProperties properties,
                                            ResilientCall resilientCall) {
        this.llm = properties.getLlm();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Only the answer is slow; an endpoint that cannot be reached should fail fast.
        factory.setConnectTimeout(llm.getTimeout().compareTo(CONNECT_TIMEOUT) < 0 ? llm.getTimeout() : CONNECT_TIMEOUT);
        factory.setReadTimeout(llm.getTimeout());
        this.restClient = builder.requestFactory(factory).baseUrl(llm.getBaseUrl()).build();
        this.resilientCall = resilientCall;
        this.retryDelays = DEFAULT_RETRY_DELAYS;
    }

    /** Test seam: lets a test pass a client bound to a mock server. */
    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties) {
        this(restClient, properties, defaultResilientCall());
    }

    /** Retained for existing test callers; writes are never retried regardless of this legacy argument. */
    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties,
                                     List<Duration> retryDelays) {
        this(restClient, properties, defaultResilientCall(), retryDelays);
    }

    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties,
                                     ResilientCall resilientCall) {
        this.llm = properties.getLlm();
        this.restClient = restClient;
        this.resilientCall = resilientCall;
        this.retryDelays = DEFAULT_RETRY_DELAYS;
    }

    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties,
                                     ResilientCall resilientCall, List<Duration> retryDelays) {
        this.llm = properties.getLlm();
        this.restClient = restClient;
        this.resilientCall = resilientCall;
        this.retryDelays = retryDelays;
    }

    @Override
    public GenerationReply generate(String prompt) {
        if (llm.getBaseUrl().isBlank() || llm.getModel().isBlank()) {
            throw new ContentGenerationException(ContentGenerationException.NOT_CONFIGURED,
                    "Language model is not configured");
        }
        Map<String, Object> body = Map.of(
                "model", llm.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        JsonNode reply;
        try {
            reply = callWithRetries(body);
        } catch (DownstreamUnavailableException unavailable) {
            throw new ContentGenerationException(ContentGenerationException.FAILED,
                    "Language model call failed");
        } catch (RestClientResponseException failure) {
            throw new ContentGenerationException(ContentGenerationException.HTTP_PREFIX
                    + failure.getStatusCode().value(), "Language model returned an error status");
        } catch (RestClientException failure) {
            String code = isTimeout(failure) ? ContentGenerationException.TIMEOUT : ContentGenerationException.FAILED;
            throw new ContentGenerationException(code, "Language model call failed");
        }
        String text = reply == null ? null : reply.path("choices").path(0).path("message").path("content").asText(null);
        if (text == null || text.isBlank()) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY,
                    "Language model returned no text");
        }
        String model = reply.path("model").asText(llm.getModel());
        return new GenerationReply(text, model);
    }

    /**
     * Retries a busy model outside the breaker: each attempt is one breaker call, so an open breaker still fails fast.
     * Other errors, and the last failure, are rethrown unchanged.
     */
    private JsonNode callWithRetries(Map<String, Object> body) {
        for (int attempt = 0; ; attempt++) {
            try {
                return resilientCall.call("llm", CallKind.WRITE, () -> callOnce(body));
            } catch (RestClientResponseException failure) {
                boolean retryable = RETRYABLE_STATUSES.contains(failure.getStatusCode().value());
                if (!retryable || attempt >= retryDelays.size()) throw failure;
                pause(retryDelays.get(attempt));
            }
        }
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry the language model", interrupted);
        }
    }

    private JsonNode callOnce(Map<String, Object> body) {
        return restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (!llm.getApiKey().isBlank()) {
                        headers.setBearerAuth(llm.getApiKey());
                    }
                })
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private static ResilientCall defaultResilientCall() {
        return ResilientCall.isolatedDefault();
    }

    private static boolean isTimeout(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
