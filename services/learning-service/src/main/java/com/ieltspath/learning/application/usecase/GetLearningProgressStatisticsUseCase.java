package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningProgressReader;
import com.ieltspath.learning.application.result.LearningProgressStatistics;
import com.ieltspath.learning.application.result.LearningProgressStatistics.*;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class GetLearningProgressStatisticsUseCase {
    private final LearningProgressReader reader;
    private final LearningContentClient content;
    public GetLearningProgressStatisticsUseCase(LearningProgressReader reader, LearningContentClient content) {
        this.reader = reader;
        this.content = content;
    }

    @Transactional(readOnly = true)
    public LearningProgressStatistics execute(UUID userId, UUID selectedCourse, int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 20)
            throw new LearningRequestException(400, "INVALID_PAGE", "Page must be 0..10000 and size 1..20");
        var activities = reader.startedCourses(userId);
        if (activities.size() > 100) throw unavailable();
        if (selectedCourse != null) {
            activities = activities.stream().filter(a -> a.courseId().equals(selectedCourse)).toList();
            if (activities.isEmpty()) throw new LearningRequestException(404, "NOT_FOUND", "Started course was not found");
        }
        var metadata = activities.isEmpty() ? new LearningContentClient.ProgressMetadata("empty", true, List.of())
                : content.progressMetadata(activities.stream().map(LearningProgressReader.CourseActivity::courseId).toList());
        if (metadata == null || !metadata.complete() || metadata.lessons() == null || metadata.lessons().size() > 10000)
            throw unavailable();
        Set<UUID> scope = new HashSet<>();
        activities.forEach(a -> scope.add(a.courseId()));
        // The map prevents repeated metadata rows from double counting multi-skill lessons.
        Map<UUID, LearningContentClient.ProgressLesson> lessons = new LinkedHashMap<>();
        metadata.lessons().stream().filter(l -> scope.contains(l.courseId())).forEach(l -> lessons.put(l.lessonId(), l));
        Set<UUID> completed = reader.completedLessons(userId, List.copyOf(lessons.keySet()));
        Map<LearningSkill, int[]> skillCounts = new EnumMap<>(LearningSkill.class);
        for (LearningSkill skill : LearningSkill.values()) skillCounts.put(skill, new int[2]);
        Map<UUID, int[]> courseCounts = new HashMap<>();
        Map<UUID, LearningContentClient.ProgressLesson> courseMetadata = new HashMap<>();
        for (var lesson : lessons.values()) {
            int done = completed.contains(lesson.lessonId()) ? 1 : 0;
            int[] count = courseCounts.computeIfAbsent(lesson.courseId(), ignored -> new int[2]);
            count[0] += done;
            count[1]++;
            courseMetadata.putIfAbsent(lesson.courseId(), lesson);
            for (var skill : lesson.skills()) {
                int[] skillCount = skillCounts.get(skill);
                skillCount[0] += done;
                skillCount[1]++;
            }
        }
        List<SkillProgress> skills = skillCounts.entrySet().stream().map(entry ->
                new SkillProgress(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                        percent(entry.getValue()[0], entry.getValue()[1]))).toList();
        List<CourseProgress> courses = new ArrayList<>();
        for (var activity : activities) {
            var details = courseMetadata.get(activity.courseId());
            if (details == null) continue; // No currently published denominator in this course.
            int[] count = courseCounts.get(activity.courseId());
            courses.add(new CourseProgress(activity.courseId(), details.courseName(), details.bandLevel(),
                    count[0], count[1], percent(count[0], count[1]), activity.startedAt(), activity.lastStudiedAt(),
                    activity.startedAt() == null ? "LEGACY_UNKNOWN" : "RECORDED",
                    new ResumeTarget("COURSE", activity.courseId())));
        }
        int from = Math.min(page * size, courses.size());
        int to = Math.min(from + size, courses.size());
        return new LearningProgressStatistics(Instant.now(), lessons.isEmpty() ? "EMPTY" : "AVAILABLE",
                "CURRENT_PUBLISHED_CURRICULUM", selectedCourse == null ? "STARTED_COURSES" : "COURSE",
                metadata.revision(), skills, List.copyOf(courses.subList(from, to)), page, size,
                courses.size(), to < courses.size());
    }

    private static BigDecimal percent(int completed, int total) {
        return total == 0 ? null : BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private static LearningRequestException unavailable() {
        return new LearningRequestException(503, "PROGRESS_UNAVAILABLE", "Complete curriculum metadata is unavailable");
    }
}
