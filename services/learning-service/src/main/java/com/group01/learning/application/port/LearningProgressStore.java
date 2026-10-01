package com.group01.learning.application.port;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.domain.service.ReviewRule.ReviewCandidate;
import com.group01.learning.domain.vo.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LearningProgressStore {
    void lockUser(UUID userId);
    void refreshCurriculum(UUID userId, List<TopicOrder> topics, List<KnowledgePointCatalogEntry> knowledgePoints);
    List<TopicProgress> findTopics(UUID userId);
    Map<UUID, Integer> completedLessonCounts(UUID userId);
    Map<UUID, LessonProgress> findLessons(UUID userId, UUID topicId);
    List<LessonProgress> findCompletedLessons(UUID userId);
    List<PendingReview> findPendingReviews(UUID userId);
    void refreshLesson(UUID userId, UUID lessonId, UUID topicId, int sortOrder, List<UUID> knowledgePointIds);
    Optional<StoredSubmission> findSubmission(UUID requestId);
    boolean hasSubmission(UUID userId, UUID lessonId, UUID blockId);
    List<SubmissionResult> findFirstSubmissions(UUID userId, UUID lessonId);
    boolean passBlock(UUID userId, UUID lessonId, UUID blockId);
    boolean completeLesson(UUID userId, UUID lessonId);
    void appendEvidence(UUID userId, List<NewEvidence> evidence);
    boolean saveSubmission(UUID userId, UUID lessonId, UUID blockId, SubmitExerciseCommand command,
                           SubmissionResult response);
    List<MasteryHistory> findMastery(UUID userId);
    void insertReviews(UUID userId, List<ReviewCandidate> reviews);

    record TopicOrder(UUID topicId, int sequenceOrder) {}
    record StoredSubmission(UUID userId, UUID lessonId, UUID blockId, SubmissionResult response) {}
    record NewEvidence(UUID knowledgePointId, boolean correct, String source, UUID sourceReferenceId) {}
    record MasteryHistory(UUID knowledgePointId, UUID topicId, boolean hasPracticeSet,
                          List<Boolean> correctness, long evidenceCount) {}
}
