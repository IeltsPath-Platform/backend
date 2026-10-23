package com.ieltspath.content.domain.exception;

import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Issue;

import java.util.List;

/**
 * A course draft cannot be approved. Names that are already taken are a conflict and are reported alone; any other
 * blocking problem makes the draft invalid. Nothing has been created.
 */
public class CourseDraftErrorsException extends ContentDomainException {

    private final List<Issue> issues;
    private final boolean conflict;

    public CourseDraftErrorsException(List<Issue> issues, boolean conflict) {
        super(conflict ? "A code in the course draft is already taken" : "The course draft has errors");
        this.issues = List.copyOf(issues);
        this.conflict = conflict;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    /** True when {@link #getIssues()} are the {@link Issue#isConflict() conflicts}. */
    public boolean isConflict() {
        return conflict;
    }
}
