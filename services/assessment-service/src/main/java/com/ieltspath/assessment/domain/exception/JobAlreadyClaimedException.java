package com.ieltspath.assessment.domain.exception;

public class JobAlreadyClaimedException extends RuntimeException {
    public JobAlreadyClaimedException() { super("Bài chấm này đã có giám khảo khác nhận hoặc không còn khả dụng."); }
}
