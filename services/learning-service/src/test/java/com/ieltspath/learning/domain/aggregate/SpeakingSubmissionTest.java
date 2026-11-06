package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeakingSubmissionTest {
    private static final Instant NOW = Instant.parse("2026-11-04T05:00:00Z");
    private static final UUID SECTION_1 = UUID.randomUUID();
    private static final UUID SECTION_2 = UUID.randomUUID();
    private static final UUID Q1 = UUID.randomUUID();
    private static final UUID Q2 = UUID.randomUUID();
    private static final UUID Q3 = UUID.randomUUID();

    private static SpeakingSubmission submission() {
        // Given out of order on purpose: the submission lists them by part, then by question.
        return SpeakingSubmission.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SpeakingScope.FULL, 5, NOW, List.of(
                        SpeakingRecording.expected(Q3, SECTION_2, 2, 1),
                        SpeakingRecording.expected(Q2, SECTION_1, 1, 2),
                        SpeakingRecording.expected(Q1, SECTION_1, 1, 1)), NOW);
    }

    private static void store(SpeakingSubmission submission, UUID question, int attempt, Instant at) {
        submission.attachRecording(question, attempt, "practice/key-" + question + "-" + attempt, "audio/wav", 1000, 4000, at);
    }

    @Test
    void startsUploadingWithAnEmptyRecordingPerQuestionInOrder() {
        SpeakingSubmission submission = submission();

        assertEquals(SpeakingSubmissionStatus.UPLOADING, submission.status());
        assertTrue(submission.isNew());
        assertEquals(List.of(Q1, Q2, Q3), submission.recordings().stream().map(SpeakingRecording::questionVersionId).toList());
        assertFalse(submission.hasAllRecordings());
        assertEquals(3, submission.missingRecordings());
    }

    @Test
    void refusesToStartWithoutQuestionsOrCost() {
        assertThrows(IllegalArgumentException.class, () -> SpeakingSubmission.start(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), SpeakingScope.FULL, 5, NOW, List.of(), NOW));
        assertThrows(IllegalArgumentException.class, () -> SpeakingSubmission.start(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), SpeakingScope.FULL, 0, NOW,
                List.of(SpeakingRecording.expected(Q1, SECTION_1, 1, 1)), NOW));
    }

    @Test
    void completesOnlyWhenEveryQuestionHasItsFile() {
        SpeakingSubmission submission = submission();
        store(submission, Q1, 1, NOW);
        store(submission, Q2, 1, NOW);

        assertThrows(IllegalStateException.class, () -> submission.complete(NOW));

        store(submission, Q3, 1, NOW);
        submission.complete(NOW.plusSeconds(5));

        assertEquals(SpeakingSubmissionStatus.QUEUED, submission.status());
        assertEquals(NOW.plusSeconds(5), submission.queuedAt());
        assertFalse(submission.isAcceptingRecordings());
    }

    @Test
    void recordingAgainReturnsTheKeyItReplaces() {
        SpeakingSubmission submission = submission();
        assertEquals(Optional.empty(), submission.attachRecording(Q1, 1, "practice/a", "audio/wav", 10, null, NOW));

        Optional<String> replaced = submission.attachRecording(Q1, 2, "practice/b", "audio/wav", 20, 3000, NOW);

        assertEquals(Optional.of("practice/a"), replaced);
        SpeakingRecording recording = submission.recordingFor(Q1).orElseThrow();
        assertEquals("practice/b", recording.objectKey());
        assertEquals(2, recording.attempt());
        assertEquals(3000, recording.durationMs());
    }

    @Test
    void refusesRecordingsOfOtherQuestionsOrAfterQueued() {
        SpeakingSubmission submission = submission();
        assertThrows(IllegalArgumentException.class,
                () -> submission.attachRecording(UUID.randomUUID(), 1, "practice/x", "audio/wav", 10, null, NOW));

        store(submission, Q1, 1, NOW);
        store(submission, Q2, 1, NOW);
        store(submission, Q3, 1, NOW);
        submission.complete(NOW);

        assertThrows(IllegalStateException.class, () -> store(submission, Q1, 2, NOW));
    }

    @Test
    void anUploadNobodyFinishedGoesStaleAndCanBeAbandoned() {
        SpeakingSubmission submission = submission();
        store(submission, Q1, 1, NOW.plusSeconds(10));

        assertFalse(submission.isStaleUpload(NOW));
        assertTrue(submission.isStaleUpload(NOW.plusSeconds(11)));

        submission.abandon(NOW.plusSeconds(3600));

        assertEquals(SpeakingSubmissionStatus.ABANDONED, submission.status());
        assertEquals("UPLOAD_ABANDONED", submission.failureCode());
        assertFalse(submission.status().isActive());
    }

    @Test
    void onlyUploadingSubmissionsCanBeAbandonedOrCompleted() {
        SpeakingSubmission submission = submission();
        submission.abandon(NOW);

        assertThrows(IllegalStateException.class, () -> submission.abandon(NOW));
        assertThrows(IllegalStateException.class, () -> submission.complete(NOW));
    }

    @Test
    void aSubmissionWithoutSpeechEndsAndFreesTheLearner() {
        SpeakingSubmission submission = submission();
        submission.startAnalysis(NOW);

        submission.noSpeech(NOW.plusSeconds(5));

        assertEquals(SpeakingSubmissionStatus.ABANDONED, submission.status());
        assertEquals(SpeakingSubmission.NO_SPEECH, submission.failureCode());
        assertFalse(submission.status().isActive());
    }

    @Test
    void aDeferredSubmissionWaitsAgainWithoutLosingAnAttempt() {
        SpeakingSubmission submission = submission();
        submission.startAnalysis(NOW);
        submission.startAnalysis(NOW.plusSeconds(200));
        assertEquals(2, submission.gradingAttempts());

        submission.deferred("DAILY_LIMIT_REACHED", NOW.plusSeconds(210));

        assertEquals(SpeakingSubmissionStatus.FAILED_RETRYABLE, submission.status());
        assertEquals("DAILY_LIMIT_REACHED", submission.failureCode());
        assertEquals(1, submission.gradingAttempts());
        // The count never goes below zero.
        SpeakingSubmission fresh = submission();
        fresh.deferred("DAILY_LIMIT_REACHED", NOW);
        assertEquals(0, fresh.gradingAttempts());
    }

    @Test
    void waitingStatusesAreTheActiveOnes() {
        for (SpeakingSubmissionStatus status : SpeakingSubmissionStatus.values()) {
            boolean active = status == SpeakingSubmissionStatus.UPLOADING || status == SpeakingSubmissionStatus.QUEUED
                    || status == SpeakingSubmissionStatus.ANALYZING || status == SpeakingSubmissionStatus.GRADING
                    || status == SpeakingSubmissionStatus.PAYMENT_PENDING
                    // A grading that failed for a passing reason is retried, so it still occupies the learner's one slot.
                    || status == SpeakingSubmissionStatus.FAILED_RETRYABLE;
            assertEquals(active, status.isActive(), status.name());
        }
    }
}
