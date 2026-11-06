package com.ieltspath.learning.application.usecase;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingAvailability;
import com.ieltspath.learning.application.service.SpeakingGrader;
import com.ieltspath.learning.application.service.SpeakingGradingContextCapture;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingSettingsFixture;
import com.ieltspath.learning.application.service.SpeakingTestQuestions;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeakingPracticeUseCasesTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID PACKAGE_VERSION = UUID.randomUUID();
    private static final UUID PART_1 = UUID.randomUUID();
    private static final UUID PART_2 = UUID.randomUUID();
    private static final UUID Q1 = UUID.randomUUID();
    private static final UUID Q2 = UUID.randomUUID();
    private static final UUID Q3 = UUID.randomUUID();
    private static final SpeakingPracticeSettings SETTINGS =
            SpeakingSettingsFixture.enabled();

    private final InMemorySubmissions submissions = new InMemorySubmissions();
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final AccessClient points = mock(AccessClient.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final SpeechAnalysisClient speech = mock(SpeechAnalysisClient.class);
    private final SpeakingGrader grader = mock(SpeakingGrader.class);

    private CreateSpeakingSubmissionUseCase create;
    private UploadSpeakingRecordingUseCase upload;
    private CompleteSpeakingSubmissionUseCase complete;

    @BeforeEach
    void setUp() {
        when(points.balance()).thenReturn(50L);
        when(speech.available()).thenReturn(true);
        when(grader.available()).thenReturn(true);
        when(content.getPackageVersion(PACKAGE_VERSION)).thenReturn(new PackageVersion(PACKAGE_VERSION, UUID.randomUUID(),
                "PRACTICE_TEST", null, Map.of(), List.of(
                section(PART_2, 2, item(Q3, 1)),
                section(PART_1, 1, item(Q1, 1), item(Q2, 2)))));
        when(storage.put(anyString(), anyString(), any(), anyString())).thenAnswer(call ->
                new StoredObject(call.getArgument(0), call.getArgument(1), call.getArgument(3),
                        ((byte[]) call.getArgument(2)).length));
        create = new CreateSpeakingSubmissionUseCase(submissions, new SpeakingTestQuestions(content), points, userId -> { },
                SETTINGS, new SpeakingAvailability(SETTINGS, speech, grader), transactions);
        upload = new UploadSpeakingRecordingUseCase(submissions, storage, userId -> { }, SETTINGS, transactions);
        complete = new CompleteSpeakingSubmissionUseCase(submissions, userId -> { }, transactions);
    }

    private static Section section(UUID id, int order, Item... items) {
        return new Section(id, "Part " + order, "SPEAKING", null, order, null, List.of(items));
    }

    private static Item item(UUID id, int order) {
        return new Item(id, order, "Question " + order, List.of(), Map.of(), null, BigDecimal.ONE, List.of());
    }

    private static byte[] wav() {
        return wav(16000, 1);
    }

    /** A 16-bit PCM WAV with a canonical header and one second of silence. */
    private static byte[] wav(int sampleRate, int channels) {
        int data = sampleRate * channels * 2;
        var buffer = java.nio.ByteBuffer.allocate(44 + data).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes()).putInt(36 + data).put("WAVE".getBytes());
        buffer.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) channels).putInt(sampleRate)
                .putInt(sampleRate * channels * 2).putShort((short) (channels * 2)).putShort((short) 16);
        buffer.put("data".getBytes()).putInt(data);
        return buffer.array();
    }

    private SpeakingSubmissionResult open(UUID requestId) {
        return create.execute(USER, requestId, PACKAGE_VERSION, null, true);
    }

    private void record(UUID submissionId, UUID question) {
        upload.execute(USER, submissionId, question, "audio/wav", wav(), 4000);
    }

    @Test
    void opensASubmissionForTheWholeTestWithOneEmptyRecordingPerQuestion() {
        SpeakingSubmissionResult result = open(UUID.randomUUID());

        assertEquals(SpeakingSubmissionStatus.UPLOADING, result.status());
        assertEquals(SpeakingScope.FULL, result.scope());
        assertEquals(5, result.pointCost());
        // Part numbers follow the order of the sections, not the order Content lists them in.
        assertEquals(List.of(Q1, Q2, Q3), result.recordings().stream().map(SpeakingSubmissionResult.Recording::questionVersionId).toList());
        assertEquals(List.of(1, 1, 2), result.recordings().stream().map(SpeakingSubmissionResult.Recording::partNo).toList());
        assertTrue(result.recordings().stream().noneMatch(SpeakingSubmissionResult.Recording::uploaded));
    }

    @Test
    void asksOnlyForTheChosenParts() {
        SpeakingSubmissionResult result = create.execute(USER, UUID.randomUUID(), PACKAGE_VERSION, List.of(PART_2), true);

        assertEquals(SpeakingScope.PART, result.scope());
        assertEquals(List.of(Q3), result.recordings().stream().map(SpeakingSubmissionResult.Recording::questionVersionId).toList());
        // The part keeps its place in the test, so the folder says part2 even when it is the only one.
        assertEquals(2, result.recordings().get(0).partNo());
    }

    @Test
    void refusesAPartThatIsNotInTheTest() {
        LearningRequestException refused = assertThrows(LearningRequestException.class,
                () -> create.execute(USER, UUID.randomUUID(), PACKAGE_VERSION, List.of(UUID.randomUUID()), true));

        assertEquals("SPEAKING_PART_NOT_FOUND", refused.getCode());
    }

    @Test
    void needsConsent() {
        LearningRequestException refused = assertThrows(LearningRequestException.class,
                () -> create.execute(USER, UUID.randomUUID(), PACKAGE_VERSION, null, false));

        assertEquals("CONSENT_REQUIRED", refused.getCode());
        assertTrue(submissions.all().isEmpty());
    }

    @Test
    void notEnoughPointsStopsBeforeAnythingIsStoredOrCharged() {
        when(points.balance()).thenReturn(4L);

        LearningRequestException refused = assertThrows(LearningRequestException.class, () -> open(UUID.randomUUID()));

        assertEquals(402, refused.getStatus());
        assertEquals("INSUFFICIENT_POINTS", refused.getCode());
        assertTrue(submissions.all().isEmpty());
        verify(storage, never()).put(anyString(), anyString(), any(), anyString());
        verify(points, never()).debit(any(), org.mockito.ArgumentMatchers.anyInt(), any(UUID.class), anyString(), anyString());
    }

    @Test
    void exactlyTheCostIsEnough() {
        when(points.balance()).thenReturn(5L);

        assertEquals(SpeakingSubmissionStatus.UPLOADING, open(UUID.randomUUID()).status());
    }

    @Test
    void anUnreachablePointsServiceIsReportedNotTreatedAsEnough() {
        when(points.balance()).thenThrow(new AccessUnavailableException(503));

        LearningRequestException refused = assertThrows(LearningRequestException.class, () -> open(UUID.randomUUID()));

        assertEquals(503, refused.getStatus());
        assertTrue(submissions.all().isEmpty());
    }

    @Test
    void sendingTheSameRequestAgainReturnsTheSameSubmission() {
        UUID requestId = UUID.randomUUID();

        SpeakingSubmissionResult first = open(requestId);
        SpeakingSubmissionResult again = open(requestId);

        assertEquals(first.id(), again.id());
        assertEquals(1, submissions.all().size());
    }

    @Test
    void aRequestIdUsedForAnotherPackageIsAConflict() {
        UUID requestId = UUID.randomUUID();
        open(requestId);

        LearningRequestException refused = assertThrows(LearningRequestException.class,
                () -> create.execute(USER, requestId, UUID.randomUUID(), null, true));

        assertEquals("REQUEST_CONFLICT", refused.getCode());
    }

    @Test
    void aLearnerHasOneWaitingSubmissionAtATime() {
        SpeakingSubmissionResult first = open(UUID.randomUUID());

        LearningRequestException refused = assertThrows(LearningRequestException.class, () -> open(UUID.randomUUID()));

        assertEquals(409, refused.getStatus());
        assertEquals("SPEAKING_SUBMISSION_IN_PROGRESS", refused.getCode());
        assertEquals(first.id(), refused.getSubmissionId());
    }

    @Test
    void anUploadLeftUnfinishedIsGivenUpSoTheLearnerCanStartAgain() {
        SpeakingSubmissionResult stale = open(UUID.randomUUID());
        submissions.age(stale.id(), 3600);

        SpeakingSubmissionResult fresh = open(UUID.randomUUID());

        assertEquals(SpeakingSubmissionStatus.ABANDONED, submissions.get(stale.id()).status());
        assertEquals(SpeakingSubmissionStatus.UPLOADING, fresh.status());
    }

    @Test
    void anUnsentRecordingOfTheSameAttemptIsReplacedAtOnceButAnotherAttemptIsStillRefused() {
        UUID version = UUID.randomUUID(), lesson = UUID.randomUUID(), attempt = UUID.randomUUID();
        when(content.getPackageVersion(version)).thenReturn(new PackageVersion(version, UUID.randomUUID(), "PRACTICE_SET",
                null, Map.of(), List.of(section(PART_1, 1, new Item(Q1, 1, "Describe your weekend.", List.of(),
                Map.of("type", "SPEAKING"), null, BigDecimal.ONE, List.of())))));
        SpeakingSubmissionResult unsent = create.executeForAttempt(USER, UUID.randomUUID(), version, lesson, attempt, true);

        // The page was opened again before any file was sent: a new request for the attempt takes its place.
        SpeakingSubmissionResult again = create.executeForAttempt(USER, UUID.randomUUID(), version, lesson, attempt, true);

        assertEquals(SpeakingSubmissionStatus.ABANDONED, submissions.get(unsent.id()).status());
        assertEquals(SpeakingSubmissionStatus.UPLOADING, again.status());
        assertEquals(attempt, submissions.get(again.id()).practiceAttemptId());
        // Another attempt, or a free practice, still waits for the one that is open.
        LearningRequestException refused = assertThrows(LearningRequestException.class,
                () -> create.executeForAttempt(USER, UUID.randomUUID(), version, lesson, UUID.randomUUID(), true));
        assertEquals("SPEAKING_SUBMISSION_IN_PROGRESS", refused.getCode());
        assertThrows(LearningRequestException.class, () -> open(UUID.randomUUID()));
    }

    @Test
    void storesAFileUnderTheFolderOfItsLearnerDayAndSubmission() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());

        record(opened.id(), Q2);

        var key = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(storage).put(eq("speaking-recordings"), key.capture(), any(), eq("audio/wav"));
        assertTrue(key.getValue().startsWith("practice/" + USER + "/"), key.getValue());
        assertTrue(key.getValue().contains("_" + opened.id() + "/"), key.getValue());
        assertTrue(key.getValue().matches(".*/part1_q02_[0-9a-f]{8}\\.wav"), key.getValue());
        assertTrue(submissions.get(opened.id()).recordingFor(Q2).orElseThrow().isUploaded());
    }

    @Test
    void recordingAQuestionAgainUsesANewKeyAndDeletesTheOldFile() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());
        record(opened.id(), Q1);
        String first = submissions.get(opened.id()).recordingFor(Q1).orElseThrow().objectKey();

        record(opened.id(), Q1);

        String second = submissions.get(opened.id()).recordingFor(Q1).orElseThrow().objectKey();
        assertTrue(second.matches(".*/part1_q01_r2_[0-9a-f]{8}\\.wav"), second);
        verify(storage).delete("speaking-recordings", first);
        verify(storage, never()).delete("speaking-recordings", second);
    }

    @Test
    void aFailedWriteLeavesTheQuestionWithoutAFile() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());
        org.mockito.Mockito.doThrow(StorageException.withoutObject("put", StorageException.Reason.UNAVAILABLE))
                .when(storage).put(anyString(), anyString(), any(), anyString());

        assertThrows(StorageException.class, () -> record(opened.id(), Q1));

        assertFalse(submissions.get(opened.id()).recordingFor(Q1).orElseThrow().isUploaded());
    }

    @Test
    void aKeyThatIsAlreadyTakenIsAConflictNotAServerError() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());
        org.mockito.Mockito.doThrow(StorageException.withoutObject("put", StorageException.Reason.ALREADY_EXISTS))
                .when(storage).put(anyString(), anyString(), any(), anyString());

        LearningRequestException refused = assertThrows(LearningRequestException.class, () -> record(opened.id(), Q1));

        assertEquals("SPEAKING_UPLOAD_CONFLICT", refused.getCode());
    }

    @Test
    void refusesAFileThatIsNotAudioOrEmpty() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class,
                () -> upload.execute(USER, opened.id(), Q1, "audio/wav", "not audio at all".getBytes(), null));
        assertThrows(IllegalArgumentException.class, () -> upload.execute(USER, opened.id(), Q1, "audio/wav", new byte[0], null));
        assertThrows(IllegalArgumentException.class, () -> upload.execute(USER, opened.id(), Q1, "text/plain", wav(), null));
        verify(storage, never()).put(anyString(), anyString(), any(), anyString());
    }

    @Test
    void takesOnlyWavAtSixteenKilohertzMono() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());

        // The browser's own format is refused: it has to be converted before it is sent.
        assertThrows(IllegalArgumentException.class,
                () -> upload.execute(USER, opened.id(), Q1, "audio/webm", "webm".getBytes(), null));
        for (byte[] unsuitable : List.of(wav(44100, 1), wav(16000, 2))) {
            LearningRequestException refused = assertThrows(LearningRequestException.class,
                    () -> upload.execute(USER, opened.id(), Q1, "audio/wav", unsuitable, null));
            assertEquals("RECORDING_FORMAT_UNSUPPORTED", refused.getCode());
            assertEquals(422, refused.getStatus());
        }
        verify(storage, never()).put(anyString(), anyString(), any(), anyString());
    }

    @Test
    void refusesAQuestionOfAnotherSubmissionAndASubmissionOfAnotherLearner() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());

        assertEquals("SPEAKING_QUESTION_NOT_FOUND", assertThrows(LearningRequestException.class,
                () -> record(opened.id(), UUID.randomUUID())).getCode());
        assertEquals(404, assertThrows(LearningRequestException.class, () -> upload.execute(UUID.randomUUID(), opened.id(),
                Q1, "audio/wav", wav(), null)).getStatus());
        verify(storage, never()).put(anyString(), anyString(), any(), anyString());
    }

    @Test
    void completingNeedsEveryRecordingThenQueuesTheSubmission() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());
        record(opened.id(), Q1);
        record(opened.id(), Q2);

        LearningRequestException missing = assertThrows(LearningRequestException.class,
                () -> complete.execute(USER, opened.id()));
        assertEquals("SPEAKING_RECORDINGS_MISSING", missing.getCode());
        assertEquals(422, missing.getStatus());

        record(opened.id(), Q3);
        assertEquals(SpeakingSubmissionStatus.QUEUED, complete.execute(USER, opened.id()).status());
        // A second call, such as a retry after a lost response, does not change anything.
        assertEquals(SpeakingSubmissionStatus.QUEUED, complete.execute(USER, opened.id()).status());
    }

    @Test
    void completingKeepsTheContentTheGradingJobNeedsBecauseItHoldsNoLearnerToken() {
        String context = "{\"errorTypes\":[],\"knowledgePoints\":[]}";
        SpeakingGradingContextCapture capture = mock(SpeakingGradingContextCapture.class);
        when(capture.capture()).thenReturn(context);
        when(content.getActiveRubric("SPEAKING")).thenReturn(new LearningContentClient.Rubric("IELTS_SPEAKING", "SPEAKING",
                2, "note", List.of(), List.of()));
        CompleteSpeakingSubmissionUseCase withContext = new CompleteSpeakingSubmissionUseCase(submissions, userId -> { },
                transactions, content, new com.fasterxml.jackson.databind.ObjectMapper(), capture);
        SpeakingSubmissionResult opened = open(UUID.randomUUID());
        for (UUID question : List.of(Q1, Q2, Q3)) record(opened.id(), question);

        withContext.execute(USER, opened.id());
        // Completing again does not capture a second time or replace the first copy.
        withContext.execute(USER, opened.id());

        assertEquals(Optional.of(context), submissions.findGradingContext(opened.id()));
        verify(capture).capture();
    }

    @Test
    void aSubmissionCannotBeOpenedWhenGradingCannotRun() {
        when(speech.available()).thenReturn(false);

        LearningRequestException refused = assertThrows(LearningRequestException.class, () -> open(UUID.randomUUID()));

        assertEquals("SPEAKING_GRADING_DISABLED", refused.getCode());
        assertEquals(503, refused.getStatus());
        assertTrue(submissions.all().isEmpty());
    }

    @Test
    void aQueuedSubmissionTakesNoMoreRecordings() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());
        for (UUID question : List.of(Q1, Q2, Q3)) record(opened.id(), question);
        complete.execute(USER, opened.id());

        LearningRequestException refused = assertThrows(LearningRequestException.class, () -> record(opened.id(), Q1));

        assertEquals("SPEAKING_SUBMISSION_NOT_UPLOADING", refused.getCode());
    }

    @Test
    void anAbandonedUploadCannotBeCompleted() {
        SpeakingSubmissionResult stale = open(UUID.randomUUID());
        submissions.age(stale.id(), 3600);
        open(UUID.randomUUID());

        assertEquals("SPEAKING_SUBMISSION_ABANDONED", assertThrows(LearningRequestException.class,
                () -> complete.execute(USER, stale.id())).getCode());
    }

    @Test
    void theResultNeverShowsObjectKeys() {
        SpeakingSubmissionResult opened = open(UUID.randomUUID());
        SpeakingSubmissionResult stored = upload.execute(USER, opened.id(), Q1, "audio/wav", wav(), 4000);

        assertFalse(stored.toString().contains("practice/"));
        assertTrue(stored.recordings().get(0).uploaded());
        assertEquals(4000, stored.recordings().get(0).durationMs());
    }

    /** A repository that keeps submissions in a map; {@code age} moves a submission's last change into the past. */
    private static final class InMemorySubmissions implements SpeakingSubmissionRepository {
        private final Map<UUID, SpeakingSubmission> byId = new LinkedHashMap<>();
        private final Map<UUID, String> contexts = new LinkedHashMap<>();

        List<SpeakingSubmission> all() {
            return new ArrayList<>(byId.values());
        }

        SpeakingSubmission get(UUID id) {
            return byId.get(id);
        }

        void age(UUID id, long seconds) {
            SpeakingSubmission current = byId.get(id);
            byId.put(id, SpeakingSubmission.restore(current.id(), current.userId(), current.requestId(),
                    current.packageVersionId(), current.scope(), current.status(), current.pointCost(),
                    current.consentAt(), current.failureCode(), current.createdAt().minusSeconds(seconds),
                    current.updatedAt().minusSeconds(seconds), current.queuedAt(), current.recordings()));
        }

        @Override
        public Optional<SpeakingSubmission> findLatestForAttempt(UUID userId, UUID practiceAttemptId) {
            return byId.values().stream().filter(found -> found.userId().equals(userId)
                    && practiceAttemptId.equals(found.practiceAttemptId()))
                    .max(java.util.Comparator.comparing(SpeakingSubmission::createdAt));
        }

        @Override
        public Optional<SpeakingSubmission> findByRequestId(UUID requestId) {
            return byId.values().stream().filter(found -> found.requestId().equals(requestId)).findFirst();
        }

        @Override
        public Optional<SpeakingSubmission> findOwned(UUID id, UUID userId) {
            return Optional.ofNullable(byId.get(id)).filter(found -> found.userId().equals(userId));
        }

        @Override
        public Optional<SpeakingSubmission> findForUpdate(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<SpeakingSubmission> findActive(UUID userId) {
            return byId.values().stream().filter(found -> found.userId().equals(userId) && found.status().isActive()).toList();
        }

        @Override
        public void save(SpeakingSubmission submission) {
            byId.put(submission.id(), submission);
        }

        @Override
        public Optional<SpeakingSubmission> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        // The grading queue and the stored analyses are exercised by their own tests, not by these use cases.
        @Override
        public Optional<SpeakingSubmission> claimNext(java.time.Instant staleBefore, int maxAttempts) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findAnalysis(UUID submissionId, UUID questionVersionId) {
            return Optional.empty();
        }

        @Override
        public void saveAnalysis(UUID submissionId, UUID questionVersionId, String analysisJson) {
        }

        @Override
        public List<SpeakingSubmission> findExpiredBefore(java.time.Instant cutoff, int limit) {
            return List.of();
        }

        @Override
        public boolean deleteExpired(UUID id, java.time.Instant cutoff) {
            return false;
        }

        @Override
        public void touch(UUID id) {
        }

        @Override
        public Optional<String> findGradingContext(UUID submissionId) {
            return Optional.ofNullable(contexts.get(submissionId));
        }

        @Override
        public void saveGradingContext(UUID submissionId, String contextJson) {
            contexts.putIfAbsent(submissionId, contextJson);
        }
    }
}
