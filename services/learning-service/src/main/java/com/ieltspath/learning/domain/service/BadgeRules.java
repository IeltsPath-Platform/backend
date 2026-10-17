package com.ieltspath.learning.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which badges a count has earned. Each event type checks only its own badges, so no check scans the whole catalog. The
 * thresholds are part of the code; an admin can switch a badge on or off but not change what it asks.
 */
public final class BadgeRules {
    public static final String FIRST_TOPIC_TEST = "FIRST_TOPIC_TEST";
    public static final String COURSE_PASSED = "COURSE_PASSED";

    /** Badge code by the count it needs, for each countable family. */
    public static final Map<String, Integer> STREAK = targets("STREAK_7", 7, "STREAK_30", 30, "STREAK_100", 100);
    public static final Map<String, Integer> MAINTAINED = targets("KP_MAINTAIN_10", 10, "KP_MAINTAIN_50", 50);
    public static final Map<String, Integer> LEVEL = targets("LEVEL_10", 10);
    public static final Map<String, Integer> QUESTS = targets("QUESTS_30", 30);

    private BadgeRules() {}

    /** The codes of {@code family} whose target {@code value} has reached. */
    public static List<String> reached(Map<String, Integer> family, int value) {
        List<String> codes = new ArrayList<>();
        family.forEach((code, target) -> {
            if (value >= target) codes.add(code);
        });
        return codes;
    }

    /** The target of a countable badge, or null for one that is not counted. */
    public static Integer target(String code) {
        for (Map<String, Integer> family : List.of(STREAK, MAINTAINED, LEVEL, QUESTS)) {
            if (family.containsKey(code)) return family.get(code);
        }
        return null;
    }

    private static Map<String, Integer> targets(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) map.put((String) pairs[index], (Integer) pairs[index + 1]);
        return java.util.Collections.unmodifiableMap(map);
    }
}
