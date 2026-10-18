package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.repository.GamificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * The learner's timezone. Daily limits (the XP cap, the streak day) are counted in it; a learner who has not chosen one
 * gets the configured default.
 */
@Component
public class LearnerTimezones {
    private static final Set<String> REGION_IDS = Set.copyOf(ZoneId.getAvailableZoneIds());

    private final GamificationRepository repository;
    private final String defaultTimezone;

    public LearnerTimezones(GamificationRepository repository,
                            @Value("${learning.gamification.default-timezone:Asia/Ho_Chi_Minh}") String defaultTimezone) {
        this.repository = repository;
        this.defaultTimezone = defaultTimezone;
    }

    public ZoneId zoneOf(UUID userId) {
        return ZoneId.of(timezoneOf(userId));
    }

    /** The saved timezone, or the default when the learner has not chosen one. */
    public String timezoneOf(UUID userId) {
        return repository.timezone(userId).orElse(defaultTimezone);
    }

    /** Accepts region ids such as {@code Asia/Ho_Chi_Minh}; offsets and abbreviations are refused. */
    public static boolean isValid(String timezone) {
        return timezone != null && REGION_IDS.contains(timezone);
    }

    public void save(UUID userId, String timezone) {
        if (!isValid(timezone)) {
            throw new LearningRequestException(400, "INVALID_TIMEZONE", "timezone must be an IANA region id");
        }
        repository.saveTimezone(userId, timezone);
    }
}
