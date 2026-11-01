package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import com.ieltspath.learning.domain.vo.LearningSkill;

/**
 * {@code completedLessonCount} and {@code totalLessonCount} count the lessons of the course topics;
 * {@code lastStudiedAt} is when the learner last opened one of its lessons or submitted one of its practice sets, null
 * when they never did.
 */
public record CourseResult(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                           int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                           int completedLessonCount, int totalLessonCount, Instant lastStudiedAt,
                           String coverImageUrl, String description, Map<LearningSkill, Integer> skillPrices,
                           Integer comboUnlockPoints, Set<LearningSkill> unlockedSkills, boolean comboUnlocked,
                           Integer comboPayablePoints, Set<LearningSkill> availableSkills, boolean premium) {
    public CourseResult {
        skillPrices = skillPrices == null ? Map.of() : Map.copyOf(skillPrices);
        unlockedSkills = unlockedSkills == null ? Set.of() : Set.copyOf(unlockedSkills);
        availableSkills = availableSkills == null ? Set.of() : Set.copyOf(availableSkills);
    }

    public CourseResult(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                        int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                        int completedLessonCount, int totalLessonCount, Instant lastStudiedAt,
                        String coverImageUrl, String description) {
        this(courseId, code, name, bandLevel, topicCount, passedTopicCount, recommended, testStatus, passedAt,
                completedLessonCount, totalLessonCount, lastStudiedAt, coverImageUrl, description, Map.of(), null,
                Set.of(), false, null, Set.of(), false);
    }
    public CourseResult(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                        int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                        int completedLessonCount, int totalLessonCount, Instant lastStudiedAt) {
        this(courseId, code, name, bandLevel, topicCount, passedTopicCount, recommended, testStatus, passedAt,
                completedLessonCount, totalLessonCount, lastStudiedAt, null, null, Map.of(), null,
                Set.of(), false, null, Set.of(), false);
    }
}
