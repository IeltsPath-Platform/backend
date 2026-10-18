package com.ieltspath.learning.api.dto.request;

import com.ieltspath.learning.application.command.UpdateRuleConfigCommand;

import java.util.List;

/** Any subset of the mastery rules; a missing field keeps its current value. */
public record UpdateRuleConfigRequest(Double priorityBelow, Double maintainFrom, List<Integer> reviewIntervalsDays,
                                      Integer consolidateDays) {
    public UpdateRuleConfigCommand toCommand() {
        return new UpdateRuleConfigCommand(priorityBelow, maintainFrom, reviewIntervalsDays, consolidateDays);
    }
}
