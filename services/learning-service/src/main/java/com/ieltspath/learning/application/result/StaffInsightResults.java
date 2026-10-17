package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.StaffRecords.EdgeStat;
import com.ieltspath.learning.domain.vo.StaffRecords.KnowledgePointStat;
import com.ieltspath.learning.domain.vo.StaffRecords.LearnerFlag;
import com.ieltspath.learning.domain.vo.StaffRecords.QuestionStat;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Results of the staff insight endpoints. Names of courses and knowledge points come from Content; here only ids. */
public final class StaffInsightResults {
    private StaffInsightResults() {}

    /**
     * One learner at a glance. {@code knowledgePointsByTier} counts points per mastery tier; the full report is
     * {@code /api/learning/admin/learners/{userId}/report}. Profile and plan come from user and access service.
     */
    public record Overview(UUID userId, List<CourseProgress> courses, Map<String, Integer> knowledgePointsByTier,
                           List<PendingReview> pendingReviews, List<Diagnosis> diagnoses, List<Remediation> remediation,
                           List<LearnerFlag> flags, List<WorkItem> openWork,
                           SnapshotReportResult.SnapshotView weekly) {}

    /** {@code status} is the course score status; {@code total} the current weighted total. */
    public record CourseProgress(UUID courseId, int topics, int topicsDone, Instant passedAt, String status,
                                 double total, double passThreshold) {}

    public record PendingReview(UUID reviewId, UUID knowledgePointId, boolean blocking, String triggerKind) {}

    public record Diagnosis(UUID sessionId, UUID rootKnowledgePointId, String status, int probes) {}

    public record Remediation(UUID courseId, UUID topicId, String kind, boolean open, Instant createdAt) {}

    /** {@code probes}, {@code passed} and {@code concludedAsRoot} are over concluded diagnoses. */
    public record TreeQuality(List<EdgeStat> edges, List<KnowledgePointStat> knowledgePoints) {}

    public record QuestionStats(List<QuestionStat> questions) {}
}
