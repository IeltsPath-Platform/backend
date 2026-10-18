package com.ieltspath.assessment.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ExaminerQaRepository {
    void enqueueSample(UUID submissionId);
    void saveReview(UUID gradingJobId, UUID reviewerId, BigDecimal reviewerBand, String reviewerCriteria, String note);
    List<ExaminerQuality> quality();

    record ExaminerQuality(UUID examinerId, long reviewedEssays, BigDecimal averageAbsoluteBandDifference,
                           BigDecimal overOneBandRate) {}
}
