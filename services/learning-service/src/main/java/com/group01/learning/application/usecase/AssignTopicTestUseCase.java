package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.TestPackage;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.port.ReviewStore;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.service.TopicStatusDeriver;
import com.group01.learning.domain.vo.LessonProgress;
import com.group01.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
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
    private final LearningProgressStore progress;
    private final ReviewStore reviews;
    private final TopicStatusDeriver topicStatuses = new TopicStatusDeriver();

    public AssignTopicTestUseCase(LearningContentClient content, LearningProgressStore progress, ReviewStore reviews) {
        this.content = content;
        this.progress = progress;
        this.reviews = reviews;
    }

    @Transactional
    public ReviewStore.TestAssignment assign(UUID userId, UUID topicId) {
        progress.lockUser(userId);
        var pending = progress.findPendingReviews(userId);
        if (!pending.isEmpty()) throw new LearningGateException("REVIEW_REQUIRED", pending);
        TopicStatus status = topicStatuses.derive(progress.findTopics(userId)).getOrDefault(topicId, TopicStatus.LOCKED);
        if (status != TopicStatus.IN_PROGRESS || !allLessonsCompleted(userId, topicId)) {
            throw new LearningRequestException(403, "TEST_LOCKED", "Complete the topic lessons first");
        }
        var open = reviews.findOpenAssignment(userId, topicId);
        if (open.isPresent()) return open.get();

        List<TestPackage> packages = content.getTopicTestPackages(topicId);
        if (packages.isEmpty()) {
            throw new LearningRequestException(409, "TEST_UNAVAILABLE", "No published test code is available");
        }
        Map<UUID, Instant> lastConsumed = reviews.lastConsumedAt(userId, topicId);
        TestPackage chosen = packages.stream()
                .min(Comparator.comparing((TestPackage p) -> lastConsumed.getOrDefault(p.packageId(), Instant.MIN))
                        .thenComparing(TestPackage::code).thenComparing(p -> p.packageId().toString()))
                .orElseThrow();
        return reviews.insertAssignment(userId, topicId, chosen.packageId(), chosen.packageVersionId());
    }

    private boolean allLessonsCompleted(UUID userId, UUID topicId) {
        var lessons = content.getTopicLessons(topicId);
        Map<UUID, LessonProgress> done = progress.findLessons(userId, topicId);
        return !lessons.isEmpty() && lessons.stream().allMatch(lesson -> {
            LessonProgress lessonProgress = done.get(lesson.lessonId());
            return lessonProgress != null && lessonProgress.completedAt() != null;
        });
    }
}
