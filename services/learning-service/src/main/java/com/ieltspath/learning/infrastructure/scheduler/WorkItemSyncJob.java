package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.learning.application.service.WorkItemSync;
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
 * Keeps the staff work queues current without anyone opening them: every few hours it adds the items new sources need.
 * Only one service instance runs a round at a time (a transaction-scoped database lock), so several instances never
 * duplicate work; a round that finds the lock taken is skipped. Disable with {@code learning.work-items.sync-enabled=false}.
 */
@Component
@EnableScheduling
public class WorkItemSyncJob {
    private static final Logger log = LoggerFactory.getLogger(WorkItemSyncJob.class);
    private static final String LOCK_NAME = "learning:work-item-sync";

    private final WorkItemSync sync;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public WorkItemSyncJob(WorkItemSync sync, JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                           @Value("${learning.work-items.sync-enabled:true}") boolean enabled) {
        this.sync = sync;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${learning.work-items.sync-initial-delay:PT5M}",
            fixedDelayString = "${learning.work-items.sync-interval:PT3H}")
    public void scheduled() {
        if (!enabled) return;
        try {
            runOnce();
        } catch (RuntimeException failure) {
            log.warn("Work item sync failed", failure);
        }
    }

    /** One round; returns the number of items added, or -1 when another instance holds the lock. */
    public int runOnce() {
        Integer added = transaction.execute(status -> {
            Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtext(?))", Boolean.class,
                    LOCK_NAME);
            if (!Boolean.TRUE.equals(locked)) return -1;
            return sync.sync();
        });
        if (added != null && added > 0) log.info("Work item sync added {} items", added);
        return added == null ? -1 : added;
    }
}
