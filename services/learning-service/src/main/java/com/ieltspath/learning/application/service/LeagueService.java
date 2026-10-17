package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.LeagueRepository;
import com.ieltspath.learning.domain.service.LeagueRules;
import com.ieltspath.learning.domain.vo.LeagueTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Weekly league membership and the weekly close. {@link #addWeeklyXp} runs in the submission transaction while the caller
 * holds the learner's lock, so a learner is never placed in two groups.
 */
@Component
public class LeagueService {
    private static final Logger log = LoggerFactory.getLogger(LeagueService.class);

    private final LeagueRepository repository;
    private final Clock clock = Clock.systemUTC();

    public LeagueService(LeagueRepository repository) {
        this.repository = repository;
    }

    /**
     * Adds counted XP to the learner's week. The first XP of the week places them in a group of the tier they play in;
     * a learner with no XP in the week is in no group.
     */
    public void addWeeklyXp(UUID userId, int xp) {
        if (xp <= 0) return;
        Instant now = clock.instant();
        LocalDate week = LeagueRules.weekStart(now);
        if (repository.addXp(userId, week, xp, now)) return;
        repository.join(userId, week, repository.tierOf(userId).orElse(LeagueTier.LOWEST), xp, now);
    }

    /** Closes every finished week not yet closed; returns how many weeks were closed. Safe to run again. */
    public int closeFinishedWeeks() {
        Instant now = clock.instant();
        int closed = 0;
        for (LocalDate week : repository.unclosedWeeksBefore(LeagueRules.weekStart(now))) {
            int ranked = repository.closeWeek(week, LeagueRules.PROMOTE_TOP, LeagueRules.DEMOTE_FROM, now);
            if (ranked >= 0) {
                closed++;
                log.info("League week closed: week={} learners={}", week, ranked);
            }
        }
        return closed;
    }
}
