package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.learning.application.service.MasteryProfileMaintenance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Brings MP-1.0 scores up to date for learners who have not answered lately, since the score falls as evidence ages.
 * Runs once a day; a session-level advisory lock keeps several service instances from repeating the work, and a round
 * that finds the lock taken is skipped. It does nothing under the other formula. Disable with
 * {@code learning.mastery.decay-enabled=false}.
 */
@Component
@EnableScheduling
public class MasteryDecayJob {
    private static final Logger log = LoggerFactory.getLogger(MasteryDecayJob.class);
    private static final String LOCK_NAME = "learning:mastery-decay";

    private final MasteryProfileMaintenance maintenance;
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final Duration staleAfter;

    public MasteryDecayJob(MasteryProfileMaintenance maintenance, JdbcTemplate jdbc,
                           @Value("${learning.mastery.decay-enabled:true}") boolean enabled,
                           @Value("${learning.mastery.decay-stale-after:PT24H}") Duration staleAfter) {
        this.maintenance = maintenance;
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.staleAfter = staleAfter;
    }

    @Scheduled(initialDelayString = "${learning.mastery.decay-initial-delay:PT10M}",
            fixedDelayString = "${learning.mastery.decay-interval:P1D}")
    public void scheduled() {
        if (!enabled) return;
        try {
            runOnce();
        } catch (RuntimeException failure) {
            log.warn("Mastery decay failed", failure);
        }
    }

    /** One round; returns the profiles looked at, or -1 when another instance holds the lock. */
    public int runOnce() {
        // The batches commit on their own, so the lock must outlive a single transaction: hold it on one connection.
        return jdbc.execute((java.sql.Connection connection) -> {
            try (var lock = connection.prepareStatement("SELECT pg_try_advisory_lock(hashtext(?))")) {
                lock.setString(1, LOCK_NAME);
                try (var rows = lock.executeQuery()) {
                    rows.next();
                    if (!rows.getBoolean(1)) return -1;
                }
            }
            try {
                return maintenance.decayStale(staleAfter);
            } finally {
                try (var unlock = connection.prepareStatement("SELECT pg_advisory_unlock(hashtext(?))")) {
                    unlock.setString(1, LOCK_NAME);
                    unlock.execute();
                }
            }
        });
    }
}
