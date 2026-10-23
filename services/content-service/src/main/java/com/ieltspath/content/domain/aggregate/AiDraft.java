package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One request to the AI and its outcome. The state machine is the only place that decides what may happen next:
 * a draft is generated once, reviewed once, and a failed one is replaced by a new request rather than retried here.
 */
public class AiDraft {

    private final UUID id;
    private final DraftKind kind;
    private DraftStatus status;
    private final UUID requestedBy;
    private final String requestJson;
    private String resultJson;
    private String errorCode;
    private String model;
    private String promptVersion;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private String reviewNote;

    public AiDraft(UUID id, DraftKind kind, DraftStatus status, UUID requestedBy, String requestJson, String resultJson,
                   String errorCode, String model, String promptVersion, Instant createdAt, Instant startedAt,
                   Instant finishedAt, UUID reviewedBy, Instant reviewedAt, String reviewNote) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        this.requestJson = Objects.requireNonNull(requestJson, "requestJson must not be null");
        this.resultJson = resultJson;
        this.errorCode = errorCode;
        this.model = model;
        this.promptVersion = promptVersion;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNote = reviewNote;
    }

    public static AiDraft request(DraftKind kind, UUID requestedBy, String requestJson, Instant now) {
        return new AiDraft(UUID.randomUUID(), kind, DraftStatus.QUEUED, requestedBy, requestJson, null, null, null,
                null, now, null, null, null, null, null);
    }

    /** Called by the job after it claims the draft; the claim itself already moved it to GENERATING. */
    public void complete(String resultJson, String model, String promptVersion, Instant now) {
        requireStatus(DraftStatus.GENERATING);
        this.resultJson = Objects.requireNonNull(resultJson, "resultJson must not be null");
        this.model = model;
        this.promptVersion = promptVersion;
        this.status = DraftStatus.READY;
        this.finishedAt = now;
    }

    public void fail(String errorCode, Instant now) {
        requireStatus(DraftStatus.GENERATING);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.status = DraftStatus.FAILED;
        this.finishedAt = now;
    }

    /** A claim that never finished (the process died) goes back to the queue. */
    public void requeue() {
        requireStatus(DraftStatus.GENERATING);
        this.status = DraftStatus.QUEUED;
        this.startedAt = null;
    }

    public void reject(UUID reviewer, String note, Instant now) {
        requireStatus(DraftStatus.READY);
        this.status = DraftStatus.REJECTED;
        this.reviewedBy = Objects.requireNonNull(reviewer, "reviewer must not be null");
        this.reviewedAt = now;
        this.reviewNote = note;
    }

    /** Approval is recorded by the kind's own use case, which also creates the content. */
    public void approve(UUID reviewer, Instant now) {
        requireStatus(DraftStatus.READY);
        this.status = DraftStatus.APPROVED;
        this.reviewedBy = Objects.requireNonNull(reviewer, "reviewer must not be null");
        this.reviewedAt = now;
    }

    private void requireStatus(DraftStatus expected) {
        if (status != expected) {
            throw new InvalidContentStateException("AI draft is " + status + ", expected " + expected);
        }
    }

    public UUID getId() { return id; }
    public DraftKind getKind() { return kind; }
    public DraftStatus getStatus() { return status; }
    public UUID getRequestedBy() { return requestedBy; }
    public String getRequestJson() { return requestJson; }
    public String getResultJson() { return resultJson; }
    public String getErrorCode() { return errorCode; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
}
