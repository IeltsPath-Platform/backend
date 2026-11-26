package com.ieltspath.assessment.domain.exception;

public class HumanGradingCreditInsufficientException extends AssessmentDomainException {
    public HumanGradingCreditInsufficientException() {
        super("Bạn không đủ lượt chấm bài bởi giám khảo. Vui lòng mua thêm lượt để tiếp tục.");
    }
}
