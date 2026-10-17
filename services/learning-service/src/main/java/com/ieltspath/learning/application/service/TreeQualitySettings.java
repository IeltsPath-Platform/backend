package com.ieltspath.learning.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Thresholds for work items made from learner data ({@code learning.tree-quality.*}): suspect prerequisite edges and
 * questions that are too easy or too hard.
 */
@ConfigurationProperties("learning.tree-quality")
public record TreeQualitySettings(@DefaultValue("20") int minSessions, @DefaultValue("0.8") double passRateAtLeast,
                                  @DefaultValue("0.1") double rootRateBelow, @DefaultValue("20") int minAttempts,
                                  @DefaultValue("0.9") double tooEasyAbove, @DefaultValue("0.3") double tooHardBelow) {}
