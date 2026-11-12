package com.ieltspath.learning.application.result;

import java.time.Instant;

public record LearningOverviewStatistics(Instant generatedAt, Instant dataFrom, long totalStudySeconds,
                                         long completedLessons) {}
