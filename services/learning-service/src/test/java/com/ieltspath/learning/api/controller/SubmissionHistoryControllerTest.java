package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.GetPracticeSinceEssayUseCase;
import com.ieltspath.learning.application.usecase.ListSubmissionHistoryUseCase;
import com.ieltspath.learning.application.usecase.ListSubmissionHistoryUseCase.Scope;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository.Entry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubmissionHistoryController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class SubmissionHistoryControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean ListSubmissionHistoryUseCase history;
    @MockitoBean GetPracticeSinceEssayUseCase practiceSinceEssay;
    final UUID learner = UUID.randomUUID();

    @Test
    void aPractisedKnowledgePointLineCarriesItsPointAndAttempt() throws Exception {
        UUID attempt = UUID.randomUUID();
        UUID point = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(learner);
        when(history.execute(learner, Scope.KNOWLEDGE, null, 0, 20)).thenReturn(new ListSubmissionHistoryUseCase.Page(List.of(
                new Entry("KP_PRACTICE", attempt, Instant.parse("2026-11-14T10:00:00Z"), "READING", null, null, null, null, 3, 4,
                        true, null, null, null, null, attempt, null, null, point, "TREND", "Từ vựng xu hướng")), 0, 20, false));

        mvc.perform(get("/api/learning/submission-history").param("scope", "KNOWLEDGE")
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].kind").value("KP_PRACTICE"))
                .andExpect(jsonPath("$.items[0].attemptId").value(attempt.toString()))
                .andExpect(jsonPath("$.items[0].knowledgePointId").value(point.toString()))
                .andExpect(jsonPath("$.items[0].knowledgePointName").value("Từ vựng xu hướng"))
                .andExpect(jsonPath("$.items[0].correctCount").value(3));
    }

    @Test
    void tellsWhatWasPractisedSinceAnEssay() throws Exception {
        UUID essay = UUID.randomUUID();
        UUID point = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(learner);
        when(practiceSinceEssay.execute(learner, essay, List.of(point, other))).thenReturn(List.of(
                new SubmissionHistoryRepository.PracticeTally(point, 2, attempt, Instant.parse("2026-11-15T08:00:00Z"), 7, 10, true)));

        mvc.perform(get("/api/learning/submission-history/practice-since-essay").param("submissionId", essay.toString())
                        .param("knowledgePointIds", point.toString(), other.toString()).with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].knowledgePointId").value(point.toString()))
                .andExpect(jsonPath("$[0].attempts").value(2))
                .andExpect(jsonPath("$[0].lastAttemptId").value(attempt.toString()))
                .andExpect(jsonPath("$[0].lastCorrectCount").value(7))
                .andExpect(jsonPath("$[0].lastPassed").value(true));
    }

    @Test
    void listsTheLearnersSubmissionsAndShortensTheQuestion() throws Exception {
        UUID essay = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(learner);
        when(history.execute(learner, Scope.COURSE, null, 0, 20)).thenReturn(new ListSubmissionHistoryUseCase.Page(List.of(
                new Entry("WRITING", essay, Instant.parse("2026-11-14T10:00:00Z"), "WRITING", "TASK_2", "x".repeat(300),
                        new BigDecimal("6.5"), null, null, null, true, "GRADED", UUID.randomUUID(), null, null, null, null, 2)),
                0, 20, true));

        mvc.perform(get("/api/learning/submission-history").param("scope", "COURSE")
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items[0].id").value(essay.toString()))
                .andExpect(jsonPath("$.items[0].kind").value("WRITING"))
                .andExpect(jsonPath("$.items[0].band").value(6.5))
                .andExpect(jsonPath("$.items[0].promptPreview").value("x".repeat(160) + "…"))
                .andExpect(jsonPath("$.items[0].revisionNo").value(2));

        verify(history).execute(learner, Scope.COURSE, null, 0, 20);
    }

    @Test
    void freePracticePassesTheSkillAndPage() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(history.execute(learner, Scope.FREE, "SPEAKING", 2, 5))
                .thenReturn(new ListSubmissionHistoryUseCase.Page(List.of(), 2, 5, false));

        mvc.perform(get("/api/learning/submission-history").param("scope", "FREE").param("skill", "SPEAKING")
                        .param("page", "2").param("size", "5").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    void anUnknownScopeIsRejectedAsInvalid() throws Exception {
        mvc.perform(get("/api/learning/submission-history").param("scope", "ALL").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anotherRoleCannotRead() throws Exception {
        mvc.perform(get("/api/learning/submission-history").param("scope", "COURSE").with(user("staff").roles("EXAMINER")))
                .andExpect(status().isForbidden());
    }
}
