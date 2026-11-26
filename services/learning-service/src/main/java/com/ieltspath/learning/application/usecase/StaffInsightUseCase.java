package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.StaffInsightResults.QuestionStats;
import com.ieltspath.learning.application.result.StaffInsightResults.TreeQuality;
import com.ieltspath.learning.application.service.TreeQualitySettings;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read-only statistics R&D uses to improve content. */
@Service
public class StaffInsightUseCase {
    private static final int QUESTION_LIMIT = 200;

    private final StaffInsightRepository insight;
    private final LearningContentClient content;
    private final TreeQualitySettings settings;

    public StaffInsightUseCase(StaffInsightRepository insight, LearningContentClient content,
                               TreeQualitySettings settings) {
        this.insight = insight;
        this.content = content;
        this.settings = settings;
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
