package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.CourseResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import com.ieltspath.learning.domain.vo.LearningSkill;

public record CourseResponse(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                            int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                            int completedLessonCount, int totalLessonCount, Instant lastStudiedAt,
                            String coverImageUrl, String description, Map<LearningSkill, Integer> skillPrices,
                            Integer comboUnlockPoints, Set<LearningSkill> unlockedSkills, boolean comboUnlocked,
                            Integer comboPayablePoints, Set<LearningSkill> availableSkills, boolean premium) {
    public CourseResponse {
        skillPrices = skillPrices == null ? Map.of() : Map.copyOf(skillPrices);
        unlockedSkills = unlockedSkills == null ? Set.of() : Set.copyOf(unlockedSkills);
        availableSkills = availableSkills == null ? Set.of() : Set.copyOf(availableSkills);
    }

    public CourseResponse(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                          int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                          int completedLessonCount, int totalLessonCount, Instant lastStudiedAt,
                          String coverImageUrl, String description) {
        this(courseId, code, name, bandLevel, topicCount, passedTopicCount, recommended, testStatus, passedAt,
                completedLessonCount, totalLessonCount, lastStudiedAt, coverImageUrl, description, Map.of(), null,
                Set.of(), false, null, Set.of(), false);
    }

    public CourseResponse(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                          int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                          int completedLessonCount, int totalLessonCount, Instant lastStudiedAt) {
        this(courseId, code, name, bandLevel, topicCount, passedTopicCount, recommended, testStatus, passedAt,
                completedLessonCount, totalLessonCount, lastStudiedAt, null, null, Map.of(), null,
                Set.of(), false, null, Set.of(), false);
    }

    public static CourseResponse from(CourseResult result) {
        return new CourseResponse(result.courseId(), result.code(), result.name(), result.bandLevel(),
                result.topicCount(), result.passedTopicCount(), result.recommended(), result.testStatus(),
                result.passedAt(), result.completedLessonCount(), result.totalLessonCount(), result.lastStudiedAt(),
                result.coverImageUrl(), result.description(), result.skillPrices(), result.comboUnlockPoints(),
                result.unlockedSkills(), result.comboUnlocked(), result.comboPayablePoints(),
                result.availableSkills(), result.premium());
    }
}
