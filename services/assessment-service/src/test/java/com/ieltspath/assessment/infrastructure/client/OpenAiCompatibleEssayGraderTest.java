package com.ieltspath.assessment.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.exception.EssayGradingException;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.infrastructure.config.AssessmentLlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiCompatibleEssayGraderTest {
    @Test
    void activatedPromptReadsOnlyItsPinnedPublishedRubric() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ActiveRubricProvider provider = mock(ActiveRubricProvider.class);
        when(provider.getVersion("PINNED", "1")).thenReturn(pinnedRubric("PINNED", 1));
        String reply = "{\"criteria\":[{\"code\":\"TR\",\"band\":6},{\"code\":\"CC\",\"band\":6},"
                + "{\"code\":\"LR\",\"band\":6},{\"code\":\"GRA\",\"band\":6}]}";
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(Map.of("choices", List.of(Map.of(
                        "message", Map.of("content", reply))))), MediaType.APPLICATION_JSON));
        var grader = new OpenAiCompatibleEssayGrader(builder.build(), properties(), new ObjectMapper(), provider,
                () -> Set.of());

        var grade = grader.grade(new EssayGradingPort.Prompt("Discuss", "TASK_2", 250, null,
                "PINNED@1+tplv1"), "candidate essay");

        assertEquals("PINNED@1+tplv1", grade.promptVersion());
        verify(provider).getVersion("PINNED", "1");
        verify(provider, never()).getActive(anyString());
        server.verify();
    }

    @Test
    void missingPinnedRubricDoesNotFallBackToActive() {
        ActiveRubricProvider provider = mock(ActiveRubricProvider.class);
        var grader = new OpenAiCompatibleEssayGrader(RestClient.create(), properties(), new ObjectMapper(),
                provider, () -> Set.of());

        var error = assertThrows(EssayGradingException.class, () -> grader.grade(new EssayGradingPort.Prompt(
                "Discuss", "TASK_2", 250, null, "PINNED@1+tplv1"), "candidate essay"));

        assertEquals("RUBRIC_UNAVAILABLE", error.getCode());
        verify(provider, never()).getActive(anyString());
    }

    @Test
    void mismatchedPinnedRubricDoesNotCallTheModelOrFallBackToActive() {
        ActiveRubricProvider provider = mock(ActiveRubricProvider.class);
        when(provider.getVersion("PINNED", "1")).thenReturn(pinnedRubric("OTHER", 2));
        var grader = new OpenAiCompatibleEssayGrader(RestClient.create(), properties(), new ObjectMapper(),
                provider, () -> Set.of());

        var error = assertThrows(EssayGradingException.class, () -> grader.grade(new EssayGradingPort.Prompt(
                "Discuss", "TASK_2", 250, null, "PINNED@1+tplv1"), "candidate essay"));

        assertEquals("PROMPT_VERSION_UNAVAILABLE", error.getCode());
        verify(provider, never()).getActive(anyString());
    }

    private static ActiveRubricProvider.Rubric pinnedRubric(String code, int version) {
        var criteria = List.of("TR", "CC", "LR", "GRA").stream()
                .map(criterion -> new ActiveRubricProvider.Criterion(criterion, criterion, "Descriptor", 1, List.of()))
                .toList();
        return new ActiveRubricProvider.Rubric(code, "TASK_2", version, "source", criteria, List.of());
    }

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

        EssayGradingPort grader = grader(builder.build(), properties(), new ObjectMapper());

        EssayGradingPort.EssayGrade grade = grader.grade(
                new EssayGradingPort.Prompt("Discuss the topic", "TASK_2", 250, null), "candidate essay");
        assertEquals(new BigDecimal("6.0"), grade.band());
        assertNull(grade.feedback(), "a reply with bands only carries no comments");
        server.verify();
    }

    @Test
    void keepsTheExaminerCommentsBesideTheBand() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String reply = new ObjectMapper().writeValueAsString(Map.of(
                "criteria", List.of(
                        Map.of("code", "TR", "band", 5.0, "comment", "  Lạc đề: \"social networks\" → bám sát đề.  "),
                        Map.of("code", "CC", "band", 6.0, "comment", "Bố cục rõ."),
                        Map.of("code", "LR", "band", 6.0),
                        Map.of("code", "GRA", "band", 5.5, "comment", "")),
                "summary", "Bài viết cần bám đề hơn.",
                "focus", List.of("PEEL Structure: triển khai ý", 7)));
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(Map.of("choices", List.of(Map.of(
                        "message", Map.of("content", reply))))), MediaType.APPLICATION_JSON));

        EssayGradingPort.EssayGrade grade = grader(builder.build(), properties(), new ObjectMapper())
                .grade(new EssayGradingPort.Prompt("Discuss the topic", "TASK_2", 250, null), "candidate essay");

        assertEquals(new BigDecimal("5.5"), grade.band());
        var feedback = new ObjectMapper().readTree(grade.feedback());
        assertEquals("Bài viết cần bám đề hơn.", feedback.path("summary").asText());
        assertEquals("Lạc đề: \"social networks\" → bám sát đề.", feedback.path("criteria").path(0).path("comment").asText());
        assertTrue(feedback.path("criteria").path(2).path("comment").isMissingNode(), "a missing comment stays omitted");
        assertTrue(feedback.path("criteria").path(3).path("comment").isMissingNode(), "a blank comment stays omitted");
        assertEquals(1, feedback.path("focus").size(), "non-text focus items are dropped");
        server.verify();
    }

    @Test
    void invalidLlmReplyIsReportedAsSafeGradingFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        EssayGradingPort grader = grader(builder.build(), properties(), new ObjectMapper());

        EssayGradingException error = assertThrows(EssayGradingException.class,
                () -> grader.grade(new EssayGradingPort.Prompt("Discuss", "TASK_2", 250, null), "essay"));
        assertEquals("INVALID_GRADE", error.getCode());
        server.verify();
    }

    @Test
    void taskOneRequiresChartFactsBeforeMakingHttpRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EssayGradingPort grader = grader(builder.build(), properties(), new ObjectMapper());

        EssayGradingException error = assertThrows(EssayGradingException.class,
                () -> grader.grade(new EssayGradingPort.Prompt("Describe the chart", "TASK_1", 150, null), "essay"));
        assertEquals("INVALID_PROMPT", error.getCode());
        server.verify();
    }

    @Test
    void buildsTheChatCompletionsUrlFromDifferentBaseUrlShapes() throws Exception {
        String reply = new ObjectMapper().writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of(
                "content", "{\"criteria\":[{\"code\":\"TR\",\"band\":6.0},{\"code\":\"CC\",\"band\":6.0},"
                        + "{\"code\":\"LR\",\"band\":6.0},{\"code\":\"GRA\",\"band\":6.0}]}")))));
        Map<String, String> expectedUrlByBase = Map.of(
                "https://llm.example", "https://llm.example/v1/chat/completions",
                "https://llm.example/", "https://llm.example/v1/chat/completions",
                "https://llm.example/v1", "https://llm.example/v1/chat/completions",
                "https://generativelanguage.googleapis.com/v1beta/openai/",
                "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions");
        for (var entry : expectedUrlByBase.entrySet()) {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(entry.getValue())).andRespond(withSuccess(reply, MediaType.APPLICATION_JSON));
            AssessmentLlmProperties properties = properties();
            properties.setBaseUrl(entry.getKey());

            grader(builder.build(), properties, new ObjectMapper())
                    .grade(new EssayGradingPort.Prompt("Discuss", "TASK_2", 250, null), "essay");
            server.verify();
        }
    }

    @Test
    void removesCorrectionsWithErrorCodesOutsideTheCatalog() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String reply = "{\"criteria\":[{\"code\":\"TR\",\"band\":6},{\"code\":\"CC\",\"band\":6},"
                + "{\"code\":\"LR\",\"band\":6},{\"code\":\"GRA\",\"band\":6}],"
                + "\"corrections\":[{\"excerpt\":\"bad\",\"suggestion\":\"fix\",\"errorCode\":\"UNKNOWN\"},"
                + "{\"excerpt\":\"writed\",\"suggestion\":\"wrote\",\"errorCode\":\"GRAMMAR\"}]}";
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(Map.of("choices", List.of(Map.of(
                        "message", Map.of("content", reply))))), MediaType.APPLICATION_JSON));
        var grade = grader(builder.build(), properties(), new ObjectMapper()).grade(
                new EssayGradingPort.Prompt("Discuss", "TASK_2", 250, null), "I writed it.");
        assertEquals(List.of("GRAMMAR"), grade.errorCodes());
        assertEquals(1, new ObjectMapper().readTree(grade.feedback()).path("corrections").size());
        server.verify();
    }

    private EssayGradingPort grader(RestClient restClient, AssessmentLlmProperties properties, ObjectMapper json) {
        var provider = new com.ieltspath.assessment.application.port.ActiveRubricProvider() {
            @Override public Rubric getActive(String task) { return rubric(task); }
            @Override public Rubric getVersion(String code, String version) { return rubric("TASK_2"); }
            private Rubric rubric(String task) {
                var codes = "TASK_1".equals(task) ? List.of("TA", "CC", "LR", "GRA") : List.of("TR", "CC", "LR", "GRA");
                return new Rubric("rubric-" + task.toLowerCase(), task, 1, "source",
                        java.util.stream.IntStream.range(0, codes.size()).mapToObj(i -> new Criterion(codes.get(i), codes.get(i),
                                "description", i, List.of())).toList(), List.of());
            }
        };
        return new OpenAiCompatibleEssayGrader(restClient, properties, json, provider, () -> Set.of("GRAMMAR"));
    }

    private AssessmentLlmProperties properties() {
        AssessmentLlmProperties properties = new AssessmentLlmProperties();
        properties.setBaseUrl("https://llm.example");
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        return properties;
    }
}
