package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.repository.StaffInsightRepository.OpenRemediation;
import com.ieltspath.learning.domain.service.QuestionDifficultyRules;
import com.ieltspath.learning.domain.vo.StaffRecords.EdgeStat;
import com.ieltspath.learning.domain.vo.StaffRecords.LearnerFlag;
import com.ieltspath.learning.domain.vo.StaffRecords.QuestionStat;
import com.ieltspath.learning.domain.vo.StaffRecords;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns the sources of staff work into work items: open remediation and learner flags for Examiners; knowledge points
 * without sets, questions too easy or too hard, and prerequisite edges that do not explain failures for R&D. A source
 * becomes at most one item, so syncing again adds only what is new and never reopens finished work.
 */
@Component
public class WorkItemSync {
    private static final int QUESTION_LIMIT = 500;

    private final StaffInsightRepository insight;
    private final TreeQualitySettings tree;
    private final LearningContentClient content;
    private final QuestionDifficultyRules difficulty = new QuestionDifficultyRules();

    public WorkItemSync(StaffInsightRepository insight, TreeQualitySettings tree, LearningContentClient content) {
        this.insight = insight;
        this.tree = tree;
        this.content = content;
    }

    /** Loads all source data and Content metadata before the caller opens a write transaction. */
    public Loaded loadFromContent() {
        var remediations = insight.openRemediation();
        var flags = insight.openFlags();
        var contentGaps = insight.contentGapKnowledgePoints();
        var stats = insight.questionStats(tree.minAttempts(), QUESTION_LIMIT);
        var tags = difficulties(stats);
        var edges = insight.edgeStats();
        return new Loaded(remediations, flags, contentGaps, stats, tags, edges);
    }

    /** Applies a previously loaded snapshot; duplicate rows are rejected by the repository's unique key. */
    public int apply(Loaded loaded) {
        int added = 0;
        for (var remediation : loaded.remediations()) {
            WorkKind kind = "MID_COURSE".equals(remediation.kind()) ? WorkKind.MID_COURSE_REMEDIATION
                    : WorkKind.POST_COURSE_REMEDIATION;
            String detail = "{\"courseId\":\"" + remediation.courseId() + "\""
                    + (remediation.topicId() == null ? "" : ",\"topicId\":\"" + remediation.topicId() + "\"") + "}";
            if (insight.addWorkItem(Queue.EXAMINER, kind, remediation.userId(), remediation.flagId(), detail)) added++;
        }
        for (var flag : loaded.flags()) {
            if (insight.addWorkItem(Queue.EXAMINER, WorkKind.LEARNER_FLAG, flag.userId(), flag.id(),
                    "{\"kind\":\"" + flag.kind() + "\"}")) added++;
        }
        for (UUID kp : loaded.contentGaps()) {
            if (insight.addWorkItem(Queue.CONTENT_AUTHOR, WorkKind.CONTENT_GAP, null, kp,
                    "{\"knowledgePointId\":\"" + kp + "\"}")) added++;
        }
        for (var stat : loaded.questionStats()) {
            String tag = loaded.difficulties().get(stat.questionVersionId());
            var verdict = difficulty.judge(stat.correctRate(), tag, tree.questionLimits());
            WorkKind kind = verdict == QuestionDifficultyRules.Verdict.TOO_EASY ? WorkKind.QUESTION_TOO_EASY
                    : verdict == QuestionDifficultyRules.Verdict.TOO_HARD ? WorkKind.QUESTION_TOO_HARD : null;
            if (kind != null && insight.addWorkItem(Queue.CONTENT_AUTHOR, kind, null, stat.questionVersionId(),
                    "{\"attempts\":" + stat.attempts() + ",\"correctRate\":" + stat.correctRate()
                            + (tag == null ? "" : ",\"difficulty\":\"" + tag + "\"") + "}")) added++;
        }
        for (var edge : loaded.edges()) {
            if (edge.probes() >= tree.minSessions() && (edge.passRate() >= tree.passRateAtLeast()
                    || edge.rootRate() < tree.rootRateBelow())
                    && insight.addWorkItem(Queue.CONTENT_AUTHOR, WorkKind.EDGE_SUSPECT, null, edgeRef(edge.from(),
                    edge.to()), "{\"from\":\"" + edge.from() + "\",\"to\":\"" + edge.to() + "\",\"probes\":"
                    + edge.probes() + ",\"passRate\":" + edge.passRate() + ",\"rootRate\":" + edge.rootRate() + "}")) {
                added++;
            }
        }
        return added;
    }

    /** Adds the items for everything that needs work now; returns how many were new. */
    public int sync() {
        return apply(loadFromContent());
    }

    public record Loaded(List<OpenRemediation> remediations,
                         List<LearnerFlag> flags,
                         List<UUID> contentGaps,
                         List<QuestionStat> questionStats,
                         Map<UUID, String> difficulties,
                         List<EdgeStat> edges) {
        public Loaded {
            remediations = java.util.List.copyOf(remediations);
            flags = java.util.List.copyOf(flags);
            contentGaps = java.util.List.copyOf(contentGaps);
            questionStats = java.util.List.copyOf(questionStats);
            difficulties = Collections.unmodifiableMap(new HashMap<>(difficulties));
            edges = java.util.List.copyOf(edges);
        }
    }

    /** The difficulty tags of the questions; empty when Content cannot be reached, so the general limits apply. */
    private Map<UUID, String> difficulties(List<QuestionStat> stats) {
        if (stats.isEmpty()) return Map.of();
        try {
            return content.questionDifficulties(stats.stream().map(QuestionStat::questionVersionId)
                    .toList());
        } catch (RuntimeException unavailable) {
            return Map.of();
        }
    }

    /** The stable subject of an edge's work item. */
    public static UUID edgeRef(UUID from, UUID to) {
        return UUID.nameUUIDFromBytes(("edge:" + from + ":" + to).getBytes(StandardCharsets.UTF_8));
    }
}
