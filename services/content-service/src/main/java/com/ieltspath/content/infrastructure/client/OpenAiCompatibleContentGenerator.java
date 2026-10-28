package com.ieltspath.content.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
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
    /** Waits before the second and the third try after the endpoint says it is busy. */
    private static final List<Duration> BUSY_RETRY_DELAYS = List.of(Duration.ofSeconds(5), Duration.ofSeconds(15));
    /** Statuses that mean "not now" and usually clear within seconds: rate limit and an overloaded or restarting endpoint. */
    private static final Set<Integer> BUSY_STATUSES = Set.of(429, 502, 503, 504);

    private final RestClient restClient;
    private final ContentAiProperties.Llm llm;
    private final List<Duration> retryDelays;

    @Autowired
    public OpenAiCompatibleContentGenerator(RestClient.Builder builder, ContentAiProperties properties) {
        this.llm = properties.getLlm();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Only the answer is slow; an endpoint that cannot be reached should fail fast.
        factory.setConnectTimeout(llm.getTimeout().compareTo(CONNECT_TIMEOUT) < 0 ? llm.getTimeout() : CONNECT_TIMEOUT);
        factory.setReadTimeout(llm.getTimeout());
        this.restClient = builder.requestFactory(factory).baseUrl(llm.getBaseUrl()).build();
        this.retryDelays = BUSY_RETRY_DELAYS;
    }

    /** Test seam: lets a test pass a client bound to a mock server. It does not retry. */
    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties) {
        this(restClient, properties, List.of());
    }

    /** Test seam: the waits between tries of a call the endpoint answered with a busy status. */
    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties, List<Duration> retryDelays) {
        this.llm = properties.getLlm();
        this.restClient = restClient;
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
        JsonNode reply = callWithRetry(body);
        String text = reply == null ? null : reply.path("choices").path(0).path("message").path("content").asText(null);
        if (text == null || text.isBlank()) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY,
                    "Language model returned no text");
        }
        String model = reply.path("model").asText(llm.getModel());
        return new GenerationReply(text, model);
    }

    /** Tries again, after a wait, when the endpoint says it is busy. Any other failure ends the call at once. */
    private JsonNode callWithRetry(Map<String, Object> body) {
        for (int attempt = 0; ; attempt++) {
            try {
                return callOnce(body);
            } catch (ContentGenerationException failure) {
                if (attempt >= retryDelays.size() || !isBusy(failure) || !pause(retryDelays.get(attempt))) {
                    throw failure;
                }
            }
        }
    }

    private JsonNode callOnce(Map<String, Object> body) {
        try {
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
        } catch (RestClientResponseException failure) {
            // Only the status is kept: the body may echo the prompt.
            throw new ContentGenerationException(
                    ContentGenerationException.HTTP_PREFIX + failure.getStatusCode().value(), "Language model returned an error status");
        } catch (RestClientException failure) {
            String code = isTimeout(failure) ? ContentGenerationException.TIMEOUT : ContentGenerationException.FAILED;
            throw new ContentGenerationException(code, "Language model call failed");
        }
    }

    private static boolean isBusy(ContentGenerationException failure) {
        String code = failure.code();
        if (!code.startsWith(ContentGenerationException.HTTP_PREFIX)) {
            return false;
        }
        try {
            return BUSY_STATUSES.contains(Integer.parseInt(code.substring(ContentGenerationException.HTTP_PREFIX.length())));
        } catch (NumberFormatException notAStatus) {
            return false;
        }
    }

    /** Waits; false when the thread was interrupted, so the caller stops trying. */
    private static boolean pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
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
