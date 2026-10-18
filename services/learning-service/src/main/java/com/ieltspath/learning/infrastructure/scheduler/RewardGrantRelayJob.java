package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.learning.application.service.RewardGrantService;
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
 * Sends the queued points grants to Access every minute. A transaction-scoped advisory lock keeps several instances from
 * sending the same batch, and rows are claimed with SKIP LOCKED. Disable with {@code learning.rewards.relay-enabled=false}.
 */
@Component
@EnableScheduling
public class RewardGrantRelayJob {
    private static final Logger log = LoggerFactory.getLogger(RewardGrantRelayJob.class);
    private static final String LOCK_NAME = "learning:reward-grant-relay";

    private final RewardGrantService grants;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public RewardGrantRelayJob(RewardGrantService grants, JdbcTemplate jdbc,
                               PlatformTransactionManager transactionManager,
                               @Value("${learning.rewards.relay-enabled:true}") boolean enabled) {
        this.grants = grants;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${learning.rewards.relay-initial-delay:PT1M}",
            fixedDelayString = "${learning.rewards.relay-interval:PT1M}")
    public void scheduled() {
        if (!enabled) return;
        try {
            runOnce();
        } catch (RuntimeException failure) {
            log.warn("Reward grant relay failed", failure);
        }
    }

    /** One run; returns the grants sent, or -1 when another instance holds the lock. */
    public int runOnce() {
        Integer sent = transaction.execute(status -> {
            Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtext(?))", Boolean.class,
                    LOCK_NAME);
            if (!Boolean.TRUE.equals(locked)) return -1;
            return grants.sendDue();
        });
        if (sent != null && sent > 0) log.info("Reward grants sent: {}", sent);
        return sent == null ? -1 : sent;
    }
}
