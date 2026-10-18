package com.ieltspath.learning.application.result;

import java.util.List;

/** The mastery rules in force. {@code overridden} lists the keys whose value comes from Admin, not the service config. */
public record RuleConfigResult(double priorityBelow, double maintainFrom, List<Integer> reviewIntervalsDays,
                               int consolidateDays, List<String> overridden) {}
