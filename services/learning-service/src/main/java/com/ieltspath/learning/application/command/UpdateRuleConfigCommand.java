package com.ieltspath.learning.application.command;

import java.util.List;

/** The rule values Admin wants to change; a null field keeps its current value. */
public record UpdateRuleConfigCommand(Double priorityBelow, Double maintainFrom, List<Integer> reviewIntervalsDays,
                                      Integer consolidateDays) {}
