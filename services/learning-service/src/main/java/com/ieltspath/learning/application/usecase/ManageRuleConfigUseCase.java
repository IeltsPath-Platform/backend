package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.command.UpdateRuleConfigCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.RuleConfigResult;
import com.ieltspath.learning.application.service.MasteryProfileMaintenance;
import com.ieltspath.learning.application.service.MasteryRulesProvider;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import com.ieltspath.learning.domain.vo.MasteryRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Admin view and update of the mastery rules, and the background re-tiering that applies a change to old profiles. */
@Service
public class ManageRuleConfigUseCase {
    private final MasteryRulesProvider rules;
    private final LearningRuleConfigRepository configs;
    private final MasteryProfileMaintenance maintenance;
    private final ObjectMapper json;

    public ManageRuleConfigUseCase(MasteryRulesProvider rules, LearningRuleConfigRepository configs,
                                   MasteryProfileMaintenance maintenance, ObjectMapper json) {
        this.rules = rules;
        this.configs = configs;
        this.maintenance = maintenance;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public RuleConfigResult get() {
        return result();
    }

    /**
     * Stores the given values after checking that the resulting rules are valid as a whole. A change affects profiles
     * written afterwards; {@link #recompute()} applies it to the stored ones.
     */
    @Transactional
    public RuleConfigResult update(UUID adminId, UpdateRuleConfigCommand command) {
        MasteryRules current = rules.current();
        MasteryRules candidate;
        try {
            candidate = new MasteryRules(
                    command.priorityBelow() != null ? command.priorityBelow() : current.priorityBelow(),
                    command.maintainFrom() != null ? command.maintainFrom() : current.maintainFrom(),
                    command.reviewIntervalsDays() != null ? command.reviewIntervalsDays() : current.reviewIntervalsDays(),
                    command.consolidateDays() != null ? command.consolidateDays() : current.consolidateDays());
        } catch (IllegalArgumentException exception) {
            throw new LearningRequestException(422, "INVALID_RULE_CONFIG", exception.getMessage());
        }
        if (command.priorityBelow() != null) store(MasteryRulesProvider.PRIORITY_BELOW, candidate.priorityBelow(), adminId);
        if (command.maintainFrom() != null) store(MasteryRulesProvider.MAINTAIN_FROM, candidate.maintainFrom(), adminId);
        if (command.reviewIntervalsDays() != null) {
            store(MasteryRulesProvider.REVIEW_INTERVALS_DAYS, candidate.reviewIntervalsDays(), adminId);
        }
        if (command.consolidateDays() != null) store(MasteryRulesProvider.CONSOLIDATE_DAYS, candidate.consolidateDays(), adminId);
        return result();
    }

    /** Starts re-tiering every stored profile in the background; false when a job is already running. */
    public boolean recompute() {
        return maintenance.startRecompute();
    }

    private RuleConfigResult result() {
        MasteryRules current = rules.current();
        return new RuleConfigResult(current.priorityBelow(), current.maintainFrom(), current.reviewIntervalsDays(),
                current.consolidateDays(), rules.overriddenKeys());
    }

    private void store(String key, Object value, UUID adminId) {
        try {
            configs.upsert(key, json.writeValueAsString(value), adminId);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot store rule " + key, exception);
        }
    }
}
