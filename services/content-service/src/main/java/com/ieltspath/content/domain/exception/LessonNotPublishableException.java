package com.ieltspath.content.domain.exception;

import java.util.List;

public class LessonNotPublishableException extends ContentDomainException {
    private final List<String> issues;
    public LessonNotPublishableException(List<String> issues) {
        super("Lesson does not meet publication requirements");
        this.issues = List.copyOf(issues);
    }
    public List<String> getIssues() { return issues; }
}
