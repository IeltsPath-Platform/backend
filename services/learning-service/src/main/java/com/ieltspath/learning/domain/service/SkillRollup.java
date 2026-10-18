package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.MasteryTier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Groups a learner's knowledge points into the six blocks of the skill view: the four skills, then grammar and
 * vocabulary. A block's mastery is the mean over its judged points weighted by evidence; points with too little
 * evidence to judge are counted in the tiers but not in the mean.
 */
public final class SkillRollup {
    public static final List<String> ORDER =
            List.of("LISTENING", "READING", "WRITING", "SPEAKING", "GRAMMAR", "VOCABULARY");

    private SkillRollup() {}

    public record Item(String skill, MasteryTier tier, double mastery, int evidenceCount) {}

    /** {@code mastery} is null when no point of the block is judged. {@code tierCounts} lists every tier. */
    public record Summary(String skill, Double mastery, Map<String, Integer> tierCounts, int kpCount) {}

    public static List<Summary> summarize(List<Item> items) {
        Map<String, List<Item>> bySkill = new HashMap<>();
        for (Item item : items) {
            if (item.skill() != null) {
                bySkill.computeIfAbsent(item.skill().toUpperCase(Locale.ROOT), key -> new ArrayList<>()).add(item);
            }
        }
        List<Summary> summaries = new ArrayList<>();
        for (String skill : ORDER) {
            List<Item> own = bySkill.getOrDefault(skill, List.of());
            Map<String, Integer> tierCounts = new LinkedHashMap<>();
            for (MasteryTier tier : MasteryTier.values()) tierCounts.put(tier.name(), 0);
            double weighted = 0;
            long weight = 0;
            for (Item item : own) {
                tierCounts.merge(item.tier().name(), 1, Integer::sum);
                if (item.tier() != MasteryTier.INSUFFICIENT) {
                    weighted += item.mastery() * item.evidenceCount();
                    weight += item.evidenceCount();
                }
            }
            summaries.add(new Summary(skill, weight == 0 ? null : weighted / weight, tierCounts, own.size()));
        }
        return summaries;
    }
}
