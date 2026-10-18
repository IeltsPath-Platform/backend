package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.EssayPromptActivationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class RestEssayPromptActivationClient implements EssayPromptActivationProvider {
    private final RestClient client;

    @Autowired
    public RestEssayPromptActivationClient(RestClient.Builder builder,
            @Value("${learning.assessment.base-url:http://localhost:8083}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = builder.requestFactory(factory).baseUrl(baseUrl).build();
    }

    RestEssayPromptActivationClient(RestClient client) { this.client = client; }

    @Override
    public String activatedVersion(String task) {
        try {
            Activation response = client.get()
                    .uri("/internal/assessments/ai-prompt-activations?task={task}", task)
                    .headers(headers -> {
                        headers.setBearerAuth(GatewayRequestHeaders.bearerToken().orElseThrow(
                                () -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));
                        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
                    })
                    .retrieve().body(Activation.class);
            if (response == null || !task.equals(response.task())
                    || response.promptVersion() != null && response.promptVersion().isBlank()) {
                throw unavailable();
            }
            return response.promptVersion();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private LearningRequestException unavailable() {
        return new LearningRequestException(503, "PROMPT_VERSION_UNAVAILABLE", "Essay prompt activation is unavailable");
    }

    private record Activation(String task, String promptVersion) {}
}
