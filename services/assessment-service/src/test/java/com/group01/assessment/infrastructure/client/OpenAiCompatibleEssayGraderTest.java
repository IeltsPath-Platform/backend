package com.group01.assessment.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.application.exception.EssayGradingException;
import com.group01.assessment.application.port.EssayGradingPort;
import com.group01.assessment.infrastructure.config.AssessmentLlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiCompatibleEssayGraderTest {
    @Test
    void postsEssayToConfiguredOpenAiCompatibleEndpointAndReturnsMeanBand() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("candidate essay")))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(Map.of("choices", List.of(Map.of(
                        "message", Map.of("content", "{\"criteria\":[{\"code\":\"TR\",\"band\":6.0},"
                                + "{\"code\":\"CC\",\"band\":6.5},{\"code\":\"LR\",\"band\":5.5},"
                                + "{\"code\":\"GRA\",\"band\":6.0}]}"))))), MediaType.APPLICATION_JSON));

        EssayGradingPort grader = new OpenAiCompatibleEssayGrader(builder.build(), properties(), new ObjectMapper());

        assertEquals(new BigDecimal("6.0"), grader.grade(
                new EssayGradingPort.Prompt("Discuss the topic", "TASK_2", 250, null), "candidate essay"));
        server.verify();
    }

    @Test
    void invalidLlmReplyIsReportedAsSafeGradingFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        EssayGradingPort grader = new OpenAiCompatibleEssayGrader(builder.build(), properties(), new ObjectMapper());

        EssayGradingException error = assertThrows(EssayGradingException.class,
                () -> grader.grade(new EssayGradingPort.Prompt("Discuss", "TASK_2", 250, null), "essay"));
        assertEquals("INVALID_GRADE", error.getCode());
        server.verify();
    }

    @Test
    void taskOneRequiresChartFactsBeforeMakingHttpRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EssayGradingPort grader = new OpenAiCompatibleEssayGrader(builder.build(), properties(), new ObjectMapper());

        EssayGradingException error = assertThrows(EssayGradingException.class,
                () -> grader.grade(new EssayGradingPort.Prompt("Describe the chart", "TASK_1", 150, null), "essay"));
        assertEquals("INVALID_PROMPT", error.getCode());
        server.verify();
    }

    private AssessmentLlmProperties properties() {
        AssessmentLlmProperties properties = new AssessmentLlmProperties();
        properties.setBaseUrl("https://llm.example");
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        return properties;
    }
}
