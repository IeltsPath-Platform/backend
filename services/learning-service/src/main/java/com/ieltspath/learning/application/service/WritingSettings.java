package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.service.WritingBandCaps;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
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
        BigDecimal noPositionBandCap,
        @DefaultValue("30") @Min(1) @Max(50) int maxCorrections,
        // Prompt template used when no evaluation run has activated a version for the task; an activation wins.
        @DefaultValue("v2") @Pattern(regexp = "v1|v2|v3") String defaultTemplate,
        // Caps for essays whose errors are in most sentences; they apply only while bandCapsEnabled is true.
        @DefaultValue Density density) {

    @ConstructorBinding
    public WritingSettings {
        density = density == null ? Density.OFF : density;
    }

    /**
     * Shares of the essay at which a density rule fires, and the cap each sets. A null cap switches its rule off. The
     * shares are fractions: 0.8 means errors in at least four sentences in five.
     */
    public record Density(
            @DefaultValue("0.8") BigDecimal grammarShare,
            @DefaultValue("0.5") BigDecimal vocabularyShare,
            @DefaultValue("0.5") BigDecimal wrongDataShare,
            BigDecimal grammarBandCap,
            BigDecimal vocabularyBandCap,
            BigDecimal wrongDataBandCap,
            BigDecimal wrongDataOverviewBandCap) {
        public static final Density OFF = new Density(null, null, null, null, null, null, null);

        public WritingBandCaps.DensityRules rules() {
            return new WritingBandCaps.DensityRules(grammarShare, vocabularyShare, wrongDataShare, grammarBandCap,
                    vocabularyBandCap, wrongDataBandCap, wrongDataOverviewBandCap);
        }
    }

    /** The settings without density rules, for callers that built them before those rules existed. */
    public WritingSettings(int pointCost, int minWords, int maxWords, int maxChars, int gradingTimeoutSeconds,
                           int staleGradingSeconds, int dailyGradingLimit, String quotaTimezone, int progressFixedAfter,
                           boolean bandCapsEnabled, BigDecimal tooShortBandCap, BigDecimal offTopicBandCap,
                           BigDecimal noOverviewBandCap, BigDecimal partialAnswerBandCap, BigDecimal noPositionBandCap,
                           int maxCorrections, String defaultTemplate) {
        this(pointCost, minWords, maxWords, maxChars, gradingTimeoutSeconds, staleGradingSeconds, dailyGradingLimit,
                quotaTimezone, progressFixedAfter, bandCapsEnabled, tooShortBandCap, offTopicBandCap, noOverviewBandCap,
                partialAnswerBandCap, noPositionBandCap, maxCorrections, defaultTemplate, Density.OFF);
    }

    /** Settings for a grader built without Spring, as unit tests do: caps off and the first prompt template. */
    public static WritingSettings defaults() {
        return new WritingSettings(3, 50, 1000, 10000, 20, 120, 10, "Asia/Ho_Chi_Minh", 2, false,
                null, null, null, null, null, 30, "v1");
    }

    @AssertTrue(message = "minWords must be below maxWords")
    public boolean isWordRangeValid() {
        return minWords < maxWords;
    }
}
