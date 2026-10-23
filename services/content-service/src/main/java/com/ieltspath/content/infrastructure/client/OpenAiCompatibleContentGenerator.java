package com.ieltspath.content.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Chat-completion call to an OpenAI-compatible endpoint. Failures carry a code only: the prompt and the reply are never
 * copied into a message or a log line.
 */
@Component
public class OpenAiCompatibleContentGenerator implements ContentGenerationPort {

    private final RestClient restClient;
    private final ContentAiProperties.Llm llm;

    public OpenAiCompatibleContentGenerator(RestClient.Builder builder, ContentAiProperties properties) {
        this.llm = properties.getLlm();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(llm.getTimeout());
        factory.setReadTimeout(llm.getTimeout());
        this.restClient = builder.requestFactory(factory).baseUrl(llm.getBaseUrl()).build();
    }

    /** Test seam: lets a test pass a client bound to a mock server. */
    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties) {
        this.llm = properties.getLlm();
        this.restClient = restClient;
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
            reply = restClient.post()
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
        } catch (RestClientException failure) {
            throw new ContentGenerationException(ContentGenerationException.FAILED, "Language model call failed");
        }
        String text = reply == null ? null : reply.path("choices").path(0).path("message").path("content").asText(null);
        if (text == null || text.isBlank()) {
            throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY,
                    "Language model returned no text");
        }
        String model = reply.path("model").asText(llm.getModel());
        return new GenerationReply(text, model);
    }
}
