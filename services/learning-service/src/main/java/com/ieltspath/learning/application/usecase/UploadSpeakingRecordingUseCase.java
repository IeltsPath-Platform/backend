package com.ieltspath.learning.application.usecase;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingRecordingKeys;
import com.ieltspath.learning.application.service.WavFormat;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores the recording of one question in the bucket and records its key on the submission. The file is written outside
 * any transaction; the storage never overwrites a key, so a question recorded again gets a new key and the old file is
 * deleted afterwards (best effort).
 */
@Service
public class UploadSpeakingRecordingUseCase {
    private static final Logger log = LoggerFactory.getLogger(UploadSpeakingRecordingUseCase.class);
    /** Only WAV: the browser converts its recording before sending, because the speech analysis reads nothing else. */
    private static final String[] ALLOWED_TYPES = {"audio/wav"};
    private static final int MAX_DURATION_MS = 30 * 60 * 1000;

    private final SpeakingSubmissionRepository submissions;
    private final ObjectStorage storage;
    private final LearnerLock lock;
    private final SpeakingPracticeSettings settings;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public UploadSpeakingRecordingUseCase(SpeakingSubmissionRepository submissions, @Lazy ObjectStorage storage,
                                          LearnerLock lock, SpeakingPracticeSettings settings,
                                          PlatformTransactionManager transactionManager) {
        this.submissions = submissions;
        this.storage = storage;
        this.lock = lock;
        this.settings = settings;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public SpeakingSubmissionResult execute(UUID userId, UUID submissionId, UUID questionVersionId,
                                            String declaredContentType, byte[] content, Integer durationMs) {
        if (durationMs != null && (durationMs < 0 || durationMs > MAX_DURATION_MS)) {
            throw new IllegalArgumentException("durationMs is out of range");
        }
        SpeakingSubmission snapshot = submissions.findOwned(submissionId, userId).orElseThrow(UploadSpeakingRecordingUseCase::notFound);
        if (!snapshot.isAcceptingRecordings()) {
            throw new LearningRequestException(409, "SPEAKING_SUBMISSION_NOT_UPLOADING",
                    "This submission no longer takes recordings", submissionId);
        }
        SpeakingRecording current = snapshot.recordingFor(questionVersionId).orElseThrow(
                () -> new LearningRequestException(404, "SPEAKING_QUESTION_NOT_FOUND", "The question is not in this submission"));

        String contentType = UploadPolicy.of(settings.maxRecordingBytes(), ALLOWED_TYPES).check(content, declaredContentType);
        if (!WavFormat.isPcm16kMono(content)) {
            throw new LearningRequestException(422, "RECORDING_FORMAT_UNSUPPORTED",
                    "The recording must be WAV, 16-bit, mono, 16 kHz");
        }
        int attempt = current.attempt() + 1;
        String key = SpeakingRecordingKeys.uniqueKey(userId, snapshot.createdAt().atZone(ZoneId.of(settings.timezone())).toLocalDate(),
                submissionId, current.partNo(), current.questionNo(), attempt, ContentTypes.extensionFor(contentType));
        ObjectKeys.requireSafe(key);

        try {
            storage.put(settings.bucket(), key, content, contentType);
        } catch (StorageException exception) {
            if (exception.reason() == StorageException.Reason.ALREADY_EXISTS) {
                throw new LearningRequestException(409, "SPEAKING_UPLOAD_CONFLICT",
                        "This recording is being uploaded by another request", submissionId);
            }
            throw exception;
        }

        Attached attached;
        try {
            attached = transaction.execute(status -> attach(userId, submissionId, questionVersionId, current.attempt(),
                    attempt, key, contentType, content.length, durationMs));
        } catch (RuntimeException exception) {
            deleteQuietly(key);
            throw exception;
        }
        attached.replacedKey().ifPresent(this::deleteQuietly);
        return SpeakingSubmissionResult.from(attached.submission());
    }

    private record Attached(SpeakingSubmission submission, Optional<String> replacedKey) { }

    private Attached attach(UUID userId, UUID submissionId, UUID questionVersionId, int expectedAttempt,
                            int attempt, String key, String contentType, long size, Integer durationMs) {
        lock.lock(userId);
        SpeakingSubmission submission = submissions.findForUpdate(submissionId).orElseThrow(UploadSpeakingRecordingUseCase::notFound);
        if (!submission.isAcceptingRecordings()) {
            throw new LearningRequestException(409, "SPEAKING_SUBMISSION_NOT_UPLOADING",
                    "This submission no longer takes recordings", submissionId);
        }
        // Another upload for the same question finished while this file was being stored.
        if (submission.recordingFor(questionVersionId).map(SpeakingRecording::attempt).orElse(-1) != expectedAttempt) {
            throw new LearningRequestException(409, "SPEAKING_UPLOAD_CONFLICT",
                    "This recording is being uploaded by another request", submissionId);
        }
        Optional<String> replaced = submission.attachRecording(questionVersionId, attempt, key, contentType, size,
                durationMs, Instant.now(clock));
        submissions.save(submission);
        return new Attached(submission, replaced);
    }

    private void deleteQuietly(String key) {
        try {
            storage.delete(settings.bucket(), key);
        } catch (RuntimeException exception) {
            // The file stays orphaned until retention removes the submission folder; nothing else depends on it.
            log.warn("Speaking recording cleanup failed: reason={}", exception instanceof StorageException storage
                    ? storage.reason() : exception.getClass().getSimpleName());
        }
    }

    private static LearningRequestException notFound() {
        return new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND", "Speaking submission not found");
    }
}
