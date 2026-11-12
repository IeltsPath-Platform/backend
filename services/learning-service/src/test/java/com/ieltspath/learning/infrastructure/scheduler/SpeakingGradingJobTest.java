package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LlmUsageQuota;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingGrader;
import com.ieltspath.learning.application.service.SpeakingGradingContext;
import com.ieltspath.learning.application.service.SpeakingGradingContextCapture;
import com.ieltspath.learning.application.service.SpeakingHistorySummarizer;
import com.ieltspath.learning.application.service.SpeakingPromptAssembler;
import com.ieltspath.learning.application.service.SpeakingSettingsFixture;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpeechSegment;
import com.ieltspath.learning.domain.vo.SpokenWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeakingGradingJobTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID Q1 = UUID.randomUUID();
    private static final UUID Q2 = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-04T05:00:00Z");

    private final SpeakingSubmissionRepository submissions = mock(SpeakingSubmissionRepository.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final SpeechAnalysisClient speech = mock(SpeechAnalysisClient.class);
    private final SpeakingGrader grader = mock(SpeakingGrader.class);
    private final LlmUsageQuota quota = mock(LlmUsageQuota.class);
    private final SpeakingGradingContextCapture contexts = mock(SpeakingGradingContextCapture.class);
    private final SpeakingHistorySummarizer history = mock(SpeakingHistorySummarizer.class);
    private final ObjectMapper json = new ObjectMapper();
    private SpeakingGradingJob job;

    @BeforeEach
    void setUp() {
        job = new SpeakingGradingJob(submissions, storage, speech, grader, quota, SpeakingSettingsFixture.enabled(), json,
                contexts, history);
        when(grader.available()).thenReturn(true);
        when(speech.available()).thenReturn(true);
        when(storage.download(anyString(), anyString())).thenReturn(new byte[] {1});
        when(submissions.findGradingContext(any())).thenReturn(Optional.of("{}"));
        when(contexts.read(any())).thenReturn(SpeakingGradingContext.empty());
        when(history.summarize(any(), any(), any())).thenReturn(SpeakingHistorySummarizer.LearnerContext.empty());
    }

    /** A queued submission with both recordings stored, as the job claims it (one attempt used). */
    private SpeakingSubmission claimed() {
        SpeakingSubmission submission = SpeakingSubmission.start(UUID.randomUUID(), USER, UUID.randomUUID(), UUID.randomUUID(),
                SpeakingScope.PART, 5, NOW, List.of(SpeakingRecording.expected(Q1, UUID.randomUUID(), 1, 1, "Where do you live?"),
                        SpeakingRecording.expected(Q2, UUID.randomUUID(), 1, 2, "Do you work?")), NOW);
        submission.attachRecording(Q1, 1, "practice/a.wav", "audio/wav", 100, 3000, NOW);
        submission.attachRecording(Q2, 1, "practice/b.wav", "audio/wav", 100, 3000, NOW);
        LearningContentClient.Rubric rubric = new LearningContentClient.Rubric("IELTS_SPEAKING", "SPEAKING", 2, "note",
                List.of(), List.of());
        submission.saveRubricSnapshot(json.convertValue(rubric, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }));
        submission.complete(NOW);
        submission.startAnalysis(NOW);
        when(submissions.claimNext(any(), any(), anyInt())).thenReturn(Optional.of(submission));
        return submission;
    }

    private static SpeechAnalysis heard() {
        return new SpeechAnalysis(List.of(new SpeechSegment("I live in Hanoi", 80, 80, null, 100, 80, 0.95,
                List.of(new SpokenWord("I", 0, 200, 80, "None"), new SpokenWord("live", 300, 300, 80, "None")))));
    }

    private void gradingFailsWith(RuntimeException failure) {
        LearningContentClient.Rubric rubric = new LearningContentClient.Rubric("IELTS_SPEAKING", "SPEAKING", 2, "note",
                List.of(), List.of());
        when(grader.prepare(any(), any())).thenReturn(new SpeakingPromptAssembler.Prepared(rubric, "a".repeat(64), "user"));
        when(grader.grade(any(), any())).thenThrow(failure);
    }

    @Test
    void recordingsWithoutSpeechEndTheSubmissionBeforeAnyQuotaOrGradingIsSpent() {
        SpeakingSubmission submission = claimed();
        when(speech.analyze(any(), any())).thenReturn(new SpeechAnalysis(List.of()));

        job.runOne();

        assertEquals(SpeakingSubmissionStatus.ABANDONED, submission.status());
        assertEquals(SpeakingSubmission.NO_SPEECH, submission.failureCode());
        verify(submissions).save(submission);
        verify(quota, never()).tryConsume(any(), any(), anyString(), anyInt());
        verify(grader, never()).grade(any(), any());
    }

    @Test
    void aGradingThatProducesNothingGivesTheQuotaBackAndKeepsTheClaimCounted() {
        SpeakingSubmission submission = claimed();
        when(speech.analyze(any(), any())).thenReturn(heard());
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        job.runOne();

        assertEquals(SpeakingSubmissionStatus.FAILED_RETRYABLE, submission.status());
        assertEquals("LLM_UNAVAILABLE", submission.failureCode());
        assertEquals(1, submission.gradingAttempts());
        verify(quota).release(eq(USER), any(LocalDate.class), eq("speaking_grading"));
    }

    @Test
    void theDailyLimitDefersTheSubmissionWithoutUsingAnAttempt() {
        SpeakingSubmission submission = claimed();
        when(speech.analyze(any(), any())).thenReturn(heard());
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(false);
        when(grader.prepare(any(), any())).thenReturn(new SpeakingPromptAssembler.Prepared(
                new LearningContentClient.Rubric("IELTS_SPEAKING", "SPEAKING", 2, "note", List.of(), List.of()),
                "a".repeat(64), "user"));

        job.runOne();

        assertEquals(SpeakingSubmissionStatus.FAILED_RETRYABLE, submission.status());
        assertEquals("DAILY_LIMIT_REACHED", submission.failureCode());
        assertEquals(0, submission.gradingAttempts());
        // Nothing was taken from the day, so nothing is given back.
        verify(quota, never()).release(any(), any(), anyString());
        verify(grader, never()).grade(any(), any());
    }

    @Test
    void theJobSignalsProgressAfterEveryRecordingAndNeverCallsContent() {
        SpeakingSubmission submission = claimed();
        when(speech.analyze(any(), any())).thenReturn(heard());
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        job.runOne();

        verify(submissions, times(2)).touch(submission.id());
        // The error types and knowledge points come from the copy captured when the learner completed the submission.
        verify(submissions).findGradingContext(submission.id());
        verify(history).summarize(eq(USER), any(), eq(List.of()));
        assertFalse(submission.status() == SpeakingSubmissionStatus.GRADED);
    }
}
