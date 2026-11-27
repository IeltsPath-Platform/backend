package com.ieltspath.community.infrastructure.scheduler;

import com.ieltspath.community.infrastructure.persistence.JdbcDeletedUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** Claims pending/expired user deletion jobs with a lease and anonymizes community posts/comments. */
@Component
public class UserDeletionJobWorker {
    private static final Logger log = LoggerFactory.getLogger(UserDeletionJobWorker.class);

    private final JdbcDeletedUserStore store;
    private final TransactionTemplate transaction;
    private final long leaseSeconds;
    private final int maxAttempts;

    public UserDeletionJobWorker(JdbcDeletedUserStore store,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${community.user-deletion.lease-seconds:300}") long leaseSeconds,
                                 @Value("${community.user-deletion.max-attempts:10}") int maxAttempts) {
        this.store = store;
        this.transaction = new TransactionTemplate(transactionManager);
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${community.user-deletion.poll-delay-ms:10000}")
    public void processPending() {
        List<JdbcDeletedUserStore.Job> claimed = transaction.execute(
                status -> store.claimPendingJobs(5, leaseSeconds, maxAttempts));
        if (claimed == null || claimed.isEmpty()) return;
        for (var job : claimed) {
            try {
                transaction.executeWithoutResult(status -> {
                    store.anonymizeAndDeleteCommunityData(job.userId());
                    store.markJobDone(job.eventId());
                });
            } catch (RuntimeException failure) {
                String errorCode = failure.getClass().getSimpleName();
                if (job.attempts() >= maxAttempts) {
                    log.error("User deletion job permanently failed: eventId={}, userId={}, attempts={}, errorCode={}",
                            job.eventId(), job.userId(), job.attempts(), errorCode);
                    transaction.executeWithoutResult(status -> store.markJobFailed(job.eventId(), errorCode));
                } else {
                    log.warn("User deletion job will be retried after lease: eventId={}, userId={}, attempts={}, errorCode={}",
                            job.eventId(), job.userId(), job.attempts(), errorCode);
                    transaction.executeWithoutResult(status -> store.recordJobError(job.eventId(), errorCode));
                }
            }
        }
    }
}
