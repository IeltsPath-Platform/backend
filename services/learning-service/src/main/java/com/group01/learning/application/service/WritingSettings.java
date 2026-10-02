package com.group01.learning.application.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code learning.writing.*}. Each LLM call may take {@code gradingTimeoutSeconds}; two calls plus the debit must
 * finish within the 60 s life of the forwarded internal JWT, hence the 25 s ceiling. The daily limit counts LLM
 * gradings per learner and calendar day in {@code quotaTimezone}.
 */
@Validated
@ConfigurationProperties("learning.writing")
public record WritingSettings(
        @DefaultValue("3") @Min(1) int pointCost,
        @DefaultValue("50") @Min(1) int minWords,
        @DefaultValue("1000") @Min(1) int maxWords,
        @DefaultValue("10000") @Min(1) int maxChars,
        @DefaultValue("20") @Min(1) @Max(25) int gradingTimeoutSeconds,
        @DefaultValue("120") @Min(1) int staleGradingSeconds,
        @DefaultValue("10") @Min(1) int dailyGradingLimit,
        @DefaultValue("Asia/Ho_Chi_Minh") @NotBlank String quotaTimezone) {

    @AssertTrue(message = "minWords must be below maxWords")
    public boolean isWordRangeValid() {
        return minWords < maxWords;
    }
}
