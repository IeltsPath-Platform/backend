package com.ieltspath.content.infrastructure.client;

import com.ieltspath.commonllm.ChatRequest;
import com.ieltspath.commonllm.LlmCallException;
import com.ieltspath.commonllm.LlmChatClient;
import com.ieltspath.commonllm.LlmClientFactory;
import com.ieltspath.commonllm.LlmEndpoint;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** One draft generation call. The author may request a new draft after a provider failure. */
@Component
public class OpenAiCompatibleContentGenerator implements ContentGenerationPort {
    private final LlmChatClient client;
    private final ContentAiProperties.Llm llm;

    @Autowired
    public OpenAiCompatibleContentGenerator(LlmClientFactory factory, ContentAiProperties properties) {
        llm = properties.getLlm();
        client = factory.create(endpoint(llm), "content-service");
    }

    OpenAiCompatibleContentGenerator(RestClient restClient, ContentAiProperties properties) {
        llm = properties.getLlm();
        client = new LlmClientFactory(RestClient.builder(), ResilientCall.isolatedDefault(), new SimpleMeterRegistry())
                .create(endpoint(llm), "content-service", restClient);
    }

    private static LlmEndpoint endpoint(ContentAiProperties.Llm llm) {
        return new LlmEndpoint(llm.getBaseUrl(), llm.getApiKey(), llm.getModel(), llm.getReasoningEffort(),
                llm.getTimeout(), llm.getMaxTokens());
    }

    @Override
    public GenerationReply generate(String prompt) {
        if (llm.getBaseUrl().isBlank() || llm.getModel().isBlank())
            throw new ContentGenerationException(ContentGenerationException.NOT_CONFIGURED, "Language model is not configured");
        try {
            var reply = client.chat(new ChatRequest("content-generation", null, prompt, false, null));
            if (reply.content().isBlank())
                throw new ContentGenerationException(ContentGenerationException.INVALID_REPLY, "Language model returned no text");
            return new GenerationReply(reply.content(), reply.model());
        } catch (LlmCallException failure) {
            String code = switch (failure.code()) {
                case "TIMEOUT" -> ContentGenerationException.TIMEOUT;
                case "BAD_RESPONSE" -> "decode-failure".equals(failure.reason())
                        ? ContentGenerationException.FAILED : ContentGenerationException.INVALID_REPLY;
                default -> failure.code().startsWith("HTTP_") ? failure.code() : ContentGenerationException.FAILED;
            };
            throw new ContentGenerationException(code, "Language model call failed");
        }
    }
}
