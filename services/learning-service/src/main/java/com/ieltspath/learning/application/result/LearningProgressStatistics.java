package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningProgressStatistics(Instant generatedAt, String availability, String dataFrom, String scope,
        String curriculumRevision, List<SkillProgress> skills, List<CourseProgress> courses,
        int page, int size, int totalCourses, boolean hasNext) {
    public record SkillProgress(LearningSkill skill, int completedLessons, int totalLessons, BigDecimal percent) {}
    public record CourseProgress(UUID courseId, String name, BigDecimal bandLevel, int completedLessons,
            int totalLessons, BigDecimal percent, Instant startedAt, Instant lastStudiedAt,
            String activityProvenance, ResumeTarget resumeTarget) {}
    /** The course page applies its existing review/topic/lesson gates to choose the next action. */
    public record ResumeTarget(String type, UUID courseId) {}
}
