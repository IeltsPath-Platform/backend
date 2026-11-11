package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonllm.ChatRequest;
import com.ieltspath.commonllm.LlmCallException;
import com.ieltspath.commonllm.LlmChatClient;
import com.ieltspath.commonllm.LlmClientFactory;
import com.ieltspath.commonllm.LlmEndpoint;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LlmClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Adapts the shared technical transport to Learning's existing grading contract. */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {
    private final LlmChatClient client;
    private final boolean configured;

    @Autowired
    public OpenAiCompatibleLlmClient(LlmClientFactory factory,
                                     @Value("${learning.llm.base-url:}") String baseUrl,
                                     @Value("${learning.llm.api-key:}") String apiKey,
                                     @Value("${learning.llm.model:}") String model,
                                     @Value("${learning.llm.reasoning-effort:}") String reasoningEffort,
                                     @Value("${learning.writing.grading-timeout-seconds:20}") int timeoutSeconds,
                                     @Value("${learning.llm.max-tokens:8192}") int maxTokens) {
        configured = hasText(baseUrl) && hasText(apiKey) && hasText(model);
        client = factory.create(new LlmEndpoint(baseUrl, apiKey, model, reasoningEffort,
                Duration.ofSeconds(timeoutSeconds), maxTokens), "learning-service");
    }

    OpenAiCompatibleLlmClient(RestClient restClient, String baseUrl, String apiKey, String model,
                              String reasoningEffort) {
        this(restClient, baseUrl, apiKey, model, reasoningEffort, ResilientCall.isolatedDefault());
    }

    OpenAiCompatibleLlmClient(RestClient restClient, String baseUrl, String apiKey, String model,
                              String reasoningEffort, ResilientCall resilientCall) {
        configured = hasText(baseUrl) && hasText(apiKey) && hasText(model);
        client = new LlmClientFactory(RestClient.builder(), resilientCall, new SimpleMeterRegistry())
                .create(new LlmEndpoint(baseUrl, apiKey, model, reasoningEffort, Duration.ofSeconds(20), 8192),
                        "learning-service", restClient);
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    @Override public boolean configured() { return configured; }

    @Override
    public String completeJson(String systemPrompt, String userPrompt) {
        if (!configured()) throw new LlmUnavailableException(null);
        try {
            return client.chat(new ChatRequest("grading", systemPrompt, userPrompt, true, null)).content();
        } catch (LlmCallException failure) {
            if ("bulkhead-full".equals(failure.reason()))
                throw new LearningRequestException(503, "AI_BUSY", "AI grading is busy; try again shortly");
            Integer status = failure.code().startsWith("HTTP_") ? Integer.valueOf(failure.code().substring(5)) : null;
            throw new LlmUnavailableException(status);
        }
    }
}
