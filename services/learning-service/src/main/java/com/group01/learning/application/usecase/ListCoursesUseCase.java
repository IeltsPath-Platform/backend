package com.group01.learning.application.usecase;

import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.result.CourseResult;
import com.group01.learning.application.result.TopicResult;
import com.group01.learning.domain.aggregate.CourseProgress;
import com.group01.learning.domain.repository.CourseProgressRepository;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.repository.LearnerPlacementRepository;
import com.group01.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public ListCoursesUseCase(LearnerLock lock, RefreshLearningTopicsUseCase refreshTopics,
                              LearnerCurriculumRepository curricula, CourseProgressRepository courseProgress,
                              LearnerPlacementRepository placements) {
        this.lock = lock;
        this.refreshTopics = refreshTopics;
        this.curricula = curricula;
        this.courseProgress = courseProgress;
        this.placements = placements;
    }

    @Transactional
    public List<CourseResult> execute(UUID userId) {
        lock.lock(userId);
        List<TopicResult> refreshed = refreshTopics.execute(userId);
        Map<UUID, TopicResult.Course> courses = new LinkedHashMap<>();
        Map<UUID, Boolean> courseHasTest = new LinkedHashMap<>();
        for (TopicResult topic : refreshed) {
            TopicResult.Course course = topic.course();
            if (course == null) continue;
            courses.putIfAbsent(course.courseId(), course);
            courseHasTest.putIfAbsent(course.courseId(), topic.hasCourseTest());
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
                            progress == null ? null : progress.passedAt());
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
