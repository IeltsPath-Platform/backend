package com.group01.learning.application.exception;

import java.util.UUID;

public class LearningRequestException extends RuntimeException {
    private final int status;
    private final String code;
    private final UUID submissionId;

    public LearningRequestException(int status, String code, String detail) {
        this(status, code, detail, null);
    }

    /** {@code submissionId} names a stored essay the learner can resend or read, for errors after it was graded. */
    public LearningRequestException(int status, String code, String detail, UUID submissionId) {
        super(detail);
        this.status = status;
        this.code = code;
        this.submissionId = submissionId;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public UUID getSubmissionId() { return submissionId; }
}
