package com.ieltspath.assessment.domain.exception;

public class PlacementLimitReachedException extends AssessmentDomainException {
    public PlacementLimitReachedException() { super("Bạn đã đạt giới hạn tối đa 3 lần làm bài kiểm tra đầu vào trong 24 giờ qua."); }
}
