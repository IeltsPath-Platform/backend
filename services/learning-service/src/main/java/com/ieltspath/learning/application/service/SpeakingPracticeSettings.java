package com.ieltspath.learning.application.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code learning.speaking.*}. {@code pointCost} is the price of one AI-graded submission, charged after grading.
 * Recordings go to {@code bucket} under the {@code practice/} prefix; the bucket's own size limit must be at least
 * {@code maxRecordingBytes} (10 MB holds about five minutes of 16 kHz mono WAV).
 *
 * <p>This record has one constructor on purpose: Spring Boot binds a record with several constructors only when one is
 * marked, and a second one made the whole service fail to start. Tests build it through their own fixture.
 *
 * <p>Values marked "interim" have no calibration behind them and are to be settled with examiners before the feature
 * is opened to learners: {@code passBand}, the band caps and {@code minTotalWords}.
 */
@Validated
@ConfigurationProperties("learning.speaking")
public record SpeakingPracticeSettings(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("5") @Min(1) int pointCost,
        @DefaultValue("speaking-recordings") @NotBlank String bucket,
        @DefaultValue("10485760") @Min(1) long maxRecordingBytes,
        @DefaultValue("1800") @Min(60) int uploadStaleSeconds,
        @DefaultValue("90") @Min(1) int retentionDays,
        @DefaultValue("Asia/Ho_Chi_Minh") @NotBlank String timezone,
        @DefaultValue("5") @Min(1) int dailyGradingLimit,
        @DefaultValue("120") @Min(30) int staleAfterSeconds,
        @DefaultValue("3") @Min(1) int maxAttempts,
        @DefaultValue("6.0") double passBand,
        @DefaultValue("60000") @Min(1000) int maxInputChars,
        @DefaultValue("3") @Min(1) int historyMinEvidence,
        @DefaultValue("5") @Min(1) int historyMaxKps,
        @DefaultValue("") String historyExtraKpCodes,
        /** How many of the newest events of a point decide its trend. */
        @DefaultValue("5") @Min(4) @Max(20) int historyTrendWindow,
        /** Evidence older than this many days does not count as the learner's present state. */
        @DefaultValue("90") @Min(1) int historyMaxAgeDays,
        /** Interim: fewer spoken words than this in the whole submission means it is too short to grade fairly. */
        @DefaultValue("30") @Min(1) int minTotalWords,
        /** Interim: the highest band a criterion can get when the submission is flagged too short. */
        @DefaultValue("5.0") @Min(0) @Max(9) double tooShortBandCap,
        /** Interim: the highest band when the answers do not address the questions. */
        @DefaultValue("4.0") @Min(0) @Max(9) double offTopicBandCap,
        /** Interim: the highest band when the answers are not in English. */
        @DefaultValue("3.0") @Min(0) @Max(9) double notEnglishBandCap) {
}
