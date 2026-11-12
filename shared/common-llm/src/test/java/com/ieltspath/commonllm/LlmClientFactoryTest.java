package com.ieltspath.commonllm;

import com.ieltspath.commonresilience.ResilientCall;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(OutputCaptureExtension.class)
class LlmClientFactoryTest {
    private static final String BASE = "https://llm.test/v1";
    private record Setup(LlmChatClient client, MockRestServiceServer server, SimpleMeterRegistry metrics) {}
    private Setup setup(String model, String effort) {
        return setup(model, effort, ResilientCall.isolatedDefault());
    }
    private Setup setup(String model, String effort, ResilientCall resilience) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var metrics = new SimpleMeterRegistry();
        var endpoint = new LlmEndpoint(BASE + "/", "private-key", model, effort, Duration.ofSeconds(20), 8192);
        return new Setup(new LlmClientFactory(builder, resilience, metrics)
                .create(endpoint, "test-service", builder.build()), server, metrics);
    }
    private ChatRequest request(boolean json, Integer tokens) {
        return new ChatRequest("test-purpose", "private-system", "private-essay", json, tokens);
    }
    private static String reply() { return "{\"choices\":[{\"message\":{\"content\":\"done\"}}]}"; }

    @ParameterizedTest
    @CsvSource({"gpt-4o-mini,,max_tokens,0.0", "gpt-5-mini,,max_completion_tokens,",
            "o1-mini,,max_completion_tokens,", "o3,,max_completion_tokens,", "o4-mini,,max_completion_tokens,",
            "gemini-2.5-flash,none,max_tokens,0.0", "gemini-2.5-pro,minimal,max_tokens,0.0",
            "gemini-3-flash,minimal,max_tokens,0.0"})
    void modelFamiliesPreserveRequestRules(String model, String expectedEffort, String tokenField,
                                           Double temperature) {
        var setup = setup(model, "");
        var expected = setup.server().expect(requestTo(BASE + "/chat/completions"))
                .andExpect(header("Authorization", "Bearer private-key"))
                .andExpect(jsonPath("$." + tokenField).value(8192))
                .andExpect(jsonPath("$." + (tokenField.equals("max_tokens") ? "max_completion_tokens" : "max_tokens")).doesNotExist())
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("private-essay"));
        if (expectedEffort == null) expected.andExpect(jsonPath("$.reasoning_effort").doesNotExist());
        else expected.andExpect(jsonPath("$.reasoning_effort").value(expectedEffort));
        if (temperature == null) expected.andExpect(jsonPath("$.temperature").doesNotExist());
        else expected.andExpect(jsonPath("$.temperature").value(temperature));
        expected.andRespond(withSuccess(reply(), MediaType.APPLICATION_JSON));
        assertEquals("done", setup.client().chat(request(true, null)).content());
        setup.server().verify();
    }

    @Test void explicitEffortDropsTemperatureAndNoneMapsToMinimalForGemini() {
        assertEquals("minimal", LlmClientFactory.resolveReasoningEffort("gemini-3-flash", "none"));
        var setup = setup("gemini-2.5-flash", " HIGH ");
        setup.server().expect(requestTo(BASE + "/chat/completions"))
                .andExpect(jsonPath("$.reasoning_effort").value("high"))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andRespond(withSuccess(reply(), MediaType.APPLICATION_JSON));
        setup.client().chat(request(true, null));
        setup.server().verify();
    }

    @Test void plainTextModeOmitsFormatAndNullSystemPromptAndHonorsBudgetOverride() {
        var setup = setup("classic", "");
        setup.server().expect(requestTo(BASE + "/chat/completions"))
                .andExpect(jsonPath("$.response_format").doesNotExist())
                .andExpect(jsonPath("$.max_tokens").value(123))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andRespond(withSuccess(reply(), MediaType.APPLICATION_JSON));
        setup.client().chat(new ChatRequest("test-purpose", null, "private-essay", false, 123));
        setup.server().verify();
    }

    @Test void usageAndDurationAreRecordedWithBoundedLabels() {
        var setup = setup("configured-model", "");
        setup.server().expect(requestTo(BASE + "/chat/completions")).andRespond(withSuccess(
                "{\"model\":\"provider-model\",\"choices\":[{\"message\":{\"content\":\"done\"}}],"
                        + "\"usage\":{\"prompt_tokens\":42,\"completion_tokens\":17}}", MediaType.APPLICATION_JSON));
        var reply = setup.client().chat(request(true, null));
        assertEquals("provider-model", reply.model());
        assertEquals(42L, reply.inputTokens());
        assertEquals(17L, reply.outputTokens());
        assertEquals(42, setup.metrics().get("llm.tokens").tag("direction", "input").counter().count());
        assertEquals(17, setup.metrics().get("llm.tokens").tag("direction", "output").counter().count());
        var timer = setup.metrics().get("llm.call.duration").tags("service", "test-service", "model", "configured-model",
                "purpose", "test-purpose", "outcome", "success").timer();
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) > 0);
        setup.server().verify();
    }

    @Test void missingOrInvalidUsageDoesNotInventTokens() {
        var setup = setup("model", "");
        setup.server().expect(requestTo(BASE + "/chat/completions")).andRespond(withSuccess(reply(), MediaType.APPLICATION_JSON));
        setup.server().expect(requestTo(BASE + "/chat/completions")).andRespond(withSuccess(
                "{\"usage\":{\"prompt_tokens\":-2,\"completion_tokens\":\"17\"},"
                        + "\"choices\":[{\"message\":{\"content\":\"done\"}}]}", MediaType.APPLICATION_JSON));
        assertNull(setup.client().chat(request(true, null)).inputTokens());
        assertNull(setup.client().chat(request(true, null)).outputTokens());
        assertTrue(setup.metrics().find("llm.tokens").counters().isEmpty());
        setup.server().verify();
    }

    @ParameterizedTest @ValueSource(ints = {400, 401, 429, 500, 503, 504})
    void httpErrorsFailAfterOneRequestWithSafeCodeAndMetrics(int status, CapturedOutput output) {
        var setup = setup("model", "");
        setup.server().expect(requestTo(BASE + "/chat/completions")).andRespond(
                withStatus(HttpStatus.valueOf(status)).body("private-provider-body"));
        var failure = assertThrows(LlmCallException.class, () -> setup.client().chat(request(true, null)));
        assertEquals("HTTP_" + status, failure.code());
        assertNull(failure.getCause());
        assertEquals(1, setup.metrics().get("llm.call.duration").tag("outcome", "HTTP_" + status).timer().count());
        for (String privateText : List.of("private-system", "private-essay", "private-key", "private-provider-body"))
            assertFalse(output.getAll().contains(privateText));
        setup.server().verify();
    }

    @Test void logsOnlyMachineFieldsAndKnownRequestFieldNames(CapturedOutput output) {
        var setup = setup("model", "");
        setup.server().expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body("[{\"error\":{\"code\":400,\"param\":\"reasoning_effort\","
                        + "\"status\":\"INVALID_ARGUMENT\",\"message\":\"reasoning_effort private-essay\"}}]"));
        assertThrows(LlmCallException.class, () -> setup.client().chat(request(true, null)));
        assertTrue(output.getAll().contains("code=400, param=reasoning_effort, status=INVALID_ARGUMENT, mentions=reasoning_effort"));
        assertFalse(output.getAll().contains("private-essay"));
        setup.server().verify();
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"choices\":[]}", "not-json",
            "{\"choices\":[{\"message\":{\"content\":123}}]}"})
    void unusableResponsesHaveSafeBadResponseCode(String body) {
        var setup = setup("model", "");
        setup.server().expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertEquals("BAD_RESPONSE", assertThrows(LlmCallException.class,
                () -> setup.client().chat(request(true, null))).code());
        setup.server().verify();
    }

    @Test void emptyTextRemainsTheCallingServicesParsingDecision() {
        var setup = setup("model", "");
        setup.server().expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"\"}}]}", MediaType.APPLICATION_JSON));
        assertEquals("", setup.client().chat(request(true, null)).content());
        setup.server().verify();
    }

    @Test void timeoutAndConnectionFailuresAreNotRetried(CapturedOutput output) {
        for (IOException cause : List.of(new SocketTimeoutException("private-network"), new IOException("private-network"))) {
            var setup = setup("model", "");
            setup.server().expect(requestTo(BASE + "/chat/completions")).andRespond(withException(cause));
            var failure = assertThrows(LlmCallException.class, () -> setup.client().chat(request(true, null)));
            assertEquals(cause instanceof SocketTimeoutException ? "TIMEOUT" : "UNAVAILABLE", failure.code());
            assertNull(failure.getCause());
            setup.server().verify();
        }
        assertFalse(output.getAll().contains("private-network"));
    }

    @Test void circuitOpenAndFullBulkheadAreSafeAndNeverCallHttp() {
        var breakers = CircuitBreakerRegistry.ofDefaults();
        breakers.circuitBreaker("llm").transitionToOpenState();
        var circuit = setup("model", "", new ResilientCall(breakers, RetryRegistry.ofDefaults(), BulkheadRegistry.ofDefaults()));
        assertEquals("circuit-open", assertThrows(LlmCallException.class,
                () -> circuit.client().chat(request(true, null))).reason());
        circuit.server().verify();

        var bulkheads = BulkheadRegistry.of(BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build());
        var bulkhead = bulkheads.bulkhead("llm");
        bulkhead.acquirePermission();
        try {
            var full = setup("model", "", new ResilientCall(CircuitBreakerRegistry.ofDefaults(), RetryRegistry.ofDefaults(), bulkheads));
            var failure = assertThrows(LlmCallException.class, () -> full.client().chat(request(true, null)));
            assertEquals("UNAVAILABLE", failure.code());
            assertEquals("bulkhead-full", failure.reason());
            full.server().verify();
        } finally { bulkhead.onComplete(); }
    }

    @Test void requestReplyAndEndpointStringRepresentationsHideSensitiveText() {
        assertFalse(request(true, null).toString().contains("private-"));
        assertFalse(new LlmEndpoint(BASE, "private-key", "model", "", Duration.ofSeconds(2), 1).toString().contains("private-key"));
        assertFalse(new ChatReply("private-reply", null, null, "model").toString().contains("private-reply"));
        var body = new LlmClientFactory.Request("model", List.of(new LlmClientFactory.Message("user", "private-essay")),
                8192, null, 0.0, null, null);
        assertFalse(body.toString().contains("private-essay"));
        assertFalse(body.messages().toString().contains("private-essay"));
    }

    @Test void autoConfigurationProvidesTheFactory() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CommonLlmAutoConfiguration.class))
                .withBean(RestClient.Builder.class, RestClient::builder)
                .withBean(ResilientCall.class, ResilientCall::isolatedDefault)
                .withBean(io.micrometer.core.instrument.MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertThat(context).hasSingleBean(LlmClientFactory.class));
    }
}
