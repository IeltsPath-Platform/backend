package com.ieltspath.assessment.domain.exception;

import java.util.UUID;

public class AssessmentNotFoundException extends AssessmentDomainException {
    public AssessmentNotFoundException(UUID id) { super("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."); }
    public AssessmentNotFoundException(String message) { super(message); }
}
