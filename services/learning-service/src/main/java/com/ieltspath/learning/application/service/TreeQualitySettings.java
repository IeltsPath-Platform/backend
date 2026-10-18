package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.service.QuestionDifficultyRules;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Thresholds for work items made from learner data ({@code learning.tree-quality.*}): suspect prerequisite edges and
 * questions that are too easy or too hard. For a question tagged HARD, {@code hardTaggedEasyAt} is the correct rate from
 * which it counts as too easy; for one tagged EASY, {@code easyTaggedHardAt} the rate up to which it counts as too hard.
 */
@ConfigurationProperties("learning.tree-quality")
public record TreeQualitySettings(@DefaultValue("20") int minSessions, @DefaultValue("0.8") double passRateAtLeast,
                                  @DefaultValue("0.1") double rootRateBelow, @DefaultValue("20") int minAttempts,
                                  @DefaultValue("0.9") double tooEasyAbove, @DefaultValue("0.3") double tooHardBelow,
                                  @DefaultValue("0.8") double hardTaggedEasyAt,
                                  @DefaultValue("0.4") double easyTaggedHardAt) {
    public QuestionDifficultyRules.Limits questionLimits() {
        return new QuestionDifficultyRules.Limits(tooEasyAbove, tooHardBelow, hardTaggedEasyAt, easyTaggedHardAt);
    }
}
