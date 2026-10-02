package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.TestPackage;
import com.group01.learning.application.result.TestAssignmentResult;
import com.group01.learning.domain.aggregate.LessonProgress;
import com.group01.learning.domain.aggregate.TopicTestAssignment;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.repository.TopicTestAssignmentRepository;
import com.group01.learning.domain.service.PackageRotation;
import com.group01.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    private final ReviewItemRepository reviews;
    private final TopicTestAssignmentRepository assignments;

    public AssignTopicTestUseCase(LearningContentClient content, LearnerLock lock,
                                  LearnerCurriculumRepository curricula, LessonProgressRepository lessons,
                                  ReviewItemRepository reviews, TopicTestAssignmentRepository assignments) {
        this.content = content;
        this.lock = lock;
        this.curricula = curricula;
        this.lessons = lessons;
        this.reviews = reviews;
        this.assignments = assignments;
    }

    @Transactional
    public TestAssignmentResult execute(UUID userId, UUID topicId) {
        lock.lock(userId);
        var pending = reviews.findPending(userId);
        if (!pending.isEmpty()) throw new LearningGateException("REVIEW_REQUIRED", pending);
        TopicStatus status = curricula.find(userId).status(topicId);
        if (status != TopicStatus.IN_PROGRESS || !allLessonsCompleted(userId, topicId)) {
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

    private boolean allLessonsCompleted(UUID userId, UUID topicId) {
        var topicLessons = content.getTopicLessons(topicId);
        Map<UUID, LessonProgress> done = lessons.findByTopic(userId, topicId);
        return !topicLessons.isEmpty() && topicLessons.stream().allMatch(lesson -> {
            LessonProgress progress = done.get(lesson.lessonId());
            return progress != null && progress.isCompleted();
        });
    }

    private static TestAssignmentResult result(TopicTestAssignment assignment) {
        return new TestAssignmentResult(assignment.id(), assignment.packageId(), assignment.packageVersionId());
    }
}
