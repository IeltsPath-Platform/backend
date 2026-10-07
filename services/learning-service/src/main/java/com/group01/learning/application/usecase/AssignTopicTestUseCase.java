package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient.TestPackage;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.result.TestAssignmentResult;
import com.group01.learning.application.service.PracticeProgress;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.domain.aggregate.LessonProgress;
import com.group01.learning.domain.aggregate.TopicTestAssignment;
import com.group01.learning.domain.entity.TopicProgress;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.repository.TopicTestAssignmentRepository;
import com.group01.learning.domain.service.LessonAccessGate;
import com.group01.learning.domain.service.PackageRotation;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticeStatus;
import com.group01.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gives the learner one final-test code for the current topic. A code is used once: after its attempt is consumed
 * the next assignment takes a package not used yet, or the one used longest ago when all have been used.
 */
@Service
public class AssignTopicTestUseCase {
    private final LearningContentClient content;
    private final LearnerLock lock;
    private final LearnerCurriculumRepository curricula;
    private final LessonProgressRepository lessons;
    private final TopicTestAssignmentRepository assignments;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final PracticeProgress practice;

    public AssignTopicTestUseCase(LearningContentClient content, LearnerLock lock,
                                  LearnerCurriculumRepository curricula, LessonProgressRepository lessons,
                                  TopicTestAssignmentRepository assignments,
                                  RefreshLearningTopicsUseCase refreshTopics, PracticeProgress practice) {
        this.content = content;
        this.lock = lock;
        this.curricula = curricula;
        this.lessons = lessons;
        this.assignments = assignments;
        this.refreshTopics = refreshTopics;
        this.practice = practice;
    }

    @Transactional(noRollbackFor = LearningRequestException.class)
    public TestAssignmentResult execute(UUID userId, UUID topicId) {
        lock.lock(userId);
        var curriculum = curricula.find(userId);
        var topic = curriculum.topic(topicId);
        if (topic.isEmpty() || topic.get().skills().isEmpty()) {
            refreshTopics.execute(userId);
            curriculum = curricula.find(userId);
            topic = curriculum.topic(topicId);
        }
        if (topic.isPresent() && !topic.get().hasTopicTest()) {
            throw new LearningRequestException(409, "NO_TOPIC_TEST", "This topic has no final test");
        }
        Set<LearningSkill> skills = topic.map(TopicProgress::skills).orElse(Set.of());
        var topicLessons = content.getTopicLessons(topicId);
        Map<UUID, LessonProgress> done = lessons.findByTopic(userId, topicId);
        Map<UUID, Boolean> completed = new HashMap<>();
        for (var lesson : topicLessons) {
            LessonProgress progress = done.get(lesson.lessonId());
            completed.put(lesson.lessonId(), progress != null && progress.isCompleted());
        }
        var topicPractice = practice.forTopic(userId, topicId, completed);
        var pending = LessonAccessGate.blocking(topicPractice.pendingReviews(), skills);
        if (!pending.isEmpty()) throw new LearningGateException("REVIEW_REQUIRED", pending);
        var practiceStates = topicPractice.clearances();
        practice.persistPassed(userId, practiceStates);
        List<UUID> missing = topicLessons.stream().map(LearningContentClient.LessonSummary::lessonId)
                .filter(id -> practiceStates.get(id).status() == PracticeStatus.REQUIRED).toList();
        if (!missing.isEmpty()) {
            throw new LearningRequestException(409, "PRACTICE_REQUIRED", "Complete practice for these lessons",
                    null, missing);
        }
        TopicStatus status = curriculum.status(topicId);
        if (status != TopicStatus.IN_PROGRESS || topicLessons.isEmpty()
                || completed.values().stream().anyMatch(value -> !value)) {
            throw new LearningRequestException(403, "TEST_LOCKED", "Complete the topic lessons first");
        }
        var open = assignments.findOpen(userId, topicId);
        if (open.isPresent()) return result(open.get());
        List<TestPackage> packages = content.getTopicTestPackages(topicId);
        UUID chosenId = PackageRotation.leastRecentlyUsed(packages.stream()
                        .map(p -> new PackageRotation.Candidate(p.packageId(), p.code())).toList(),
                        assignments.lastConsumedAt(userId, topicId))
                .orElseThrow(() -> new LearningRequestException(409, "TEST_UNAVAILABLE",
                        "No published test code is available"));
        TestPackage chosen = packages.stream().filter(p -> p.packageId().equals(chosenId)).findFirst().orElseThrow();
        TopicTestAssignment assignment = TopicTestAssignment.assign(UUID.randomUUID(), userId, topicId,
                chosen.packageId(), chosen.packageVersionId());
        assignments.save(assignment);
        return result(assignment);
    }

    private static TestAssignmentResult result(TopicTestAssignment assignment) {
        return new TestAssignmentResult(assignment.id(), assignment.packageId(), assignment.packageVersionId());
    }
}
