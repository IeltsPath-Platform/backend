package com.ieltspath.learning.application.service;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Failure threshold for turning a blocking review into an optional review. The third failed set sends the learner back
 * to the theory, so the limit cannot be lower than 4.
 */
@Validated
@ConfigurationProperties("learning.review")
public record ReviewSettings(@DefaultValue("4") @Min(4) int unblockAfterFailedSets) {
    public static ReviewSettings defaults() { return new ReviewSettings(4); }
}
