package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The score configuration that applies to a course. {@code courseId} is null for the default; {@code inherited} is true
 * when the course has no configuration of its own and follows the default.
 */
public record ScoreConfigResult(UUID courseId, boolean inherited, BigDecimal processWeight, BigDecimal midWeight,
                                BigDecimal finalWeight, BigDecimal practiceShare, BigDecimal practiceTestShare,
                                BigDecimal passThreshold, BigDecimal midRemediationBelow) {}
