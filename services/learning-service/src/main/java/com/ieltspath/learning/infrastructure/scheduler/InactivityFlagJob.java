package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.learning.application.service.InactivityFlagSync;
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
 * Opens the INACTIVE flags once a day. A transaction-scoped advisory lock keeps several service instances from running it
 * twice; a run that finds the lock taken is skipped. Disable with {@code learning.flags.inactive-enabled=false}.
 */
@Component
@EnableScheduling
public class InactivityFlagJob {
    private static final Logger log = LoggerFactory.getLogger(InactivityFlagJob.class);
    private static final String LOCK_NAME = "learning:inactivity-flags";

    private final InactivityFlagSync sync;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public InactivityFlagJob(InactivityFlagSync sync, JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                             @Value("${learning.flags.inactive-enabled:true}") boolean enabled) {
        this.sync = sync;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${learning.flags.inactive-initial-delay:PT20M}",
            fixedDelayString = "${learning.flags.inactive-interval:P1D}")
    public void scheduled() {
        if (!enabled) return;
        try {
            runOnce();
        } catch (RuntimeException failure) {
            log.warn("Inactivity flag run failed", failure);
        }
    }

    /** One run; returns the flags opened, or -1 when another instance holds the lock. */
    public int runOnce() {
        Integer opened = transaction.execute(status -> {
            Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtext(?))", Boolean.class,
                    LOCK_NAME);
            if (!Boolean.TRUE.equals(locked)) return -1;
            return sync.run();
        });
        if (opened != null && opened > 0) log.info("Opened {} INACTIVE flags", opened);
        return opened == null ? -1 : opened;
    }
}
