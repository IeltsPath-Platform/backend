package com.ieltspath.assessment.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ieltspath.assessment.application.command.StartAssessmentAttemptCommand;
import com.ieltspath.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.ieltspath.assessment.application.result.AssessmentAttemptResult;
import com.ieltspath.assessment.application.result.AttemptStructureResult;
import com.ieltspath.assessment.application.result.LearnerAssessmentResult;
import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import com.ieltspath.assessment.domain.vo.AttemptType;
import com.ieltspath.assessment.application.usecase.*;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AssessmentAttemptController.class, AssessmentResultController.class},
        properties = "spring.cloud.config.enabled=false")
@Import(AssessmentAnswerExposureWebMvcTest.TestSecurityConfig.class)
class AssessmentAnswerExposureWebMvcTest {
    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean StartAssessmentAttemptUseCase startAttempt;
    @MockitoBean GetAssessmentAttemptUseCase getAttempt;
    @MockitoBean GetAttemptStructureUseCase getStructure;
    @MockitoBean SubmitAssessmentAttemptUseCase submitAttempt;
    @MockitoBean ExpireAssessmentAttemptUseCase expireAttempt;
    @MockitoBean SaveAttemptResponseUseCase saveResponse;
    @MockitoBean GetAssessmentResultUseCase getResult;
    @MockitoBean GetCurrentPlacementAttemptUseCase currentPlacement;
    @MockitoBean ListAttemptResponsesUseCase listResponses;
    @MockitoBean CompleteAttemptSectionUseCase completeSection;
    @MockitoBean StartAttemptSectionUseCase startSection;

