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
        return new Setup(new OpenAiCompatibleLlmClient(builder.build(), BASE_URL, "test-key", model, effort), server);
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
    void openAiReasoningModelsGetMaxCompletionTokensInsteadOfMaxTokens() {
        Setup reasoning = client("gpt-5-mini", "");
        reasoning.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.max_completion_tokens").value(8192))
                .andExpect(jsonPath("$.max_tokens").doesNotExist())
                .andRespond(withSuccess(reply("\"x\""), MediaType.APPLICATION_JSON));
        assertEquals("x", reasoning.client().completeJson("s", "u"));
        reasoning.server().verify();

        Setup classic = client("gpt-4o-mini", "");
        classic.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andExpect(jsonPath("$.max_completion_tokens").doesNotExist())
                .andRespond(withSuccess(reply("\"y\""), MediaType.APPLICATION_JSON));
        assertEquals("y", classic.client().completeJson("s", "u"));
        classic.server().verify();
    }

    @Test
    void aBadRequestNamesTheRejectedFieldButNeverTheProviderMessage(CapturedOutput output) {
        Setup setup = client("gpt-5-mini", "");
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"private essay echoed here\",\"type\":\"invalid_request_error\","
                                + "\"param\":\"reasoning_effort\",\"code\":\"unsupported_value\"}}"));

        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        assertTrue(output.getAll().contains("status=400, code=unsupported_value, param=reasoning_effort, "
                + "type=invalid_request_error"));
        assertFalse(output.getAll().contains("private essay echoed here"));
        setup.server().verify();
    }

    @Test
    void aGeminiStyleErrorArrayNamesTheMentionedFieldWithoutItsMessage(CapturedOutput output) {
        Setup setup = client("gemini-3.8-flash", "");
        setup.server().expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\","
                                + "\"message\":\"Invalid value for reasoning_effort near private learner essay\"}}]"));

        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u"));
        assertTrue(output.getAll().contains("status=400, code=400, status=INVALID_ARGUMENT, mentions=reasoning_effort"));
        assertFalse(output.getAll().contains("private learner essay"));
        setup.server().verify();
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
        var client = new OpenAiCompatibleLlmClient(builder.build(), "", "", "", "");
        assertFalse(client.configured());
        assertThrows(LlmUnavailableException.class, () -> client.completeJson("s", "u"));
        assertFalse(new OpenAiCompatibleLlmClient(RestClient.create(), BASE_URL, "", "m", "").configured());
    }

    @Test
    void emptyTextReachesTheBusinessParserInsteadOfBecomingAProviderFailure() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(reply("\"\""), MediaType.APPLICATION_JSON));
        assertEquals("", setup.client().completeJson("s", "u"));
        setup.server().verify();
    }

    @Test
    void classpathTemplateComesFromTheSharedContract() throws Exception {
        try (var input = getClass().getResourceAsStream("/essay-prompt-template-v1.txt")) {
            assertNotNull(input);
            assertArrayEquals(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("..", "..", "docs", "contracts",
                    "essay-prompt-template-v1.txt")), input.readAllBytes());
        }
    }
}
