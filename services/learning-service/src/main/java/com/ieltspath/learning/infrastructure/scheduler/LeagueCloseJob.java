package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.learning.application.service.LeagueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Closes the finished league week on Sunday 00:00 UTC. A transaction-scoped advisory lock keeps several instances from
 * closing a week twice, and a week records when it was closed, so a repeated run changes nothing. Disable with
 * {@code learning.league.close-enabled=false}.
 */
@Component
@EnableScheduling
public class LeagueCloseJob {
    private static final Logger log = LoggerFactory.getLogger(LeagueCloseJob.class);
    private static final String LOCK_NAME = "learning:league-close";

    private final LeagueService league;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public LeagueCloseJob(LeagueService league, JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                          @Value("${learning.league.close-enabled:true}") boolean enabled) {
        this.league = league;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
    }

    @Scheduled(cron = "${learning.league.close-cron:0 0 0 * * SUN}", zone = "UTC")
    public void scheduled() {
        if (!enabled) return;
        try {
            runOnce();
        } catch (RuntimeException failure) {
            log.warn("League close failed", failure);
        }
    }

    /** One run; returns the weeks closed, or -1 when another instance holds the lock. */
    public int runOnce() {
        Integer closed = transaction.execute(status -> {
            Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtext(?))", Boolean.class,
                    LOCK_NAME);
            if (!Boolean.TRUE.equals(locked)) return -1;
            return league.closeFinishedWeeks();
        });
        return closed == null ? -1 : closed;
    }
}
