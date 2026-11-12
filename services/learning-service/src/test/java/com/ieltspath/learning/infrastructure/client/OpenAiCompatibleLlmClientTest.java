package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.learning.application.exception.LlmUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(OutputCaptureExtension.class)
class OpenAiCompatibleLlmClientTest {
    private static final String BASE_URL = "http://llm.test/v1";

    private record Setup(OpenAiCompatibleLlmClient client, MockRestServiceServer server) {}

    private static Setup client(String model, String effort) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Setup(new OpenAiCompatibleLlmClient(builder.build(), "test-key", model, effort), server);
    }

    private static String reply(String content) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":%s}}]}".formatted(content);
    }

    @Test
    void sendsAJsonRequestWithTemperatureZeroWhenNoReasoningIsRequested() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("user text"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.temperature").value(0.0))
                .andExpect(jsonPath("$.reasoning_effort").doesNotExist())
                .andRespond(withSuccess(reply("\"{\\\"criteria\\\":[]}\""), MediaType.APPLICATION_JSON));

        assertEquals("{\"criteria\":[]}", setup.client().completeJson("system text", "user text"));
        setup.server().verify();
    }

    @Test
    void theDefaultOutputBudgetIsKeptAndAnExplicitBudgetReplacesIt() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions")).andExpect(jsonPath("$.max_tokens").value(4096))
                .andRespond(withSuccess(reply("\"x\""), MediaType.APPLICATION_JSON));
        setup.server().expect(requestTo(BASE_URL + "/chat/completions")).andExpect(jsonPath("$.max_tokens").value(12000))
                .andRespond(withSuccess(reply("\"y\""), MediaType.APPLICATION_JSON));

        assertEquals("x", setup.client().completeJson("s", "u"));
        assertEquals("y", setup.client().completeJson("s", "u", new com.ieltspath.learning.application.port.LlmClient.Budget(12000, 0)));
        setup.server().verify();
    }

    /** The longer wait is applied to a copy of the client, which must still reach the same endpoint with the same key. */
    @Test
    void aBudgetWithItsOwnTimeoutStillReachesTheEndpointWithTheKeyAndTheBudget() throws IOException {
        var seen = new java.util.concurrent.atomic.AtomicReference<String>();
        var http = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/v1/chat/completions", exchange -> {
            seen.set(exchange.getRequestHeaders().getFirst("Authorization") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            byte[] body = reply("\"ok\"").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        http.start();
        try {
            var client = new OpenAiCompatibleLlmClient(RestClient.builder().baseUrl("http://127.0.0.1:" + http.getAddress().getPort() + "/v1").build(),
                    "test-key", "gpt-4o-mini", "");

            assertEquals("ok", client.completeJson("s", "u", new com.ieltspath.learning.application.port.LlmClient.Budget(12000, 30)));
            assertTrue(seen.get().startsWith("Bearer test-key "));
            assertTrue(seen.get().contains("\"max_tokens\":12000"));
        } finally {
            http.stop(0);
        }
    }

    @Test
    void requestStringDoesNotRenderMessageContent() {
        var request = new OpenAiCompatibleLlmClient.Request("model", java.util.List.of(
                new OpenAiCompatibleLlmClient.Message("system", "private system prompt"),
                new OpenAiCompatibleLlmClient.Message("user", "private learner essay")),
                4096, 0.0, new OpenAiCompatibleLlmClient.ResponseFormat("json_object"), null);

        assertFalse(request.toString().contains("private system prompt"));
        assertFalse(request.toString().contains("private learner essay"));
    }

    @Test
    void anExplicitReasoningEffortDropsTemperature() {
        Setup setup = client("gemini-2.5-flash", "medium");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.reasoning_effort").value("medium"))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andRespond(withSuccess(reply("\"x\""), MediaType.APPLICATION_JSON));

        assertEquals("x", setup.client().completeJson("s", "u"));
        setup.server().verify();
    }

    @Test
    void providerErrorsAndUnusableRepliesAreUnavailable() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        setup.server().verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void aBusyProviderFailsAfterExactlyOneRequestWithoutLeakingText(int status, CapturedOutput output) {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body("private provider body")
                        .contentType(MediaType.TEXT_PLAIN));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("private system prompt", "private learner essay"));
        assertEquals("LLM provider request failed (status " + status + ")", failure.getMessage());
        assertNull(failure.getCause());
        assertTrue(output.getAll().contains("status=" + status));
        assertFalse(output.getAll().contains("private provider body"));
        assertFalse(output.getAll().contains("private system prompt"));
        assertFalse(output.getAll().contains("private learner essay"));
        assertFalse(output.getAll().contains("test-key"));
        setup.server().verify();
    }

    private static final com.ieltspath.learning.application.port.LlmClient.Budget RETRYING =
            new com.ieltspath.learning.application.port.LlmClient.Budget(8000, 0, true);

    private static Setup resilientClient() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Setup(new OpenAiCompatibleLlmClient(builder.build(), "test-key", "gpt-4o-mini", "",
                com.ieltspath.commonresilience.ResilientCall.isolatedDefault()), server);
    }

    @Test
    void aBudgetThatRetriesAsksAgainWhenTheProviderIsBusyWithA503() {
        Setup setup = resilientClient();
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(reply("\"ok\""), MediaType.APPLICATION_JSON));

        assertEquals("ok", setup.client().completeJson("s", "u", RETRYING));
        setup.server().verify();
    }

    @Test
    void aBudgetThatRetriesGivesUpWhenTheRetryIsBusyToo() {
        Setup setup = resilientClient();
        setup.server().expect(ExpectedCount.times(2), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("s", "u", RETRYING));
        assertEquals("LLM provider request failed (status 503)", failure.getMessage());
        setup.server().verify();
    }

    @Test
    void aBudgetThatDoesNotAskForTheRetryKeepsTheSingleRequestOfEveryOtherCall() {
        Setup setup = resilientClient();
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        setup.server().verify();
    }

    @Test
    void aGoneQuotaIsNeverAskedAgainEvenByABudgetThatRetries() {
        Setup setup = resilientClient();
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("s", "u", RETRYING));
        assertEquals("LLM provider request failed (status 429)", failure.getMessage());
        setup.server().verify();
    }

    @Test
    void errorsOtherThanBusyAreNotAskedAgain() {
        Setup setup = client("gemini-3.8-flash", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("s", "u"));
        assertTrue(failure.getMessage().contains("400"));
        setup.server().verify();
    }

    @Test
    void aRejectedRequestLogsTheProviderIdentifiersAndTheStartOfItsMessage(CapturedOutput output) {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\",\"param\":\"max_tokens\","
                                + "\"message\":\"max_tokens is too large\\nfor this model " + "x".repeat(400) + "\"}}]"));

        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        assertTrue(output.getAll().contains("status=400"));
        assertTrue(output.getAll().contains("providerCode=400"));
        assertTrue(output.getAll().contains("providerStatus=INVALID_ARGUMENT"));
        assertTrue(output.getAll().contains("providerParam=max_tokens"));
        assertTrue(output.getAll().contains("providerMessage=max_tokens is too large for this model xxx"));
        assertFalse(output.getAll().contains("x".repeat(320)));
    }

    @Test
    void aBusyProviderMessageIsNotLogged(CapturedOutput output) {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":429,\"message\":\"private learner essay\"}}"));

        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        assertTrue(output.getAll().contains("providerCode=429"));
        assertFalse(output.getAll().contains("private learner essay"));
    }

    @Test
    void aBrokenConnectionIsNotAskedAgain() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withException(new IOException("connection reset")));

        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        setup.server().verify();
    }

    @Test
    void aNetworkTimeoutFailsAfterExactlyOneRequestWithoutLeakingText(CapturedOutput output) {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withException(new SocketTimeoutException("private network failure details")));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("private system prompt", "private learner essay"));
        assertEquals("LLM provider request failed (status null)", failure.getMessage());
        assertNull(failure.getCause());
        assertFalse(output.getAll().contains("private network failure details"));
        assertFalse(output.getAll().contains("private system prompt"));
        assertFalse(output.getAll().contains("private learner essay"));
        assertFalse(output.getAll().contains("test-key"));
        setup.server().verify();
    }

    @Test
    void anUnconfiguredClientNeverCallsOut() {
        RestClient.Builder builder = RestClient.builder();
        var client = new OpenAiCompatibleLlmClient(builder, "", "", "", "", 20);
        assertFalse(client.configured());
        assertThrows(LlmUnavailableException.class, () -> client.completeJson("s", "u"));
        assertFalse(new OpenAiCompatibleLlmClient(RestClient.builder(), BASE_URL, "", "m", "", 20).configured());
    }

    @Test
    void reasoningDefaultsFollowTheModelFamily() {
        assertEquals("none", OpenAiCompatibleLlmClient.resolveReasoningEffort("gemini-2.5-flash", null));
        assertEquals("minimal", OpenAiCompatibleLlmClient.resolveReasoningEffort("gemini-2.5-pro", ""));
        assertNull(OpenAiCompatibleLlmClient.resolveReasoningEffort("gpt-4o-mini", " "));
        assertFalse(OpenAiCompatibleLlmClient.supportsTemperature("gpt-5-mini", null));
        assertTrue(OpenAiCompatibleLlmClient.supportsTemperature("gpt-4o-mini", "none"));
    }
}
