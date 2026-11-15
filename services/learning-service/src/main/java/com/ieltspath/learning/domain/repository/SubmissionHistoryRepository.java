package com.ieltspath.learning.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The learner's own past submissions, newest first, one page at a time. A page skips {@code offset} rows and returns at most
 * {@code limit}; the caller asks for one row more than it shows to learn whether another page exists.
 */
public interface SubmissionHistoryRepository {
    /** What a learner submitted inside a course: Practice sets, lesson essays and speaking, topic, course and milestone tests. */
    List<Entry> findCourse(UUID userId, int limit, int offset);

    /** Free practice essays ({@code skill} WRITING) or free practice speaking ({@code skill} SPEAKING). */
    List<Entry> findFree(UUID userId, String skill, int limit, int offset);

    /** Submitted free practice sets on one knowledge point (kind {@code KP_PRACTICE}); names are not read here. */
    List<Entry> findKnowledgePractice(UUID userId, int limit, int offset);

    /** When the learner's own essay was submitted; empty when it is not theirs or does not exist. */
    Optional<Instant> essaySubmittedAt(UUID userId, UUID submissionId);

    /** For each of {@code knowledgePointIds} the learner practised freely since {@code since}: how often and the latest set. */
    List<PracticeTally> tallyKnowledgePractice(UUID userId, Collection<UUID> knowledgePointIds, Instant since);

    /**
     * One submission with enough of it to recognise it in a list and to open it. {@code kind} is {@code PRACTICE},
     * {@code WRITING}, {@code SPEAKING}, {@code TOPIC_TEST}, {@code COURSE_TEST}, {@code PRACTICE_TEST} or {@code KP_PRACTICE};
     * a field that does not apply to the kind is null. {@code id} is the id of the record: the attempt, the essay, the speaking
     * submission or the test assignment; {@code attemptId} is the attempt a result opens at. The knowledge point fields belong
     * to {@code KP_PRACTICE}; its code and name are filled from the catalog after reading, when the catalog answers.
     */
    record Entry(String kind, UUID id, Instant submittedAt, String skill, String task, String stem, BigDecimal band,
                 Double percent, Integer correctCount, Integer totalCount, Boolean passed, String status, UUID lessonId,
                 UUID topicId, UUID courseId, UUID attemptId, UUID packageVersionId, Integer revisionNo,
                 UUID knowledgePointId, String knowledgePointCode, String knowledgePointName) {

        /** A submission that is not about one knowledge point. */
        public Entry(String kind, UUID id, Instant submittedAt, String skill, String task, String stem, BigDecimal band,
                     Double percent, Integer correctCount, Integer totalCount, Boolean passed, String status, UUID lessonId,
                     UUID topicId, UUID courseId, UUID attemptId, UUID packageVersionId, Integer revisionNo) {
            this(kind, id, submittedAt, skill, task, stem, band, percent, correctCount, totalCount, passed, status, lessonId,
                    topicId, courseId, attemptId, packageVersionId, revisionNo, null, null, null);
        }

        public Entry withKnowledgePointName(String code, String name) {
            return new Entry(kind, id, submittedAt, skill, task, stem, band, percent, correctCount, totalCount, passed, status,
                    lessonId, topicId, courseId, attemptId, packageVersionId, revisionNo, knowledgePointId, code, name);
        }
    }

    /** The free practice of one knowledge point: how many sets were submitted, and the latest of them. */
    record PracticeTally(UUID knowledgePointId, int attempts, UUID lastAttemptId, Instant lastSubmittedAt,
                         Integer lastCorrectCount, Integer lastTotalCount, Boolean lastPassed) {}
}
