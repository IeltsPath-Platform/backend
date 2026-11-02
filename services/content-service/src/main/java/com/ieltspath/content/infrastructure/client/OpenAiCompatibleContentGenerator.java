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

/**
 * Chat-completion call to an OpenAI-compatible endpoint. Failures carry a code only: the prompt and the reply are never
 * copied into a message or a log line.
 */
@Component
public class OpenAiCompatibleContentGenerator implements ContentGenerationPort {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private final RestClient restClient;
    private final ContentAiProperties.Llm llm;
    private final ResilientCall resilientCall;

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
    }

    /** Test seam: lets a test pass a client bound to a mock server. */
    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties) {
        this(restClient, properties, defaultResilientCall());
    }

    /** Retained for existing test callers; writes are never retried regardless of this legacy argument. */
    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties,
                                     java.util.List<Duration> ignoredRetryDelays) {
        this(restClient, properties, defaultResilientCall());
    }

    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties,
                                     ResilientCall resilientCall) {
        this.llm = properties.getLlm();
        this.restClient = restClient;
        this.resilientCall = resilientCall;
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
            reply = resilientCall.call("llm", CallKind.WRITE, () -> callOnce(body));
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
