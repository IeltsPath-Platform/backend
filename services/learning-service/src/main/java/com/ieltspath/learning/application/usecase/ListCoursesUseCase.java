package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.CourseResult;
import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.domain.aggregate.CourseProgress;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ListCoursesUseCase {
    private final LearnerLock lock;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final LearnerCurriculumRepository curricula;
    private final CourseProgressRepository courseProgress;
    private final LearnerPlacementRepository placements;
    private final LessonProgressRepository lessonProgress;

    public ListCoursesUseCase(LearnerLock lock, RefreshLearningTopicsUseCase refreshTopics,
                              LearnerCurriculumRepository curricula, CourseProgressRepository courseProgress,
                              LearnerPlacementRepository placements, LessonProgressRepository lessonProgress) {
        this.lock = lock;
        this.refreshTopics = refreshTopics;
        this.curricula = curricula;
        this.courseProgress = courseProgress;
        this.placements = placements;
        this.lessonProgress = lessonProgress;
    }

    @Transactional
    public List<CourseResult> execute(UUID userId) {
        lock.lock(userId);
        List<TopicResult> refreshed = refreshTopics.execute(userId);
        Map<UUID, TopicResult.Course> courses = new LinkedHashMap<>();
        Map<UUID, Boolean> courseHasTest = new LinkedHashMap<>();
        Map<UUID, Integer> completedLessons = new LinkedHashMap<>();
        Map<UUID, Integer> totalLessons = new LinkedHashMap<>();
        Map<UUID, Instant> lastStudied = new LinkedHashMap<>();
        Map<UUID, Instant> studiedTopics = lessonProgress.lastStudiedByTopic(userId);
        for (TopicResult topic : refreshed) {
            TopicResult.Course course = topic.course();
            if (course == null) continue;
            courses.putIfAbsent(course.courseId(), course);
            courseHasTest.putIfAbsent(course.courseId(), topic.hasCourseTest());
            completedLessons.merge(course.courseId(), topic.completedLessonCount(), Integer::sum);
            totalLessons.merge(course.courseId(), topic.totalLessonCount(), Integer::sum);
            Instant studied = studiedTopics.get(topic.topicId());
            if (studied != null) lastStudied.merge(course.courseId(), studied, (a, b) -> a.isAfter(b) ? a : b);
        }
        var curriculum = curricula.find(userId);
        var topics = curriculum.topics();
        var statuses = curriculum.statuses();
        Map<UUID, Integer> total = new LinkedHashMap<>();
        Map<UUID, Integer> passed = new LinkedHashMap<>();
        for (var topic : topics) {
            if (topic.courseId() == null || topic.sequenceOrder() == null) continue;
            total.merge(topic.courseId(), 1, Integer::sum);
            if (statuses.get(topic.topicId()) == TopicStatus.PASSED) passed.merge(topic.courseId(), 1, Integer::sum);
        }
        Map<UUID, CourseProgress> completed = courseProgress.findAll(userId);
        var placement = placements.find(userId).map(value -> value.band().value()).orElse(null);
        UUID recommended = recommended(courses, placement);
        return courses.values().stream().sorted(Comparator.comparing(TopicResult.Course::bandLevel))
                .map(course -> {
                    int topicCount = total.getOrDefault(course.courseId(), 0);
                    int passedCount = passed.getOrDefault(course.courseId(), 0);
                    CourseProgress progress = completed.get(course.courseId());
                    boolean hasTest = courseHasTest.getOrDefault(course.courseId(), false);
                    String testStatus = !hasTest ? "NONE"
                            : progress != null && progress.passedAt() != null ? "PASSED"
                            : passedCount < topicCount ? "LOCKED" : "AVAILABLE";
                    return new CourseResult(course.courseId(), course.code(), course.name(), course.bandLevel(), topicCount,
                            passedCount, course.courseId().equals(recommended), testStatus,
                            progress == null ? null : progress.passedAt(),
                            completedLessons.getOrDefault(course.courseId(), 0),
                            totalLessons.getOrDefault(course.courseId(), 0), lastStudied.get(course.courseId()));
                }).toList();
    }

    private static UUID recommended(Map<UUID, TopicResult.Course> courses, BigDecimal placement) {
        if (placement == null || courses.isEmpty()) return null;
        return courses.values().stream().sorted(Comparator.comparing(TopicResult.Course::bandLevel))
                .filter(course -> course.bandLevel().compareTo(placement) >= 0)
                .findFirst().or(() -> courses.values().stream()
                        .max(Comparator.comparing(TopicResult.Course::bandLevel)))
                .map(TopicResult.Course::courseId).orElse(null);
    }

}
