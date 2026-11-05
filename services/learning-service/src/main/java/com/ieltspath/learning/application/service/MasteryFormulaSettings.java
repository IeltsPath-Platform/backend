package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import com.ieltspath.learning.domain.vo.Mp1Params;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Which mastery formula is in force and the MP-1.0 parameters. The formula is {@code learning.mastery.policy}
 * (the shipped Config Server value is {@code MP1}; {@code DEEPTUTOR} is only the fallback when no value is supplied, which
 * is what the tests run on), and a {@code mastery.policy} row in {@code learning_rule_configs} overrides it. The
 * parameters come from {@code learning.mastery.mp1.*}.
 */
@Slf4j
@Component
public class MasteryFormulaSettings {
    public static final String POLICY_KEY = "mastery.policy";
    public static final String ERROR_TRAP_WEIGHT_KEY = "mastery.mp1.source-weight.error-trap";

    public enum Formula { DEEPTUTOR, MP1 }

    private final LearningRuleConfigRepository configs;
    private final ObjectMapper json;
    private final Formula configured;
    private final Mp1Params params;
    private final double configuredErrorTrapWeight;

    public MasteryFormulaSettings(LearningRuleConfigRepository configs, ObjectMapper json,
                                  @Value("${learning.mastery.policy:DEEPTUTOR}") String policy,
                                  @Value("${learning.mastery.mp1.window:8}") int window,
                                  @Value("${learning.mastery.mp1.max-practice-in-window:3}") int maxPractice,
                                  @Value("${learning.mastery.mp1.half-life-days:30}") double halfLifeDays,
                                  @Value("${learning.mastery.mp1.min-evidence:3}") int minEvidence,
                                  @Value("${learning.mastery.mp1.min-distinct-days:2}") int minDistinctDays,
                                  @Value("${learning.mastery.mp1.min-confidence:1.0}") double minConfidence,
                                  @Value("${learning.mastery.mp1.hysteresis:0.03}") double hysteresis,
                                  @Value("${learning.mastery.mp1.same-item-days:7}") int sameItemDays,
                                  @Value("${learning.mastery.mp1.source-weights.exam:1.0}") double exam,
                                  @Value("${learning.mastery.mp1.source-weights.review:1.0}") double review,
                                  @Value("${learning.mastery.mp1.source-weights.probe:1.0}") double probe,
                                  @Value("${learning.mastery.mp1.source-weights.practice:0.6}") double practice,
                                  @Value("${learning.mastery.mp1.source-weights.essay:0.5}") double essay,
                                  @Value("${learning.mastery.mp1.source-weights.error-trap:0.3}") double errorTrap) {
        this.configs = configs;
        this.json = json;
        this.configured = parse(policy);
        this.configuredErrorTrapWeight = errorTrap;
        this.params = new Mp1Params(window, maxPractice, halfLifeDays, minEvidence, minDistinctDays, minConfidence,
                hysteresis, sameItemDays, new Mp1Params.SourceWeights(exam, review, probe, practice, essay, errorTrap));
    }

    public Formula formula() {
        String stored = configs.findAll().get(POLICY_KEY);
        if (stored == null) return configured;
        try {
            JsonNode node = json.readTree(stored);
            return node.isTextual() ? parse(node.asText()) : configured;
        } catch (java.io.IOException | IllegalArgumentException exception) {
            log.warn("Stored mastery policy is invalid, using the service configuration");
            return configured;
        }
    }

    public boolean usesMp1() {
        return formula() == Formula.MP1;
    }

    public Mp1Params params() {
        double trapWeight = configuredErrorTrapWeight;
        String stored = configs.findAll().get(ERROR_TRAP_WEIGHT_KEY);
        if (stored != null) {
            try {
                JsonNode node = json.readTree(stored);
                if (node.isNumber() && Double.isFinite(node.asDouble()) && node.asDouble() >= 0) {
                    trapWeight = node.asDouble();
                }
            } catch (java.io.IOException exception) {
                log.warn("Stored error-trap mastery weight is invalid, using the service configuration");
            }
        }
        Mp1Params.SourceWeights weights = params.weights();
        return new Mp1Params(params.window(), params.maxPracticeInWindow(), params.halfLifeDays(), params.minEvidence(),
                params.minDistinctDays(), params.minConfidence(), params.hysteresis(), params.sameItemDays(),
                new Mp1Params.SourceWeights(weights.exam(), weights.review(), weights.probe(), weights.practice(),
                        weights.essay(), trapWeight));
    }

    private static Formula parse(String value) {
        return Formula.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
