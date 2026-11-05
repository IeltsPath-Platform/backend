package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.service.LearnerTimezones;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.service.StreakRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The current streak of many learners, each judged on their own local day, for another service that shows streaks
 * beside profiles. It only reads: a lapsed streak shows as 0 here and is stored as lost the next time the learner opens
 * their own streak.
 */
@Service
public class GetStreaksUseCase {
    private final StreakRepository streaks;
    private final LearnerTimezones timezones;
    private final Clock clock = Clock.systemUTC();

    public GetStreaksUseCase(StreakRepository streaks, LearnerTimezones timezones) {
        this.streaks = streaks;
        this.timezones = timezones;
    }

    /** Learners without a streak row are left out of the result. */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> execute(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        var stored = streaks.findAll(userIds);
        if (stored.isEmpty()) return Map.of();
        Map<UUID, ZoneId> zones = timezones.zonesOf(stored.keySet());
        Map<UUID, Integer> current = new HashMap<>();
        stored.forEach((userId, state) -> {
            LocalDate today = LocalDate.now(clock.withZone(zones.get(userId)));
            current.put(userId, StreakRules.forRead(state, today).currentDays());
        });
        return current;
    }
}
