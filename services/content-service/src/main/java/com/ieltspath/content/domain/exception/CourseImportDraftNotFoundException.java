package com.ieltspath.content.domain.exception;

/** Also used when the draft belongs to another author, so one author cannot tell whose drafts exist. */
public class CourseImportDraftNotFoundException extends ContentDomainException {
    public CourseImportDraftNotFoundException() {
        super("Không tìm thấy bản nháp nhập khóa học");
    }
}
