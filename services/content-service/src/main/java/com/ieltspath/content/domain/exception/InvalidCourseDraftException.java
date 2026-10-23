package com.ieltspath.content.domain.exception;

import java.util.List;

/** A course draft is malformed: something required is missing, too long, out of range or used twice. */
public class InvalidCourseDraftException extends ContentDomainException {

    private final List<Problem> issues;

    public InvalidCourseDraftException(List<Problem> issues) {
        super("The course draft is not valid");
        this.issues = List.copyOf(issues);
    }

    public List<Problem> getIssues() {
        return issues;
    }

    /** {@code problem} is {@code REQUIRED}, {@code TOO_LONG}, {@code TOO_MANY}, {@code INVALID_VALUE} or {@code DUPLICATE}. */
    public record Problem(String path, String problem) {
    }
}
