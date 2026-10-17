package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.StaffInsightRepository;
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

    public WorkItemSync(StaffInsightRepository insight, TreeQualitySettings tree) {
        this.insight = insight;
        this.tree = tree;
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
        for (var stat : insight.questionStats(tree.minAttempts(), QUESTION_LIMIT)) {
            WorkKind kind = stat.correctRate() > tree.tooEasyAbove() ? WorkKind.QUESTION_TOO_EASY
                    : stat.correctRate() < tree.tooHardBelow() ? WorkKind.QUESTION_TOO_HARD : null;
            if (kind != null && insight.addWorkItem(Queue.CONTENT_AUTHOR, kind, null, stat.questionVersionId(),
                    "{\"attempts\":" + stat.attempts() + ",\"correctRate\":" + stat.correctRate() + "}")) added++;
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

    /** The stable subject of an edge's work item. */
    public static UUID edgeRef(UUID from, UUID to) {
        return UUID.nameUUIDFromBytes(("edge:" + from + ":" + to).getBytes(StandardCharsets.UTF_8));
    }
}
