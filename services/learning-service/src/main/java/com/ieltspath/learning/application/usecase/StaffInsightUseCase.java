package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.StaffInsightResults.CourseProgress;
import com.ieltspath.learning.application.result.StaffInsightResults.Diagnosis;
import com.ieltspath.learning.application.result.StaffInsightResults.Overview;
import com.ieltspath.learning.application.result.StaffInsightResults.PendingReview;
import com.ieltspath.learning.application.result.StaffInsightResults.QuestionStats;
import com.ieltspath.learning.application.result.StaffInsightResults.Remediation;
import com.ieltspath.learning.application.result.StaffInsightResults.TreeQuality;
import com.ieltspath.learning.application.service.SnapshotService;
import com.ieltspath.learning.application.service.TreeQualitySettings;
import com.ieltspath.learning.domain.repository.SnapshotRepository;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.RemediationFlagRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** What staff see about a learner, and the statistics R&D uses to improve content. Read-only. */
@Service
public class StaffInsightUseCase {
    private static final int QUESTION_LIMIT = 200;

    private final StaffInsightRepository insight;
    private final LearnerCurriculumRepository curricula;
    private final CourseProgressRepository courseProgress;
    private final GetCourseScoreUseCase scores;
    private final MasteryProfileRepository profiles;
    private final ReviewItemRepository reviews;
    private final DiagnosticRepository diagnostics;
    private final RemediationFlagRepository remediation;
    private final LearningContentClient content;
    private final TreeQualitySettings settings;
    private final SnapshotRepository snapshots;
    private final SnapshotService snapshotService;

    public StaffInsightUseCase(StaffInsightRepository insight, LearnerCurriculumRepository curricula,
                               CourseProgressRepository courseProgress, GetCourseScoreUseCase scores,
                               MasteryProfileRepository profiles, ReviewItemRepository reviews,
                               DiagnosticRepository diagnostics, RemediationFlagRepository remediation,
                               LearningContentClient content, TreeQualitySettings settings,
                               SnapshotRepository snapshots, SnapshotService snapshotService) {
        this.snapshots = snapshots;
        this.snapshotService = snapshotService;
        this.insight = insight;
        this.curricula = curricula;
        this.courseProgress = courseProgress;
        this.scores = scores;
        this.profiles = profiles;
        this.reviews = reviews;
        this.diagnostics = diagnostics;
        this.remediation = remediation;
        this.content = content;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public Overview overview(UUID userId) {
        List<TopicProgress> topics = curricula.find(userId).topics().stream()
                .filter(topic -> topic.courseId() != null && topic.sequenceOrder() != null).toList();
        Map<UUID, int[]> counts = new LinkedHashMap<>();
        for (TopicProgress topic : topics) {
            int[] count = counts.computeIfAbsent(topic.courseId(), ignored -> new int[2]);
            count[0]++;
            if (topic.done()) count[1]++;
        }
        var passed = courseProgress.findAll(userId);
        List<CourseProgress> courses = new ArrayList<>();
        List<Remediation> remediations = new ArrayList<>();
        for (var entry : counts.entrySet()) {
            var score = scores.execute(userId, entry.getKey());
            courses.add(new CourseProgress(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                    passed.get(entry.getKey()) == null ? null : passed.get(entry.getKey()).passedAt(), score.status(),
                    score.total(), score.passThreshold()));
            remediation.findByCourse(userId, entry.getKey()).forEach(flag -> remediations.add(new Remediation(
                    flag.courseId(), flag.topicId(), flag.kind().name(), flag.open(), flag.createdAt())));
        }
        Map<String, Integer> byTier = new LinkedHashMap<>();
        for (var tier : com.ieltspath.learning.domain.vo.MasteryTier.values()) byTier.put(tier.name(), 0);
        profiles.findAll(userId).forEach(profile -> byTier.merge(profile.tier().name(), 1, Integer::sum));

        List<PendingReview> pending = reviews.findPending(userId).stream().map(review -> new PendingReview(
                review.reviewId(), review.knowledgePointId(), review.blocking(), null)).toList();
        List<Diagnosis> diagnoses = diagnostics.findSessionsByUser(userId).stream().map(session -> new Diagnosis(
                session.id(), session.rootKnowledgePointId(), session.status().name(),
                diagnostics.findProbes(session.id()).size())).toList();
        return new Overview(userId, List.copyOf(courses), byTier, pending, diagnoses, List.copyOf(remediations),
                insight.flagsOf(userId), insight.openWorkItemsOf(userId),
                snapshots.find(SnapshotRepository.Period.WEEK, userId, snapshotService.lastWeekStart())
                        .map(GetSnapshotReportsUseCase::view).orElse(null));
    }

    @Transactional(readOnly = true)
    public QuestionStats questionStats(int minAttempts) {
        var stats = insight.questionStats(Math.max(minAttempts, 1), QUESTION_LIMIT);
        Map<UUID, String> tags;
        try {
            tags = stats.isEmpty() ? Map.of() : content.questionDifficulties(stats.stream()
                    .map(stat -> stat.questionVersionId()).toList());
        } catch (RuntimeException unavailable) {
            // Statistics stay useful without the tag; the difficulty field is then absent.
            tags = Map.of();
        }
        Map<UUID, String> tagOf = tags;
        return new QuestionStats(stats.stream().map(stat -> stat.withDifficulty(tagOf.get(stat.questionVersionId())))
                .toList());
    }

    /** Edge and knowledge point statistics for R&D; with {@code courseId} only points of that course. */
    @Transactional(readOnly = true)
    public TreeQuality treeQuality(UUID courseId) {
        var edges = insight.edgeStats();
        var points = insight.knowledgePointStats();
        if (courseId == null) return new TreeQuality(edges, points);
        Set<UUID> inCourse = new HashSet<>();
        for (var topic : content.getTopicSequence()) {
            if (topic.course() != null && courseId.equals(topic.course().courseId())) {
                topic.knowledgePoints().forEach(point -> inCourse.add(point.id()));
            }
        }
        return new TreeQuality(edges.stream().filter(edge -> inCourse.contains(edge.from())
                        && inCourse.contains(edge.to())).toList(),
                points.stream().filter(point -> inCourse.contains(point.knowledgePointId())).toList());
    }

    /** The thresholds in force, so a screen can say why an edge or question was flagged. */
    public TreeQualitySettings thresholds() {
        return settings;
    }
}
