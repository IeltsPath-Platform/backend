package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import com.ieltspath.learning.domain.vo.MasteryRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The mastery rules in force: the service configuration, with any Admin override on top. {@code
 * learning.review-mastery-threshold} stays as the older name of {@code learning.mastery.priority-below}.
 */
@Slf4j
@Component
public class MasteryRulesProvider {
    public static final String PRIORITY_BELOW = "mastery.priority-below";
    public static final String MAINTAIN_FROM = "mastery.maintain-from";
    public static final String REVIEW_INTERVALS_DAYS = "mastery.review-intervals-days";
    public static final String CONSOLIDATE_DAYS = "review.consolidate-days";
    public static final List<String> KEYS = List.of(PRIORITY_BELOW, MAINTAIN_FROM, REVIEW_INTERVALS_DAYS,
            CONSOLIDATE_DAYS);

    private final LearningRuleConfigRepository configs;
    private final ObjectMapper json;
    private final MasteryRules configured;

    public MasteryRulesProvider(LearningRuleConfigRepository configs, ObjectMapper json,
                                @Value("${learning.mastery.priority-below:${learning.review-mastery-threshold:0.6}}")
                                double priorityBelow,
                                @Value("${learning.mastery.maintain-from:0.8}") double maintainFrom,
                                @Value("${learning.mastery.review-intervals-days:7,14,30,45,60}")
                                List<Integer> reviewIntervalsDays,
                                @Value("${learning.mastery.consolidate-days:3}") int consolidateDays) {
        this.configs = configs;
        this.json = json;
        this.configured = new MasteryRules(priorityBelow, maintainFrom, reviewIntervalsDays, consolidateDays);
    }

    /** The service configuration without Admin overrides. */
    public MasteryRules configured() {
        return configured;
    }

    /** The configuration with Admin overrides applied. A stored set that is no longer valid is ignored whole. */
    public MasteryRules current() {
        Map<String, String> overrides = configs.findAll();
        if (overrides.isEmpty()) return configured;
        try {
            return new MasteryRules(
                    number(overrides, PRIORITY_BELOW, configured.priorityBelow()),
                    number(overrides, MAINTAIN_FROM, configured.maintainFrom()),
                    days(overrides, configured.reviewIntervalsDays()),
                    (int) number(overrides, CONSOLIDATE_DAYS, configured.consolidateDays()));
        } catch (IllegalArgumentException | java.io.IOException exception) {
            log.warn("Stored learning rule overrides are invalid, using the service configuration");
            return configured;
        }
    }

    /** The keys that currently have an Admin override. */
    public List<String> overriddenKeys() {
        Map<String, String> overrides = configs.findAll();
        return KEYS.stream().filter(overrides::containsKey).toList();
    }

    private double number(Map<String, String> overrides, String key, double fallback) throws java.io.IOException {
        String stored = overrides.get(key);
        if (stored == null) return fallback;
        JsonNode node = json.readTree(stored);
        if (!node.isNumber()) throw new IllegalArgumentException(key + " must be a number");
        return node.asDouble();
    }

    private List<Integer> days(Map<String, String> overrides, List<Integer> fallback) throws java.io.IOException {
        String stored = overrides.get(REVIEW_INTERVALS_DAYS);
        if (stored == null) return fallback;
        JsonNode node = json.readTree(stored);
        if (!node.isArray()) throw new IllegalArgumentException(REVIEW_INTERVALS_DAYS + " must be a list");
        List<Integer> values = new ArrayList<>();
        node.forEach(item -> {
            if (!item.isInt()) throw new IllegalArgumentException(REVIEW_INTERVALS_DAYS + " must hold whole days");
            values.add(item.asInt());
        });
        return values;
    }
}
