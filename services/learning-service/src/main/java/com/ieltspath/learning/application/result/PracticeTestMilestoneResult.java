package com.ieltspath.learning.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record PracticeTestMilestoneResult(UUID packageId, UUID packageVersionId, String code, String title,
                                          int afterTopicSequence, String status, BigDecimal percent) {}
