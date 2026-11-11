package com.ieltspath.learning.application.service;

import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code learning.writing.*}. Grading uses one LLM request for bands and knowledge-point judgments.
 * The daily limit counts essays per learner and day.
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
        @DefaultValue("Asia/Ho_Chi_Minh") @NotBlank String quotaTimezone,
        @DefaultValue("2") @Min(1) int progressFixedAfter,
        // Band caps stay off until an examiner has set the thresholds; a null cap switches its rule off.
        @DefaultValue("false") boolean bandCapsEnabled,
        BigDecimal tooShortBandCap,
        BigDecimal offTopicBandCap,
        BigDecimal noOverviewBandCap,
        BigDecimal partialAnswerBandCap,
        BigDecimal noPositionBandCap) {

    /** The settings the grader uses when the configuration is not given: caps off. */
    public static WritingSettings defaults() {
        return new WritingSettings(3, 50, 1000, 10000, 20, 120, 10, "Asia/Ho_Chi_Minh", 2, false,
                null, null, null, null, null);
    }

    @AssertTrue(message = "minWords must be below maxWords")
    public boolean isWordRangeValid() {
        return minWords < maxWords;
    }
}
