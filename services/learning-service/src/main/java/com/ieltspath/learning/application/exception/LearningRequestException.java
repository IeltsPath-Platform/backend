package com.ieltspath.learning.application.exception;

import java.util.UUID;
import java.util.List;

public class LearningRequestException extends RuntimeException {
    private final int status;
    private final String code;
    private final UUID submissionId;
    private final List<UUID> lessonIds;

    public LearningRequestException(int status, String code, String detail) {
        this(status, code, detail, null);
    }

    /** {@code submissionId} names a stored essay the learner can resend or read, for errors after it was graded. */
    public LearningRequestException(int status, String code, String detail, UUID submissionId) {
        this(status, code, detail, submissionId, null);
    }

    public LearningRequestException(int status, String code, String detail, UUID submissionId,
                                    List<UUID> lessonIds) {
        super(detail);
        this.status = status;
        this.code = code;
        this.submissionId = submissionId;
        this.lessonIds = lessonIds == null ? null : List.copyOf(lessonIds);
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public UUID getSubmissionId() { return submissionId; }
    public List<UUID> getLessonIds() { return lessonIds; }
}
