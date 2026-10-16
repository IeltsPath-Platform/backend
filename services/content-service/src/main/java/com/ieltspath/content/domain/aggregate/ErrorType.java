package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.InvalidErrorTypeException;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.ErrorCategory;
import com.ieltspath.content.domain.vo.ErrorReviewStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A named reason an objective answer is wrong. The code is fixed at creation because answer specs and error analysis
 * rows store it; everything else can be revised.
 */
public class ErrorType {
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final int MAX_CODE_LENGTH = 60;

    private final UUID id;
    private final String code;
    private String name;
    private ErrorCategory category;
    private int severity;
    private UUID knowledgePointId;
    private String fixHint;
    private ContentStatus status;
    private ErrorReviewStatus reviewStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    public ErrorType(UUID id, String code, String name, ErrorCategory category, int severity, UUID knowledgePointId,
                     String fixHint, ContentStatus status, ErrorReviewStatus reviewStatus,
                     Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireCode(code);
        this.name = requireName(name);
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.severity = requireSeverity(severity);
        this.knowledgePointId = knowledgePointId;
        this.fixHint = requireFixHint(fixHint);
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.reviewStatus = reviewStatus != null ? reviewStatus : ErrorReviewStatus.DRAFT;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static ErrorType create(String code, String name, ErrorCategory category, int severity,
                                   UUID knowledgePointId, String fixHint) {
        Instant now = Instant.now();
        return new ErrorType(UUID.randomUUID(), code, name, category, severity, knowledgePointId, fixHint,
                ContentStatus.ACTIVE, ErrorReviewStatus.DRAFT, now, now);
    }

    /** A null {@code reviewStatus} leaves the current review state unchanged. */
    public void update(String name, ErrorCategory category, int severity, UUID knowledgePointId, String fixHint,
                       ContentStatus status, ErrorReviewStatus reviewStatus) {
        this.name = requireName(name);
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.severity = requireSeverity(severity);
        this.knowledgePointId = knowledgePointId;
        this.fixHint = requireFixHint(fixHint);
        this.status = status != null ? status : this.status;
        this.reviewStatus = reviewStatus != null ? reviewStatus : this.reviewStatus;
        this.updatedAt = Instant.now();
    }

    private static String requireCode(String value) {
        if (value == null || value.length() > MAX_CODE_LENGTH || !CODE.matcher(value).matches()) {
            throw new InvalidErrorTypeException("error code must be upper snake case, at most 60 characters");
        }
        return value;
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new InvalidErrorTypeException("error name is required and must be at most 255 characters");
        }
        return value.trim();
    }

    private static int requireSeverity(int value) {
        if (value < 1 || value > 3) {
            throw new InvalidErrorTypeException("severity must be 1, 2 or 3");
        }
        return value;
    }

    private static String requireFixHint(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidErrorTypeException("fix hint is required");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public ErrorCategory getCategory() { return category; }
    public int getSeverity() { return severity; }
    public UUID getKnowledgePointId() { return knowledgePointId; }
    public String getFixHint() { return fixHint; }
    public ContentStatus getStatus() { return status; }
    public ErrorReviewStatus getReviewStatus() { return reviewStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
