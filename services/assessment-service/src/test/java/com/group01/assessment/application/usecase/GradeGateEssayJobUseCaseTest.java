package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.application.exception.EssayGradingException;
import com.group01.assessment.application.port.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradeGateEssayJobUseCaseTest {
    private final GateEssayJobStore jobs = mock(GateEssayJobStore.class);
    private final EssayGradingPort grader = mock(EssayGradingPort.class);
    private final AssessmentLlmQuota quota = mock(AssessmentLlmQuota.class);
    private final GateResultAssembler assembler = mock(GateResultAssembler.class);
    private final RecordingTransactionManager tx = new RecordingTransactionManager();
    private final UUID userId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID submissionId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-07T01:00:00Z");

    @AfterEach
    void clearsTransactionState() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void successfulAiGradeRunsOutsideTransactionThenCompletesResult() {
        stubClaim();
        when(grader.available()).thenReturn(true);
        when(quota.tryConsume(eq(userId), any(LocalDate.class), eq(4))).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                    "quota access must not run in a database transaction");
            return true;
        });
        when(grader.grade(any(), eq("submitted essay"))).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                    "the remote LLM call must not run in a database transaction");
            return new BigDecimal("6.0");
        });
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                    "result assembly must share the short transaction that completes the job");
            return true;
        }).when(assembler).completeIfGraded(attemptId);

        assertEquals(1, useCase().gradeBatch());

        verify(jobs).complete(jobId, new BigDecimal("6.0"), now);
        verify(assembler).completeIfGraded(attemptId);
        verify(jobs).claim(5, now);
        verify(jobs).requeueStuck(now.minus(Duration.ofMinutes(10)));
    }

    @Test
    void unavailableLlmCreatesHumanFallbackWithoutCallingGrade() {
        stubClaim();
        when(grader.available()).thenReturn(false);

        assertEquals(1, useCase().gradeBatch());

        verify(jobs).fail(jobId, now);
        verify(jobs).enqueueHuman(submissionId, userId, "gate-essay:" + itemId + ":human");
        verify(grader, never()).grade(any(), any());
        verifyNoInteractions(assembler);
    }

    @Test
    void llmExceptionCreatesHumanFallback() {
        stubClaim();
        when(grader.available()).thenReturn(true);
        when(quota.tryConsume(eq(userId), any(LocalDate.class), eq(4))).thenReturn(true);
        when(grader.grade(any(), eq("submitted essay"))).thenThrow(new EssayGradingException("INVALID_REPLY"));

        useCase().gradeBatch();

        verify(jobs).fail(jobId, now);
        verify(jobs).enqueueHuman(submissionId, userId, "gate-essay:" + itemId + ":human");
        verify(assembler, never()).completeIfGraded(any());
    }

    @Test
    void exhaustedDailyQuotaCreatesHumanFallbackWithoutCallingLlm() {
        stubClaim();
        when(grader.available()).thenReturn(true);
        when(quota.tryConsume(eq(userId), any(LocalDate.class), eq(4))).thenReturn(false);

        useCase().gradeBatch();

        verify(jobs).fail(jobId, now);
        verify(jobs).enqueueHuman(submissionId, userId, "gate-essay:" + itemId + ":human");
        verify(grader, never()).grade(any(), any());
    }

    @Test
    void processingJobsOlderThanTheRecoveryWindowAreRequeued() {
        when(jobs.claim(5, now)).thenReturn(List.of());

        assertEquals(0, useCase().gradeBatch());

        verify(jobs).requeueStuck(now.minus(Duration.ofMinutes(10)));
    }

    private void stubClaim() {
        when(jobs.claim(5, now)).thenReturn(List.of(new GateEssayJobStore.ClaimedJob(jobId, userId, attemptId,
                itemId, submissionId, "submitted essay",
                "{\"stem\":\"Write about a topic\"}",
                "{\"answerSpec\":{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"minWords\":250},\"maxScore\":1}")));
    }

    private GradeGateEssayJobUseCase useCase() {
        return new GradeGateEssayJobUseCase(jobs, grader, quota, assembler, tx, new ObjectMapper(),
                5, 4, Duration.ofMinutes(10), ZoneId.of("UTC"), java.time.Clock.fixed(now, ZoneId.of("UTC")));
    }

    /** A real Spring transaction boundary so the assertions detect accidentally moving quota/LLM work inside it. */
    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
