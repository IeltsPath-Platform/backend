package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.EssayPromptActivationProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Objects;

@Component
public class RestEssayPromptActivationClient implements EssayPromptActivationProvider {
    private static final Logger log = LoggerFactory.getLogger(RestEssayPromptActivationClient.class);
    static final String MISMATCH_METRIC = "learning.writing.activation_mismatch";

    private final RestClient client;
    private final ResilientCall resilientCall;
    private final String llmModel;
    private final String llmReasoningEffort;
    private final MeterRegistry metrics;

    @Autowired
    public RestEssayPromptActivationClient(RestClient.Builder builder,
            @Value("${learning.assessment.base-url:http://localhost:8083}") String baseUrl,
            ResilientCall resilientCall,
            @Value("${learning.llm.model:}") String llmModel,
            @Value("${learning.llm.reasoning-effort:}") String llmReasoningEffort,
            MeterRegistry metrics) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.resilientCall = resilientCall;
        this.llmModel = llmModel;
        this.llmReasoningEffort = llmReasoningEffort;
        this.metrics = metrics;
    }

    RestEssayPromptActivationClient(RestClient client) { this(client, defaultResilientCall()); }

    RestEssayPromptActivationClient(RestClient client, ResilientCall resilientCall) {
        this(client, resilientCall, "", "", new SimpleMeterRegistry());
    }

    RestEssayPromptActivationClient(RestClient client, ResilientCall resilientCall, String llmModel,
            String llmReasoningEffort, MeterRegistry metrics) {
        this.client = client;
        this.resilientCall = resilientCall;
        this.llmModel = llmModel;
        this.llmReasoningEffort = llmReasoningEffort;
        this.metrics = metrics;
    }

    @Override
    public String activatedVersion(String task) {
        try {
            Activation response = resilientCall.call("assessment", CallKind.READ, () -> client.get()
                    .uri("/internal/assessments/ai-prompt-activations?task={task}", task)
                    .headers(headers -> {
                        headers.setBearerAuth(GatewayRequestHeaders.bearerToken().orElseThrow(
                                () -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));
                        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
                    })
                    .retrieve().body(Activation.class));
            if (response == null || !task.equals(response.task())
                    || response.promptVersion() != null && response.promptVersion().isBlank()) {
                throw unavailable();
            }
            reportSettingMismatch(task, response);
            return response.promptVersion();
        } catch (DownstreamUnavailableException exception) {
            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    /**
     * Compares the model settings of the activated run with this service's. A difference is counted and logged;
     * it does not block grading, because a block would stop all writing grades over one configuration slip.
     * Runs recorded before the settings were stored carry no model, so there is nothing to compare.
     */
    private void reportSettingMismatch(String task, Activation response) {
        if (response.model() == null) return;
        if (!same(response.model(), llmModel)) mismatch(task, "model");
        if (!same(response.reasoningEffort(), llmReasoningEffort)) mismatch(task, "reasoning_effort");
    }

    private void mismatch(String task, String field) {
        metrics.counter(MISMATCH_METRIC, "task", task, "field", field).increment();
        log.warn("Writing prompt activation was graded with another {} than this service uses: task={}", field, task);
    }

    private static boolean same(String activated, String local) {
        return Objects.equals(blankToNull(activated), blankToNull(local));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResilientCall defaultResilientCall() {
        return ResilientCall.isolatedDefault();
    }

    private LearningRequestException unavailable() {
        return new LearningRequestException(503, "PROMPT_VERSION_UNAVAILABLE", "Essay prompt activation is unavailable");
    }

    private record Activation(String task, String promptVersion, String model, String reasoningEffort) {}
}
