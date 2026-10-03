package com.group01.learning.application.usecase;

import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.result.LessonPracticeSetsResult;
import com.group01.learning.application.service.PracticeAccess;
import com.group01.learning.application.service.PracticeProgress;
import com.group01.learning.domain.repository.PracticeAttemptRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.vo.PracticeStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GetLessonPracticeSetsUseCase {
    private final LearningContentClient content;
    private final PracticeAccess access;
    private final PracticeProgress progress;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;

    public GetLessonPracticeSetsUseCase(LearningContentClient content, PracticeAccess access,
                                        PracticeProgress progress, PracticeAttemptRepository attempts,
                                        ReviewItemRepository reviews) {
        this.content = content;
        this.access = access;
        this.progress = progress;
        this.attempts = attempts;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public LessonPracticeSetsResult execute(UUID userId, UUID lessonId) {
        var lesson = content.getLesson(lessonId);
        var sets = content.lessonPracticeSets(lessonId);
        boolean completed = access.completed(userId, lessonId);
        boolean blocked = reviews.findPending(userId).stream()
                .anyMatch(review -> review.skill() == null || review.skill() == lesson.skill());
        var clearance = progress.forLesson(userId, lessonId, completed, sets);
        var summaries = attempts.summarizeForLesson(userId, lessonId,
                sets.stream().map(LearningContentClient.LessonPracticeSet::packageId).toList());
        Set<UUID> revealed = attempts.revealedPackageIds(userId,
                sets.stream().map(LearningContentClient.LessonPracticeSet::packageId).toList());
        Map<UUID, PracticeAttemptRepository.PackageSummary> byPackage = new HashMap<>();
        for (var summary : summaries) byPackage.put(summary.packageId(), summary);
        List<LessonPracticeSetsResult.Item> items = new ArrayList<>();
        for (var set : sets) {
            var summary = byPackage.get(set.packageId());
            String status = !completed || blocked ? "LOCKED"
                    : summary != null && summary.open() ? "IN_PROGRESS"
                    : summary != null && summary.passed() ? "PASSED"
                    : summary != null && summary.attempted() ? "ATTEMPTED" : "AVAILABLE";
            items.add(new LessonPracticeSetsResult.Item(set.packageId(), set.code(), set.title(),
                    set.questionCount(), set.requiredFeatureKey() == null ? "FREE" : "PREMIUM", status,
                    summary == null ? null : summary.bestPercent(),
                    summary == null ? null : summary.lastAttemptId(), revealed.contains(set.packageId())));
        }
        return new LessonPracticeSetsResult(lessonId, lesson.skill(), completed, clearance.status(),
                clearance.reason(), List.copyOf(items));
    }
}