    private final UUID userId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void ownerSeesStructureWithoutAnswerSnapshot() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getStructure.execute(userId, attemptId)).thenReturn(new AttemptStructureResult(List.of(
                new AttemptStructureResult.Section(sectionId, UUID.randomUUID(), 1, "{\"title\":\"Reading\"}",
                        List.of(new AttemptStructureResult.Item(itemId, UUID.randomUUID(), 1,
                                "{\"stem\":\"Question\"}", "{\"kp\":\"KP1\"}"))))));

        mockMvc.perform(get("/api/assessments/attempts/{id}/structure", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].id").value(sectionId.toString()))
                .andExpect(jsonPath("$.sections[0].snapshot.title").value("Reading"))
                .andExpect(jsonPath("$.sections[0].snapshot.skill").value("READING"))
                .andExpect(jsonPath("$.sections[0].items[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.sections[0].items[0].questionSnapshot").value("{\"stem\":\"Question\"}"))
                .andExpect(jsonPath("$.sections[0].items[0].knowledgeSnapshot").value("{\"kp\":\"KP1\"}"))
                .andExpect(jsonPath("$.sections[0].items[0].answerSnapshot").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void aLearnerWithoutPlacementGetsNoContentRatherThanAnError() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(currentPlacement.execute(userId)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/assessments/attempts/placement/current"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void completingASectionAnswersNoContent() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/assessments/attempts/{id}/sections/{sectionId}/complete", attemptId, sectionId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(completeSection).execute(userId, attemptId, sectionId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void structureAllowsAudioAndPassageButNeverNestedSolutions() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getStructure.execute(userId, attemptId)).thenReturn(new AttemptStructureResult(List.of(
                new AttemptStructureResult.Section(sectionId, UUID.randomUUID(), 1, """
                        {"title":"Listening","skill":"LISTENING","instructions":"Listen",
                         "audio":{"url":"https://cdn/a.mp3","durationSeconds":95,"transcript":"secret",
                                  "solution":{"transcript":"nested secret"}},
                         "solution":{"transcript":"secret"},"extra":{"solution":{"transcript":"secret"}}}
                        """, List.of()),
                new AttemptStructureResult.Section(UUID.randomUUID(), UUID.randomUUID(), 2,
                        "{\"title\":\"Reading\",\"passage\":\"City trees\"}", List.of()))));
        var response = mockMvc.perform(get("/api/assessments/attempts/{id}/structure", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].snapshot.audio.url").value("https://cdn/a.mp3"))
                .andExpect(jsonPath("$.sections[0].snapshot.audio.durationSeconds").value(95))
                .andExpect(jsonPath("$.sections[0].snapshot.skill").value("LISTENING"))
                .andExpect(jsonPath("$.sections[0].snapshot.passage").doesNotExist())
                .andExpect(jsonPath("$.sections[1].snapshot.passage").value("City trees"))
                .andExpect(jsonPath("$.sections[1].snapshot.skill").value("READING"))
                .andExpect(jsonPath("$.sections[1].snapshot.audio").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertNoSolutions(new ObjectMapper().readTree(response));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not JSON", "[]", "null", "{\"title\":", "\"legacy text\""})
    @WithMockUser(roles = "CUSTOMER")
    void malformedLegacySnapshotDoesNotBreakStructure(String snapshot) throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getStructure.execute(userId, attemptId)).thenReturn(new AttemptStructureResult(List.of(
                new AttemptStructureResult.Section(sectionId, UUID.randomUUID(), 1, snapshot, List.of()))));
        var response = mockMvc.perform(get("/api/assessments/attempts/{id}/structure", attemptId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var fields = new ObjectMapper().readTree(response).path("sections").get(0).path("snapshot");
        assertTrue(fields.isObject());
        assertTrue(fields.path("title").isNull());
        assertTrue(fields.path("skill").isNull());
        assertTrue(fields.path("instructions").isNull());
        assertNoSolutions(fields);
    }

    private static void assertNoSolutions(JsonNode node) throws Exception {
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                assertFalse(List.of("solution", "transcript").contains(field.getKey()), field.getKey());
                assertNoSolutions(field.getValue());
            }
        } else if (node.isArray()) {
            for (var child : node) assertNoSolutions(child);
        } else if (node.isTextual() && (node.asText().startsWith("{") || node.asText().startsWith("["))) {
            assertNoSolutions(new ObjectMapper().readTree(node.asText()));
        }
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotOpenResultVersion() throws Exception {
        mockMvc.perform(post("/api/assessments/attempts/{id}/result", attemptId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"overallBand\":6.0}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void startTrustsOnlyThePackageVersionAndIgnoresClientSuppliedStructure() throws Exception {
        UUID versionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        java.time.Instant now = java.time.Instant.now();
        when(startAttempt.execute(org.mockito.ArgumentMatchers.any())).thenReturn(new AssessmentAttemptResult(attemptId,
                userId, versionId, AttemptType.TOPIC_GATE, AttemptMode.STANDARD, AttemptChannel.WEB,
                AttemptStatus.IN_PROGRESS, now, null, null, 0, now, now));

        mockMvc.perform(post("/api/assessments/attempts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"packageVersionId":"%s","mode":"STANDARD","channel":"WEB","attemptType":"QUIZ",
                         "expiresAt":"2099-01-01T00:00:00Z","sections":[{"contentSectionId":"%s","items":[]}]}
                        """.formatted(versionId, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptType").value("TOPIC_GATE"));

        org.mockito.Mockito.verify(startAttempt).execute(new StartAssessmentAttemptCommand(userId, versionId,
                AttemptMode.STANDARD, AttemptChannel.WEB));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void lateSubmitIsAConflictWithAStableCode() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(submitAttempt.execute(new SubmitAssessmentAttemptCommand(userId, attemptId)))
                .thenThrow(new AttemptExpiredException());

        mockMvc.perform(post("/api/assessments/attempts/{id}/submit", attemptId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_EXPIRED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void failedResultShowsCorrectnessWithoutTheSolutionsKey() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getResult.execute(userId, attemptId)).thenReturn(new LearnerAssessmentResult(UUID.randomUUID(),
                attemptId, 1, "COMPLETED", java.time.Instant.now(), 1.0, 2.0, 50.0,
                List.of(new LearnerAssessmentResult.Item(itemId, UUID.randomUUID(), false)), null, null));

        mockMvc.perform(get("/api/assessments/attempts/{id}/result", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percent").value(50.0))
                .andExpect(jsonPath("$.items[0].correct").value(false))
                .andExpect(jsonPath("$.solutions").doesNotExist())
                .andExpect(jsonPath("$.sectionSolutions").doesNotExist())
                .andExpect(jsonPath("$.overallBand").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void passingResultHasSeparateSectionSolutionsAndUnchangedItemSolutions() throws Exception {
        UUID questionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getResult.execute(userId, attemptId)).thenReturn(new LearnerAssessmentResult(UUID.randomUUID(),
                attemptId, 1, "COMPLETED", java.time.Instant.now(), 7.0, 10.0, 70.0,
                List.of(new LearnerAssessmentResult.Item(itemId, questionId, true)),
                List.of(new LearnerAssessmentResult.Solution(itemId, questionId, "B", "Listen for the time")),
                List.of(new LearnerAssessmentResult.SectionSolution(sectionId, "The library closes at six."))));
        mockMvc.perform(get("/api/assessments/attempts/{id}/result", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solutions[0].attemptItemId").value(itemId.toString()))
                .andExpect(jsonPath("$.solutions[0].correctAnswer").value("B"))
                .andExpect(jsonPath("$.sectionSolutions[0].attemptSectionId").value(sectionId.toString()))
                .andExpect(jsonPath("$.sectionSolutions[0].transcript").value("The library closes at six."));
    }
}
