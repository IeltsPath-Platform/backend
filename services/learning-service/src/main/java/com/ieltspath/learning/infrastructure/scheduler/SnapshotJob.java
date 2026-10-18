package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.learning.application.service.SnapshotService;
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
 * Writes the weekly snapshot on Monday at 00:30 and the monthly one on the first of the month, then drops what is past
 * retention. A transaction-scoped advisory lock keeps several instances from running a snapshot twice; a run that finds
 * the lock taken is skipped. Disable with {@code learning.snapshots.enabled=false}.
 */
@Component
@EnableScheduling
public class SnapshotJob {
    private static final Logger log = LoggerFactory.getLogger(SnapshotJob.class);
    private static final String LOCK_NAME = "learning:snapshots";

    private final SnapshotService snapshots;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public SnapshotJob(SnapshotService snapshots, JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                       @Value("${learning.snapshots.enabled:true}") boolean enabled) {
        this.snapshots = snapshots;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
    }

    @Scheduled(cron = "${learning.snapshots.weekly-cron:0 30 0 * * MON}",
            zone = "${learning.snapshots.zone:Asia/Ho_Chi_Minh}")
    public void weekly() {
        if (enabled) guarded("weekly", () -> snapshots.snapshotWeek(snapshots.lastWeekStart()));
    }

    @Scheduled(cron = "${learning.snapshots.monthly-cron:0 30 0 1 * *}",
            zone = "${learning.snapshots.zone:Asia/Ho_Chi_Minh}")
    public void monthly() {
        if (enabled) guarded("monthly", () -> snapshots.snapshotMonth(snapshots.lastMonthStart()));
    }

    private void guarded(String name, java.util.function.IntSupplier run) {
        try {
            runLocked(run);
        } catch (RuntimeException failure) {
            log.warn("Snapshot run failed: kind={}", name, failure);
        }
    }

    /** One run under the lock; returns the snapshots written, or -1 when another instance holds the lock. */
    public int runLocked(java.util.function.IntSupplier run) {
        Integer written = transaction.execute(status -> {
            Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtext(?))", Boolean.class,
                    LOCK_NAME);
            if (!Boolean.TRUE.equals(locked)) return -1;
            int count = run.getAsInt();
            snapshots.purge();
            return count;
        });
        return written == null ? -1 : written;
    }
}
