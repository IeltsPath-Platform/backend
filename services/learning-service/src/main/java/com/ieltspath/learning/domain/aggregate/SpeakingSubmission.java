package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * An attempt at a Speaking test sent for AI grading. It starts {@code UPLOADING} with an empty recording for every
 * question to answer; once every question has a stored file, {@link #complete} queues it. A learner has at most one active
 * submission (see {@link SpeakingSubmissionStatus#isActive()}).
 */
public class SpeakingSubmission {
    /** Failure code of a submission whose recordings held no speech. */
    public static final String NO_SPEECH = "NO_SPEECH";

    private final UUID id;
    private final UUID userId;
    private final UUID requestId;
    private final UUID packageVersionId;
    private final SpeakingScope scope;
    private final int pointCost;
    private final Instant consentAt;
    private final Instant createdAt;
    private final List<SpeakingRecording> recordings;
    private SpeakingSubmissionStatus status;
    private String failureCode;
    private Map<String, Object> result;
    private BigDecimal overallBand;
    private UUID debitLedgerEntryId;
    private String rubricCode;
    private Integer rubricVersion;
    private String promptChecksum;
    private String promptVersion;
    private int gradingAttempts;
    private Instant gradingStartedAt;
    private Instant gradedAt;
    private Map<String, Object> rubricSnapshot;
    private Instant updatedAt;
    private Instant queuedAt;
    private boolean isNew;

    private SpeakingSubmission(UUID id, UUID userId, UUID requestId, UUID packageVersionId, SpeakingScope scope,
                               SpeakingSubmissionStatus status, int pointCost, Instant consentAt, String failureCode,
                               Instant createdAt, Instant updatedAt, Instant queuedAt,
                               List<SpeakingRecording> recordings, boolean isNew, Map<String, Object> result,
                               BigDecimal overallBand, UUID debitLedgerEntryId, String rubricCode,
                               Integer rubricVersion, String promptChecksum, String promptVersion,
                               int gradingAttempts, Instant gradingStartedAt, Instant gradedAt,
                               Map<String, Object> rubricSnapshot) {
        this.id = id;
        this.userId = userId;
        this.requestId = requestId;
        this.packageVersionId = packageVersionId;
        this.scope = scope;
        this.status = status;
        this.pointCost = pointCost;
        this.consentAt = consentAt;
        this.failureCode = failureCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.queuedAt = queuedAt;
        this.recordings = new ArrayList<>(recordings);
        this.result = immutableMap(result);
        this.overallBand = overallBand;
        this.debitLedgerEntryId = debitLedgerEntryId;
        this.rubricCode = rubricCode;
        this.rubricVersion = rubricVersion;
        this.promptChecksum = promptChecksum;
        this.promptVersion = promptVersion;
        this.gradingAttempts = gradingAttempts;
        this.gradingStartedAt = gradingStartedAt;
        this.gradedAt = gradedAt;
        this.rubricSnapshot = immutableMap(rubricSnapshot);
        this.recordings.sort(ORDER);
        this.isNew = isNew;
    }

    private static final Comparator<SpeakingRecording> ORDER =
            Comparator.comparingInt(SpeakingRecording::partNo).thenComparingInt(SpeakingRecording::questionNo);

    /** A new submission; {@code expected} are the questions to answer, each with its part and place in the part. */
    public static SpeakingSubmission start(UUID id, UUID userId, UUID requestId, UUID packageVersionId,
                                           SpeakingScope scope, int pointCost, Instant consentAt,
                                           List<SpeakingRecording> expected, Instant now) {
        if (expected.isEmpty()) throw new IllegalArgumentException("a Speaking submission needs at least one question");
        if (pointCost <= 0) throw new IllegalArgumentException("pointCost must be positive");
        return new SpeakingSubmission(id, userId, requestId, packageVersionId, scope, SpeakingSubmissionStatus.UPLOADING,
                pointCost, consentAt, null, now, now, null, expected, true, null, null, null,
                null, null, null, null, 0, null, null, null);
    }

    public static SpeakingSubmission restore(UUID id, UUID userId, UUID requestId, UUID packageVersionId,
                                             SpeakingScope scope, SpeakingSubmissionStatus status, int pointCost,
                                             Instant consentAt, String failureCode, Instant createdAt,
                                             Instant updatedAt, Instant queuedAt, List<SpeakingRecording> recordings) {
        return new SpeakingSubmission(id, userId, requestId, packageVersionId, scope, status, pointCost, consentAt,
                failureCode, createdAt, updatedAt, queuedAt, recordings, false, null, null, null,
                null, null, null, null, 0, null, null, null);
    }

    public static SpeakingSubmission restore(UUID id, UUID userId, UUID requestId, UUID packageVersionId,
                                             SpeakingScope scope, SpeakingSubmissionStatus status, int pointCost,
                                             Instant consentAt, String failureCode, Instant createdAt,
                                             Instant updatedAt, Instant queuedAt, List<SpeakingRecording> recordings,
                                             Map<String, Object> result, BigDecimal overallBand, UUID debitLedgerEntryId,
                                             String rubricCode, Integer rubricVersion, String promptChecksum,
                                             String promptVersion, int gradingAttempts, Instant gradingStartedAt,
                                             Instant gradedAt, Map<String, Object> rubricSnapshot) {
        return new SpeakingSubmission(id, userId, requestId, packageVersionId, scope, status, pointCost, consentAt,
                failureCode, createdAt, updatedAt, queuedAt, recordings, false, result, overallBand,
                debitLedgerEntryId, rubricCode, rubricVersion, promptChecksum, promptVersion, gradingAttempts,
                gradingStartedAt, gradedAt, rubricSnapshot);
    }

    public Optional<SpeakingRecording> recordingFor(UUID questionVersionId) {
        return recordings.stream().filter(recording -> recording.questionVersionId().equals(questionVersionId))
                .findFirst();
    }

    public boolean isAcceptingRecordings() {
        return status == SpeakingSubmissionStatus.UPLOADING;
    }

    public boolean hasAllRecordings() {
        return recordings.stream().allMatch(SpeakingRecording::isUploaded);
    }

    public long missingRecordings() {
        return recordings.stream().filter(recording -> !recording.isUploaded()).count();
    }

    /** An upload that nobody finished: still UPLOADING and untouched since {@code cutoff}. */
    public boolean isStaleUpload(Instant cutoff) {
        return status == SpeakingSubmissionStatus.UPLOADING && updatedAt.isBefore(cutoff);
    }

    /**
     * Records that the file for the question is stored. Returns the object key it replaces, if the question already had
     * a file, so the caller can delete it.
     */
    public Optional<String> attachRecording(UUID questionVersionId, int attempt, String objectKey, String contentType,
                                            long sizeBytes, Integer durationMs, Instant now) {
        if (!isAcceptingRecordings()) throw new IllegalStateException("the submission no longer takes recordings");
        for (int index = 0; index < recordings.size(); index++) {
            SpeakingRecording current = recordings.get(index);
            if (!current.questionVersionId().equals(questionVersionId)) continue;
            recordings.set(index, current.uploaded(attempt, objectKey, contentType, sizeBytes, durationMs, now));
            updatedAt = now;
            return Optional.ofNullable(current.objectKey());
        }
        throw new IllegalArgumentException("the question is not part of this submission");
    }

    /** Queues the submission once every question has its file. */
    public void complete(Instant now) {
        if (status != SpeakingSubmissionStatus.UPLOADING) throw new IllegalStateException("not uploading");
        if (!hasAllRecordings()) throw new IllegalStateException("recordings are missing");
        status = SpeakingSubmissionStatus.QUEUED;
        queuedAt = now;
        updatedAt = now;
    }

    /** Gives up an upload nobody finished, so the learner can start again. */
    public void abandon(Instant now) {
        if (status != SpeakingSubmissionStatus.UPLOADING) throw new IllegalStateException("not uploading");
        status = SpeakingSubmissionStatus.ABANDONED;
        failureCode = "UPLOAD_ABANDONED";
        updatedAt = now;
    }

    public void startAnalysis(Instant now) {
        status = SpeakingSubmissionStatus.ANALYZING;
        failureCode = null;
        gradingAttempts++;
        gradingStartedAt = now;
        updatedAt = now;
    }

    public void beginGrading(String rubricCode, int rubricVersion, String promptChecksum, String promptVersion,
                             Instant now) {
        if (this.rubricCode == null) {
            this.rubricCode = rubricCode;
            this.rubricVersion = rubricVersion;
            this.promptChecksum = promptChecksum;
            this.promptVersion = promptVersion;
        }
        status = SpeakingSubmissionStatus.GRADING;
        updatedAt = now;
    }

    public void saveRubricSnapshot(Map<String, Object> rubricSnapshot) {
        if (this.rubricSnapshot == null) this.rubricSnapshot = immutableMap(rubricSnapshot);
    }

    public void paymentPending(Map<String, Object> result, BigDecimal overallBand, Instant now) {
        this.result = immutableMap(result);
        this.overallBand = overallBand;
        status = SpeakingSubmissionStatus.PAYMENT_PENDING;
        failureCode = null;
        updatedAt = now;
    }

    public void graded(UUID ledgerEntryId, Instant now) {
        if (status != SpeakingSubmissionStatus.PAYMENT_PENDING || result == null) {
            throw new IllegalStateException("a graded submission needs a saved grade and completed payment");
        }
        debitLedgerEntryId = ledgerEntryId;
        status = SpeakingSubmissionStatus.GRADED;
        gradedAt = now;
        updatedAt = now;
    }

    public void retryable(String code, Instant now) {
        status = SpeakingSubmissionStatus.FAILED_RETRYABLE;
        failureCode = code;
        updatedAt = now;
    }

    /**
     * A failure that is not the grading's fault (a daily limit, say): the submission waits again, and the claim that led
     * here does not count against {@code maxAttempts}.
     */
    public void deferred(String code, Instant now) {
        gradingAttempts = Math.max(0, gradingAttempts - 1);
        retryable(code, now);
    }

    /**
     * Nothing could be heard in any recording. Nothing was graded or charged, so the submission ends and frees the
     * learner to record again.
     */
    public void noSpeech(Instant now) {
        status = SpeakingSubmissionStatus.ABANDONED;
        failureCode = NO_SPEECH;
        updatedAt = now;
    }

    public void needsExaminer(String code, Instant now) {
        status = SpeakingSubmissionStatus.NEEDS_EXAMINER;
        failureCode = code;
        updatedAt = now;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID requestId() { return requestId; }
    public UUID packageVersionId() { return packageVersionId; }
    public SpeakingScope scope() { return scope; }
    public SpeakingSubmissionStatus status() { return status; }
    public int pointCost() { return pointCost; }
    public Instant consentAt() { return consentAt; }
    public String failureCode() { return failureCode; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant queuedAt() { return queuedAt; }
    public List<SpeakingRecording> recordings() { return List.copyOf(recordings); }
    public boolean isNew() { return isNew; }

    private static Map<String, Object> immutableMap(Map<String, Object> value) {
        return value == null ? null : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(value));
    }
    public Map<String, Object> result() { return result; }
    public BigDecimal overallBand() { return overallBand; }
    public UUID debitLedgerEntryId() { return debitLedgerEntryId; }
    public String rubricCode() { return rubricCode; }
    public Integer rubricVersion() { return rubricVersion; }
    public String promptChecksum() { return promptChecksum; }
    public String promptVersion() { return promptVersion; }
    public int gradingAttempts() { return gradingAttempts; }
    public Instant gradingStartedAt() { return gradingStartedAt; }
    public Instant gradedAt() { return gradedAt; }
    public Map<String, Object> rubricSnapshot() { return rubricSnapshot; }

    /** Called by the repository after the first write. */
    public void persisted() { isNew = false; }
}
