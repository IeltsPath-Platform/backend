package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseResult(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                           int passedTopicCount, boolean recommended, String testStatus, Instant passedAt) {}
