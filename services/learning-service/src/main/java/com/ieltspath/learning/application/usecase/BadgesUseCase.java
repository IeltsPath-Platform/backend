package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.LearnerTimezones;
import com.ieltspath.learning.domain.repository.BadgeRepository;
import com.ieltspath.learning.domain.repository.BadgeRepository.Badge;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.service.BadgeRules;
import com.ieltspath.learning.domain.service.StreakRules;
import com.ieltspath.learning.domain.vo.StreakState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The learner's badges, earned and not yet earned with their progress, and the admin switch for a badge. */
@Service
public class BadgesUseCase {
    /** {@code progress} is null for a badge that is not counted, such as the first topic test. */
    public record Item(String code, String name, String description, int rewardPoints, boolean awarded,
                       Instant awardedAt, Progress progress) {}

    public record Progress(int current, int target) {}

    private final BadgeRepository badges;
    private final StreakRepository streaks;
    private final GamificationRecorder gamification;
    private final LearnerTimezones timezones;
    private final Clock clock = Clock.systemUTC();

    public BadgesUseCase(BadgeRepository badges, StreakRepository streaks, GamificationRecorder gamification,
                         LearnerTimezones timezones) {
        this.badges = badges;
        this.streaks = streaks;
        this.gamification = gamification;
        this.timezones = timezones;
    }

    /** Active badges, and any switched off that the learner already holds. */
    @Transactional(readOnly = true)
    public List<Item> mine(UUID userId) {
        Map<String, Instant> held = badges.awarded(userId);
        LocalDate today = LocalDate.now(clock.withZone(timezones.zoneOf(userId)));
        StreakState streak = StreakRules.forRead(streaks.find(userId).orElse(StreakState.empty()), today);
        int level = gamification.progress(userId).level();
        int maintained = badges.maintainedPoints(userId);
        int quests = badges.completedQuests(userId);
        return badges.catalog().stream().filter(badge -> badge.active() || held.containsKey(badge.code()))
                .map(badge -> new Item(badge.code(), badge.name(), badge.description(), badge.rewardPoints(),
                        held.containsKey(badge.code()), held.get(badge.code()),
                        progress(badge.code(), streak.currentDays(), maintained, level, quests))).toList();
    }

    @Transactional
    public Badge setActive(String code, boolean active) {
        if (!badges.setActive(code, active)) {
            throw new LearningRequestException(404, "BADGE_NOT_FOUND", "No badge with this code");
        }
        return badges.find(code).orElseThrow();
    }

    private static Progress progress(String code, int streakDays, int maintained, int level, int quests) {
        Integer target = BadgeRules.target(code);
        if (target == null) return null;
        int current = BadgeRules.STREAK.containsKey(code) ? streakDays
                : BadgeRules.MAINTAINED.containsKey(code) ? maintained
                : BadgeRules.LEVEL.containsKey(code) ? level : quests;
        return new Progress(Math.min(current, target), target);
    }
}
