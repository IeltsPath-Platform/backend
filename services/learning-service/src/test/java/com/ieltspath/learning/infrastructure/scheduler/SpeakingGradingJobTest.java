package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.exception.SpeechUnavailableException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LlmUsageQuota;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingGrader;
import com.ieltspath.learning.application.service.SpeakingGradingContext;
import com.ieltspath.learning.application.service.SpeakingGradingContextCapture;
import com.ieltspath.learning.application.service.SpeakingGradingInput;
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
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
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
    private final AccessClient access = mock(AccessClient.class);
    private final SpeakingGradingContextCapture contexts = mock(SpeakingGradingContextCapture.class);
    private final SpeakingHistorySummarizer history = mock(SpeakingHistorySummarizer.class);
    private final ObjectMapper json = new ObjectMapper();
    private SpeakingGradingJob job;

    @BeforeEach
    void setUp() {
        job = new SpeakingGradingJob(submissions, storage, speech, grader, quota, SpeakingSettingsFixture.enabled(), access, json,
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
    void theLastFailedAttemptEndsTheSubmissionAtOnce() {
        SpeakingSubmission submission = claimed();
        submission.startAnalysis(NOW);
        when(speech.analyze(any(), any())).thenReturn(heard());
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        job.runOne();

        assertEquals(SpeakingSubmissionStatus.NEEDS_EXAMINER, submission.status());
        assertEquals("MAX_ATTEMPTS_REACHED", submission.failureCode());
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
        verify(quota).tryConsume(eq(USER), any(LocalDate.class), eq("speaking_grading"), eq(5));
    }

    @Test
    void premiumDailyLimitIsTenAndLookupFailureUsesTheNormalLimit() {
        SpeakingSubmission premiumSubmission = claimed();
        when(speech.analyze(any(), any())).thenReturn(heard());
        when(grader.prepare(any(), any())).thenReturn(new SpeakingPromptAssembler.Prepared(
                new LearningContentClient.Rubric("IELTS_SPEAKING", "SPEAKING", 2, "note", List.of(), List.of()),
                "a".repeat(64), "user"));
        when(access.premiumOf(USER)).thenReturn(true);
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(false);

        job.runOne();

        verify(quota).tryConsume(eq(USER), any(LocalDate.class), eq("speaking_grading"), eq(10));

        SpeakingSubmission normalSubmission = claimed();
        when(access.premiumOf(USER)).thenThrow(new RuntimeException("unavailable"));
        job.runOne();
        verify(quota).tryConsume(eq(USER), any(LocalDate.class), eq("speaking_grading"), eq(5));
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

    // ---- recordings analysed at the same time -------------------------------------------------------------------------

    private static SpeechAnalysis saying(String text) {
        return new SpeechAnalysis(List.of(new SpeechSegment(text, 80, 80, null, 100, 80, 0.95,
                List.of(new SpokenWord("I", 0, 200, 80, "None")))));
    }

    @Test
    void theRecordingsOfOneSubmissionAreAnalysedAtTheSameTime() {
        claimed();
        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicBoolean together = new AtomicBoolean(true);
        // A one-at-a-time loop would wait here for a second analysis that never starts, and give up after the limit.
        when(speech.analyze(any(), any())).thenAnswer(invocation -> {
            bothStarted.countDown();
            if (!bothStarted.await(5, TimeUnit.SECONDS)) together.set(false);
            return heard();
        });
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        job.runOne();

        assertTrue(together.get(), "both recordings were not being analysed at the same time");
    }

    @Test
    void theAnalysesKeepTheOrderOfTheRecordingsEvenWhenTheLaterOneFinishesFirst() {
        claimed();
        when(storage.download(anyString(), eq("practice/a.wav"))).thenReturn(new byte[] {1});
        when(storage.download(anyString(), eq("practice/b.wav"))).thenReturn(new byte[] {2});
        when(speech.analyze(any(), any())).thenAnswer(invocation -> {
            byte[] audio = invocation.getArgument(0);
            if (audio[0] == 1) Thread.sleep(300);
            return saying(audio[0] == 1 ? "first answer" : "second answer");
        });
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));
        ArgumentCaptor<SpeakingGradingInput> input = ArgumentCaptor.forClass(SpeakingGradingInput.class);

        job.runOne();

        verify(grader).prepare(any(), input.capture());
        List<String> heardInOrder = input.getValue().answers().stream()
                .map(answer -> answer.analysis().segments().get(0).text()).toList();
        assertEquals(List.of("first answer", "second answer"), heardInOrder);
    }

    @Test
    void aRecordingThatFailsDoesNotLoseTheAnalysisOfTheOtherOne() {
        SpeakingSubmission submission = claimed();
        UUID first = submission.recordings().get(0).questionVersionId();
        UUID second = submission.recordings().get(1).questionVersionId();
        when(storage.download(anyString(), eq("practice/a.wav"))).thenReturn(new byte[] {1});
        when(storage.download(anyString(), eq("practice/b.wav"))).thenReturn(new byte[] {2});
        when(speech.analyze(any(), any())).thenAnswer(invocation -> {
            byte[] audio = invocation.getArgument(0);
            if (audio[0] == 1) throw new SpeechUnavailableException("SPEECH_FAILED", true);
            return heard();
        });

        job.runOne();

        assertEquals(SpeakingSubmissionStatus.FAILED_RETRYABLE, submission.status());
        assertEquals("SPEECH_UNAVAILABLE", submission.failureCode());
        // The finished one is stored, so the retry analyses only the one that failed.
        verify(submissions).saveAnalysis(eq(submission.id()), eq(second), anyString());
        verify(submissions, never()).saveAnalysis(eq(submission.id()), eq(first), anyString());
        verify(grader, never()).grade(any(), any());
    }

    // ---- workers ------------------------------------------------------------------------------------------------------

    private SpeakingGradingJob jobWith(int workers, boolean wakeOnSubmit) {
        return new SpeakingGradingJob(submissions, storage, speech, grader, quota,
                SpeakingSettingsFixture.withWorkers(true, 5L * 1024 * 1024, workers, 4, wakeOnSubmit), access, json, contexts, history);
    }

    /** A claimed submission with one recording, so what runs at the same time is whole submissions. */
    private SpeakingSubmission oneRecording(String key) {
        UUID question = UUID.randomUUID();
        SpeakingSubmission submission = SpeakingSubmission.start(UUID.randomUUID(), USER, UUID.randomUUID(), UUID.randomUUID(),
                SpeakingScope.PART, 5, NOW, new ArrayList<>(List.of(SpeakingRecording.expected(question, UUID.randomUUID(), 1, 1,
                        "Where do you live?"))), NOW);
        submission.attachRecording(question, 1, key, "audio/wav", 100, 3000, NOW);
        LearningContentClient.Rubric rubric = new LearningContentClient.Rubric("IELTS_SPEAKING", "SPEAKING", 2, "note",
                List.of(), List.of());
        submission.saveRubricSnapshot(json.convertValue(rubric, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }));
        submission.complete(NOW);
        submission.startAnalysis(NOW);
        return submission;
    }

    @Test
    void severalSubmissionsAreGradedAtOnceUpToTheNumberOfWorkers() {
        SpeakingGradingJob workers = jobWith(2, false);
        when(submissions.claimNext(any(), any(), anyInt())).thenReturn(Optional.of(oneRecording("practice/a.wav")),
                Optional.of(oneRecording("practice/b.wav")), Optional.empty());
        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicBoolean together = new AtomicBoolean(true);
        when(speech.analyze(any(), any())).thenAnswer(invocation -> {
            bothStarted.countDown();
            if (!bothStarted.await(5, TimeUnit.SECONDS)) together.set(false);
            return heard();
        });
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        workers.drain();

        verify(quota, timeout(5000).times(2)).release(any(), any(), eq("speaking_grading"));
        assertTrue(together.get(), "the two submissions were not graded at the same time");
    }

    @Test
    void aWorkerThatFinishesTakesTheNextSubmissionAtOnceAndNeverRunsMoreThanTheSettingAllows() {
        SpeakingGradingJob oneWorker = jobWith(1, false);
        when(submissions.claimNext(any(), any(), anyInt())).thenReturn(Optional.of(oneRecording("practice/a.wav")),
                Optional.of(oneRecording("practice/b.wav")), Optional.empty());
        AtomicInteger running = new AtomicInteger();
        AtomicInteger mostAtOnce = new AtomicInteger();
        when(speech.analyze(any(), any())).thenAnswer(invocation -> {
            mostAtOnce.accumulateAndGet(running.incrementAndGet(), Math::max);
            Thread.sleep(50);
            running.decrementAndGet();
            return heard();
        });
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        oneWorker.drain();

        // Both are done after one call to drain: the second was not left for the next poll.
        verify(quota, timeout(5000).times(2)).release(any(), any(), eq("speaking_grading"));
        assertEquals(1, mostAtOnce.get());
    }

    @Test
    void aWakeUpStartsAWorkerWithoutWaitingForThePoll() {
        SpeakingGradingJob woken = jobWith(1, true);
        when(submissions.claimNext(any(), any(), anyInt())).thenReturn(Optional.of(oneRecording("practice/a.wav")),
                Optional.empty());
        when(speech.analyze(any(), any())).thenReturn(heard());
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        woken.wake();

        verify(quota, timeout(5000)).release(any(), any(), eq("speaking_grading"));
    }

    @Test
    void aWakeUpDoesNothingWhenItIsSwitchedOff() {
        jobWith(1, false).wake();

        verify(submissions, after(300).never()).claimNext(any(), any(), anyInt());
    }

    // ---- stopping the instance ----------------------------------------------------------------------------------------

    @Test
    void aSubmissionStillBeingGradedWhenTheInstanceStopsIsHandedBackAndTheAttemptIsNotCounted() throws Exception {
        SpeakingGradingJob stopping = jobWith(1, false);
        stopping.shutdownWaitSeconds(1);
        SpeakingSubmission running = oneRecording("practice/a.wav");
        int attemptsBefore = running.gradingAttempts();
        when(submissions.claimNext(any(), any(), anyInt())).thenReturn(Optional.of(running), Optional.empty());
        when(submissions.findById(running.id())).thenReturn(Optional.of(running));
        CountDownLatch analysing = new CountDownLatch(1);
        CountDownLatch never = new CountDownLatch(1);
        when(speech.analyze(any(), any())).thenAnswer(invocation -> {
            analysing.countDown();
            never.await(10, TimeUnit.SECONDS); // longer than the wait of the shutdown
            return heard();
        });

        stopping.drain();
        assertTrue(analysing.await(5, TimeUnit.SECONDS), "the submission did not start");
        stopping.shutdown();
        never.countDown();

        ArgumentCaptor<SpeakingSubmission> saved = ArgumentCaptor.forClass(SpeakingSubmission.class);
        verify(submissions, timeout(2000).atLeastOnce()).save(saved.capture());
        SpeakingSubmission handedBack = saved.getAllValues().get(0);
        assertEquals(SpeakingSubmissionStatus.FAILED_RETRYABLE, handedBack.status());
        assertEquals("INSTANCE_STOPPED", handedBack.failureCode());
        assertEquals(attemptsBefore - 1, handedBack.gradingAttempts(), "a hand-back is not an attempt of the learner's submission");
    }

    @Test
    void aSubmissionThatFinishesWithinTheWaitIsNotHandedBack() throws Exception {
        SpeakingGradingJob stopping = jobWith(1, false);
        SpeakingSubmission running = oneRecording("practice/a.wav");
        when(submissions.claimNext(any(), any(), anyInt())).thenReturn(Optional.of(running), Optional.empty());
        when(submissions.findById(running.id())).thenReturn(Optional.of(running));
        CountDownLatch analysing = new CountDownLatch(1);
        when(speech.analyze(any(), any())).thenAnswer(invocation -> {
            analysing.countDown();
            Thread.sleep(200);
            return heard();
        });
        when(quota.tryConsume(any(), any(), anyString(), anyInt())).thenReturn(true);
        gradingFailsWith(new LlmUnavailableException(null));

        stopping.drain();
        assertTrue(analysing.await(5, TimeUnit.SECONDS), "the submission did not start");
        stopping.shutdown();

        // It ended as its own outcome says (the model could not be reached), not as a hand-back.
        ArgumentCaptor<SpeakingSubmission> saved = ArgumentCaptor.forClass(SpeakingSubmission.class);
        verify(submissions, timeout(2000).atLeastOnce()).save(saved.capture());
        assertTrue(saved.getAllValues().stream().noneMatch(row -> "INSTANCE_STOPPED".equals(row.failureCode())));
    }

    @Test
    void aStoppedInstanceClaimsNothingNew() {
        SpeakingGradingJob stopped = jobWith(1, true);
        stopped.shutdown();

        stopped.wake();
        stopped.drain();

        verify(submissions, after(300).never()).claimNext(any(), any(), anyInt());
    }

    @Test
    void aRunningSubmissionIsKeptFreshThreeTimesWithinTheStaleLimit() {
        assertEquals(40, SpeakingGradingJob.heartbeatSeconds(120));
        assertEquals(10, SpeakingGradingJob.heartbeatSeconds(30));
        assertEquals(5, SpeakingGradingJob.heartbeatSeconds(9));
    }
}
