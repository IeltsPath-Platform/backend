package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeRulesTest {
    @Test
    void aStreakEarnsEveryBadgeUpToItsLength() {
        assertThat(BadgeRules.reached(BadgeRules.STREAK, 6)).isEmpty();
        assertThat(BadgeRules.reached(BadgeRules.STREAK, 7)).containsExactly("STREAK_7");
        assertThat(BadgeRules.reached(BadgeRules.STREAK, 30)).containsExactly("STREAK_7", "STREAK_30");
        assertThat(BadgeRules.reached(BadgeRules.STREAK, 120)).containsExactly("STREAK_7", "STREAK_30", "STREAK_100");
    }

    @Test
    void maintainedPointsEarnTheirBadgesAtTenAndFifty() {
        assertThat(BadgeRules.reached(BadgeRules.MAINTAINED, 9)).isEmpty();
        assertThat(BadgeRules.reached(BadgeRules.MAINTAINED, 10)).containsExactly("KP_MAINTAIN_10");
        assertThat(BadgeRules.reached(BadgeRules.MAINTAINED, 50)).containsExactly("KP_MAINTAIN_10", "KP_MAINTAIN_50");
    }

    @Test
    void levelTenAndThirtyQuestsEarnTheirBadges() {
        assertThat(BadgeRules.reached(BadgeRules.LEVEL, 9)).isEmpty();
        assertThat(BadgeRules.reached(BadgeRules.LEVEL, 10)).containsExactly("LEVEL_10");
        assertThat(BadgeRules.reached(BadgeRules.QUESTS, 29)).isEmpty();
        assertThat(BadgeRules.reached(BadgeRules.QUESTS, 30)).containsExactly("QUESTS_30");
    }

    @Test
    void onlyCountableBadgesHaveATarget() {
        assertThat(BadgeRules.target("STREAK_30")).isEqualTo(30);
        assertThat(BadgeRules.target("LEVEL_10")).isEqualTo(10);
        assertThat(BadgeRules.target(BadgeRules.COURSE_PASSED)).isNull();
        assertThat(BadgeRules.target(BadgeRules.FIRST_TOPIC_TEST)).isNull();
    }
}
