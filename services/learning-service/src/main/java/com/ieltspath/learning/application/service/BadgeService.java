package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.BadgeRepository;
import com.ieltspath.learning.domain.repository.BadgeRepository.Badge;
import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.domain.service.BadgeRules;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

/**
 * Awards badges. Each event type calls the one check that concerns it, inside the transaction of the event, so a badge
 * and the thing that earned it commit together. A badge is held once: repeating a check, or a redelivered event, awards
 * nothing again. The points of a newly awarded badge go through the reward queue.
 */
@Component
public class BadgeService {
    static final String REWARD_REASON = "BADGE";

    private final BadgeRepository badges;
    private final RewardGrantRepository rewards;
    private final Clock clock = Clock.systemUTC();

    public BadgeService(BadgeRepository badges, RewardGrantRepository rewards) {
        this.badges = badges;
        this.rewards = rewards;
    }

    /** The streak is {@code days} long. */
    public void onStreak(UUID userId, int days) {
        award(userId, BadgeRules.reached(BadgeRules.STREAK, days));
    }

    /** A knowledge point reached the maintain tier: count the points the learner holds there. */
    public void onMaintained(UUID userId) {
        award(userId, BadgeRules.reached(BadgeRules.MAINTAINED, badges.maintainedPoints(userId)));
    }

    public void onLevel(UUID userId, int level) {
        award(userId, BadgeRules.reached(BadgeRules.LEVEL, level));
    }

    /** A daily quest was completed. */
    public void onQuestCompleted(UUID userId) {
        award(userId, BadgeRules.reached(BadgeRules.QUESTS, badges.completedQuests(userId)));
    }

    public void onTopicTest(UUID userId) {
        award(userId, java.util.List.of(BadgeRules.FIRST_TOPIC_TEST));
    }

    public void onCoursePassed(UUID userId) {
        award(userId, java.util.List.of(BadgeRules.COURSE_PASSED));
    }

    private void award(UUID userId, Collection<String> codes) {
        if (codes.isEmpty()) return;
        for (Badge badge : badges.award(userId, codes, clock.instant())) {
            if (badge.rewardPoints() > 0) rewards.enqueue(userId, REWARD_REASON, badge.code(), badge.rewardPoints());
        }
    }
}
