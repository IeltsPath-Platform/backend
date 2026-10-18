package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.service.QuestionDifficultyRules;
import com.ieltspath.learning.domain.vo.StaffRecords;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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

    /** Adds the items for everything that needs work now; returns how many were new. */
    public int sync() {
        int added = 0;
        for (var remediation : insight.openRemediation()) {
            WorkKind kind = "MID_COURSE".equals(remediation.kind()) ? WorkKind.MID_COURSE_REMEDIATION
                    : WorkKind.POST_COURSE_REMEDIATION;
            String detail = "{\"courseId\":\"" + remediation.courseId() + "\""
                    + (remediation.topicId() == null ? "" : ",\"topicId\":\"" + remediation.topicId() + "\"") + "}";
            if (insight.addWorkItem(Queue.EXAMINER, kind, remediation.userId(), remediation.flagId(), detail)) added++;
        }
        for (var flag : insight.openFlags()) {
            if (insight.addWorkItem(Queue.EXAMINER, WorkKind.LEARNER_FLAG, flag.userId(), flag.id(),
                    "{\"kind\":\"" + flag.kind() + "\"}")) added++;
        }
        for (UUID kp : insight.contentGapKnowledgePoints()) {
            if (insight.addWorkItem(Queue.CONTENT_AUTHOR, WorkKind.CONTENT_GAP, null, kp,
                    "{\"knowledgePointId\":\"" + kp + "\"}")) added++;
        }
        var stats = insight.questionStats(tree.minAttempts(), QUESTION_LIMIT);
        var tags = difficulties(stats);
        for (var stat : stats) {
            String tag = tags.get(stat.questionVersionId());
            var verdict = difficulty.judge(stat.correctRate(), tag, tree.questionLimits());
            WorkKind kind = verdict == QuestionDifficultyRules.Verdict.TOO_EASY ? WorkKind.QUESTION_TOO_EASY
                    : verdict == QuestionDifficultyRules.Verdict.TOO_HARD ? WorkKind.QUESTION_TOO_HARD : null;
            if (kind != null && insight.addWorkItem(Queue.CONTENT_AUTHOR, kind, null, stat.questionVersionId(),
                    "{\"attempts\":" + stat.attempts() + ",\"correctRate\":" + stat.correctRate()
                            + (tag == null ? "" : ",\"difficulty\":\"" + tag + "\"") + "}")) added++;
        }
        for (var edge : insight.edgeStats()) {
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

    /** The difficulty tags of the questions; empty when Content cannot be reached, so the general limits apply. */
    private java.util.Map<UUID, String> difficulties(java.util.List<StaffRecords.QuestionStat> stats) {
        if (stats.isEmpty()) return java.util.Map.of();
        try {
            return content.questionDifficulties(stats.stream().map(StaffRecords.QuestionStat::questionVersionId)
                    .toList());
        } catch (RuntimeException unavailable) {
            return java.util.Map.of();
        }
    }

    /** The stable subject of an edge's work item. */
    public static UUID edgeRef(UUID from, UUID to) {
        return UUID.nameUUIDFromBytes(("edge:" + from + ":" + to).getBytes(StandardCharsets.UTF_8));
    }
}
