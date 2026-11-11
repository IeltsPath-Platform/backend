package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.exception.WritingGradingException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LlmUsageQuota;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EssaySubmissionFlowTest {
    private final WritingSubmissionRepository essays = mock(WritingSubmissionRepository.class);
    private final LlmUsageQuota quota = mock(LlmUsageQuota.class);
    private final EssayGrader grader = mock(EssayGrader.class);
    private final AccessClient points = mock(AccessClient.class);
    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    private final EssaySubmissionFlow flow = new EssaySubmissionFlow(essays, quota, grader, points,
            WritingSettings.defaults(), metrics);

    @BeforeEach
    void setUp() {
        when(quota.tryConsume(any(), any(), eq(EssaySubmissionFlow.USAGE_KIND), eq(10))).thenReturn(true);
        when(essays.save(any())).thenReturn(true);
    }

    @Test
    void systemFailuresReturnTheConsumedQuota() {
        for (String code : List.of("LLM_UNAVAILABLE", "RUBRIC_UNAVAILABLE", "PROMPT_VERSION_UNAVAILABLE")) {
            reset(grader);
            reset(quota);
            when(quota.tryConsume(any(), any(), eq(EssaySubmissionFlow.USAGE_KIND), eq(10))).thenReturn(true);
            doThrow(new WritingGradingException(code)).when(grader).grade(any(), anyString());

            assertThrows(LearningRequestException.class, () -> flow.grade(submission()));

            verify(quota).release(any(), any(LocalDate.class), eq(EssaySubmissionFlow.USAGE_KIND));
            assertEquals(1.0, metrics.counter("learning.writing.grading", "outcome", code, "source", "free").count());
        }
    }

    @Test
    void invalidGradeConsumesTheQuota() {
        doThrow(new WritingGradingException("INVALID_GRADE")).when(grader).grade(any(), anyString());

        assertThrows(LearningRequestException.class, () -> flow.grade(submission()));

        verify(quota, org.mockito.Mockito.never()).release(any(), any(), anyString());
        assertEquals(1.0, metrics.counter("learning.writing.grading", "outcome", "INVALID_GRADE", "source", "free").count());
    }

    @Test
    void bulkheadFailureReturnsQuotaAndKeepsTheAiBusyResponse() {
        LearningRequestException busy = new LearningRequestException(503, "AI_BUSY", "busy");
        doThrow(busy).when(grader).grade(any(), anyString());

        assertEquals(busy, assertThrows(LearningRequestException.class, () -> flow.grade(submission())));

        verify(quota).release(any(), any(LocalDate.class), eq(EssaySubmissionFlow.USAGE_KIND));
        assertEquals(1.0, metrics.counter("learning.writing.grading", "outcome", "AI_BUSY", "source", "free").count());
    }

    @Test
    void successfulGradeRecordsOutcomeAndCorrectionCount() {
        WritingGrade grade = new WritingGrade(List.of(), List.of(
                new WritingGrade.Correction("wrong", "right", "GRAMMAR")), "summary", new BigDecimal("6.0"));
        when(grader.grade(any(), anyString())).thenReturn(new EssayGrader.GradedEssay(grade, "RUBRIC@1+tplv1"));

        flow.grade(submission());

        assertEquals(1.0, metrics.counter("learning.writing.grading", "outcome", "GRADED", "source", "free").count());
        assertEquals(1, metrics.summary("learning.writing.corrections").count());
        assertEquals(1.0, metrics.summary("learning.writing.corrections").totalAmount());
    }

    private static WritingSubmission submission() {
        EssayPrompt prompt = new EssayPrompt(UUID.randomUUID(), "Write an essay.", "TASK_2", 250,
                new BigDecimal("5.5"), null, null, List.of(), List.of());
        return WritingSubmission.startFree(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "An essay", 250, prompt, 3, java.time.Instant.now());
    }
}
