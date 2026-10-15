package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code completedLessonCount} and {@code totalLessonCount} count the lessons of the course's topics;
 * {@code lastStudiedAt} is when the learner last opened one of its lessons or submitted one of its practice sets, null
 * when they never did.
 */
public record CourseResult(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                           int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                           int completedLessonCount, int totalLessonCount, Instant lastStudiedAt) {}
