package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.learning.application.exception.LlmUnavailableException;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiCompatibleLlmClientTest {
    private static final String BASE_URL = "http://llm.test/v1";

    private record Setup(OpenAiCompatibleLlmClient client, MockRestServiceServer server) {}

    private static Setup client(String model, String effort) {
        return client(model, effort, Duration.ofMinutes(1));
    }

    /** {@code retryWindow}: a busy answer slower than this is not asked again; the pause before a repeat is zero. */
    private static Setup client(String model, String effort, Duration retryWindow) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Setup(new OpenAiCompatibleLlmClient(builder.build(), "test-key", model, effort, Duration.ZERO,
                retryWindow), server);
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

        assertEquals("{\"criteria\":[]}", setup.client().completeJson("system text", "user text", false));
        setup.server().verify();
    }

    @Test
    void anExplicitReasoningEffortDropsTemperatureAndARetryAsksForLowEffort() {
        Setup setup = client("gemini-2.5-flash", "medium");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.reasoning_effort").value("medium"))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andRespond(withSuccess(reply("\"x\""), MediaType.APPLICATION_JSON));
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.reasoning_effort").value("low"))
                .andRespond(withSuccess(reply("\"y\""), MediaType.APPLICATION_JSON));

        assertEquals("x", setup.client().completeJson("s", "u", false));
        assertEquals("y", setup.client().completeJson("s", "u", true));
        setup.server().verify();
    }

    @Test
    void providerErrorsAndUnusableRepliesAreUnavailable() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u", false));
        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u", false));
        setup.server().verify();
    }

    @Test
    void aBusyProviderIsAskedAgainOnceAndTheSecondReplyIsUsed() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(reply("\"graded\""), MediaType.APPLICATION_JSON));

        assertEquals("graded", setup.client().completeJson("s", "u", false));
        setup.server().verify();
    }

    @Test
    void aRateLimitedRequestIsAskedAgainOnce() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(reply("\"graded\""), MediaType.APPLICATION_JSON));

        assertEquals("graded", setup.client().completeJson("s", "u", false));
        setup.server().verify();
    }

    @Test
    void aProviderThatStaysBusyIsGivenUpOnAfterTheSecondAttempt() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("s", "u", false));
        assertTrue(failure.getMessage().contains("503"));
        setup.server().verify();
    }

    @Test
    void aBusyAnswerThatCameTooLateLeavesNoRoomToAskAgain() {
        Setup setup = client("gpt-4o-mini", "", Duration.ZERO);
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("s", "u", false));
        assertTrue(failure.getMessage().contains("503"));
        setup.server().verify();
    }

    @Test
    void errorsOtherThanBusyAreNotAskedAgain() {
        Setup setup = client("gemini-3.8-flash", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        LlmUnavailableException failure = assertThrows(LlmUnavailableException.class,
                () -> setup.client().completeJson("s", "u", false));
        assertTrue(failure.getMessage().contains("400"));
        setup.server().verify();
    }

    @Test
    void aBrokenConnectionIsNotAskedAgain() {
        Setup setup = client("gpt-4o-mini", "");
        setup.server().expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withException(new IOException("connection reset")));

        assertThrows(LlmUnavailableException.class, () -> setup.client().completeJson("s", "u", false));
        setup.server().verify();
    }

    @Test
    void anUnconfiguredClientNeverCallsOut() {
        RestClient.Builder builder = RestClient.builder();
        var client = new OpenAiCompatibleLlmClient(builder, "", "", "", "", 20);
        assertFalse(client.configured());
        assertThrows(LlmUnavailableException.class, () -> client.completeJson("s", "u", false));
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
