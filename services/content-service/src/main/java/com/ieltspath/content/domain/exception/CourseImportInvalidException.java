package com.ieltspath.content.domain.exception;

import com.ieltspath.content.domain.vo.CourseImportModel.Issue;

import java.util.List;

/** The workbook has errors, so nothing was created. Carries the full report. */
public class CourseImportInvalidException extends ContentDomainException {

    private final List<Issue> errors;
    private final List<Issue> warnings;

    public CourseImportInvalidException(List<Issue> errors, List<Issue> warnings) {
        super("The workbook has errors");
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
    }

    public List<Issue> getErrors() {
        return errors;
    }

    public List<Issue> getWarnings() {
        return warnings;
    }
}
