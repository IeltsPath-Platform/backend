package com.ieltspath.community.infrastructure.scheduler;

import com.ieltspath.community.infrastructure.persistence.JdbcDeletedUserStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** Claims pending user deletion jobs with SKIP LOCKED and anonymizes community posts/comments. */
@Component
public class UserDeletionJobWorker {
    private final JdbcDeletedUserStore store;
    private final TransactionTemplate transaction;

    public UserDeletionJobWorker(JdbcDeletedUserStore store, PlatformTransactionManager transactionManager) {
        this.store = store;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${community.user-deletion.poll-delay-ms:10000}")
    public void processPending() {
        List<JdbcDeletedUserStore.Job> claimed = transaction.execute(status -> store.claimPendingJobs(5));
        if (claimed == null || claimed.isEmpty()) return;
        for (var job : claimed) {
            transaction.executeWithoutResult(status -> {
                store.anonymizeAndDeleteCommunityData(job.userId());
                store.markJobDone(job.eventId());
            });
        }
    }
}
