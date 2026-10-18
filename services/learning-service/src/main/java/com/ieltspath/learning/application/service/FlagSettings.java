package com.ieltspath.learning.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Thresholds of the anomaly flags ({@code learning.flags.*}). */
@ConfigurationProperties("learning.flags")
public record FlagSettings(@DefaultValue("10") int minQuestions, @DefaultValue("3000") long medianBelowMs,
                           @DefaultValue("0.9") double minAccuracy, @DefaultValue("30") int burstCount,
                           @DefaultValue("10") int burstWindowMinutes, @DefaultValue("1") int dayWindowDays,
                           @DefaultValue("24") int repeatSilenceHours) {}
