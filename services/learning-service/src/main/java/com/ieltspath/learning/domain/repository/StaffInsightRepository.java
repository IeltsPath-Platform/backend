package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.service.AnomalyRules;
import com.ieltspath.learning.domain.vo.StaffRecords.EdgeStat;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagKind;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagStatus;
import com.ieltspath.learning.domain.vo.StaffRecords.KnowledgePointStat;
import com.ieltspath.learning.domain.vo.StaffRecords.LearnerFlag;
import com.ieltspath.learning.domain.vo.StaffRecords.QuestionStat;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads and writes behind the staff area: flags, work items and the statistics over learner data. */
public interface StaffInsightRepository {
    // --- signals for the anomaly rules
    List<AnomalyRules.Sample> samplesSince(UUID userId, Instant since);

    long eventsSince(UUID userId, Instant since);

    /** Learners other than {@code userId} that sent an essay with the same normalised hash. */
    long otherLearnersWithEssayHash(UUID userId, String essayHash);

    List<String> recentEssayHashes(UUID userId, Instant since);

    // --- flags
    /** False when the learner already has a flag of this kind raised since {@code notBefore}. */
    boolean insertFlag(UUID userId, FlagKind kind, String evidenceJson, Instant notBefore);

    /**
     * Opens an INACTIVE flag for every learner who has studied, is still on a course that is not passed, and whose last
     * answer is at or before {@code cutoff}, unless the learner already has one open. Returns how many were opened.
     */
    int openInactivityFlags(Instant cutoff, Instant now);

    /** Closes the learner's open INACTIVE flags because they answered again; returns how many closed. */
    int dismissInactivityFlags(java.util.UUID userId);

    Optional<LearnerFlag> findFlag(UUID id);

    boolean reviewFlag(UUID id, FlagStatus status, UUID reviewer, String note);

    List<LearnerFlag> flagsOf(UUID userId);

    List<LearnerFlag> openFlags();

    // --- work items
    /** Adds the item unless one with the same kind and subject exists; true when it was added. */
    boolean addWorkItem(Queue queue, WorkKind kind, UUID subjectUserId, UUID subjectRef, String detailJson);

    Optional<WorkItem> findWorkItem(UUID id);

    List<WorkItem> workItems(Collection<Queue> queues, WorkStatus status);

    List<WorkItem> openWorkItemsOf(UUID userId);

    List<WorkItem> visibleNotes(UUID userId);

    void updateWorkItem(UUID id, WorkStatus status, UUID assignee, String action, String result, String note,
                        boolean noteVisibleToLearner);

    // --- sources the work items are made from
    /** Open remediation flags: (flag id, learner, course, topic or null, kind). */
    List<OpenRemediation> openRemediation();

    List<UUID> contentGapKnowledgePoints();

    List<QuestionStat> questionStats(int minAttempts, int limit);

    List<EdgeStat> edgeStats();

    List<KnowledgePointStat> knowledgePointStats();

    record OpenRemediation(UUID flagId, UUID userId, UUID courseId, UUID topicId, String kind) {}
}
