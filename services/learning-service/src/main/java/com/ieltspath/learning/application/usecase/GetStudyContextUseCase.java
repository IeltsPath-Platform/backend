package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.StudyContextResult;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class GetStudyContextUseCase {
    private final LearnerLock lock;
    private final LessonAccess lessons;
    private final PracticeAccess practice;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final LearningContentClient content;

    public GetStudyContextUseCase(LearnerLock lock, LessonAccess lessons, PracticeAccess practice,
            PracticeAttemptRepository attempts, ReviewItemRepository reviews, LearningContentClient content) {
        this.lock = lock;
        this.lessons = lessons;
        this.practice = practice;
        this.attempts = attempts;
        this.reviews = reviews;
        this.content = content;
    }

    @Transactional
    public StudyContextResult execute(UUID userId, String sourceType, UUID sourceId) {
        if (!java.util.Set.of("LESSON", "PRACTICE", "REVIEW").contains(sourceType))
            throw new LearningRequestException(400, "INVALID_SOURCE", "Unsupported study source");
        lock.lock(userId);
        LearningContentClient.Lesson lesson;
        switch (sourceType) {
            case "LESSON" -> lesson = lessons.authorizeToRead(userId, sourceId).lesson();
            case "PRACTICE" -> {
                var attempt = attempts.findOwned(userId, sourceId).orElseThrow(GetStudyContextUseCase::notFound);
                if (attempt.submitted()) throw inactive();
                lesson = practice.require(userId, attempt.lessonId());
            }
            case "REVIEW" -> {
                var review = reviews.findOwned(userId, sourceId).orElseThrow(GetStudyContextUseCase::notFound);
                if (review.status() != ReviewStatus.PENDING) throw inactive();
                // Reviews are precisely what can block lesson access; use their own owner/state gate.
                lesson = content.getLesson(review.lessonId());
            }
            default -> throw new IllegalStateException("Validated source type is unsupported");
        }
        UUID courseId = lessons.topic(userId, lesson.topicId()).courseId();
        if (courseId == null) throw notFound();
        return new StudyContextResult(sourceType, sourceId, courseId);
    }

    private static LearningRequestException notFound() {
        return new LearningRequestException(404, "NOT_FOUND", "Study source was not found");
    }
    private static LearningRequestException inactive() {
        return new LearningRequestException(409, "STUDY_SOURCE_INACTIVE", "Study source is no longer active");
    }
}
