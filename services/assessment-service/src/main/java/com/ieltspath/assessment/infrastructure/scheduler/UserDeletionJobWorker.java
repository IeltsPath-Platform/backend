package com.ieltspath.assessment.infrastructure.scheduler;

import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcDeletedUserStore;
import com.ieltspath.commonstorage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

/** Claims pending user deletion jobs with SKIP LOCKED and purges assessment data and Speaking recordings. */
@Component
public class UserDeletionJobWorker {
    private static final Logger log = LoggerFactory.getLogger(UserDeletionJobWorker.class);
    private final JdbcDeletedUserStore store;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final SpeakingRecordingProperties recordingProperties;
    private final TransactionTemplate transaction;

    public UserDeletionJobWorker(JdbcDeletedUserStore store,
                                 ObjectProvider<ObjectStorage> storageProvider,
                                 SpeakingRecordingProperties recordingProperties,
                                 PlatformTransactionManager transactionManager) {
        this.store = store;
        this.storageProvider = storageProvider;
        this.recordingProperties = recordingProperties;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${assessment.user-deletion.poll-delay-ms:10000}")
    public void processPending() {
        List<JdbcDeletedUserStore.Job> claimed = transaction.execute(status -> store.claimPendingJobs(5));
        if (claimed == null || claimed.isEmpty()) return;
        for (var job : claimed) {
            purgeUser(job);
        }
    }

    public void purgeUser(JdbcDeletedUserStore.Job job) {
        UUID userId = job.userId();
        deleteSpeakingRecordings(userId);
        transaction.executeWithoutResult(status -> {
            store.deleteUserAssessmentData(userId);
            store.markJobDone(job.eventId());
        });
    }

    private void deleteSpeakingRecordings(UUID userId) {
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) return;
        List<String> keys = store.findSpeakingRecordingRefs(userId, 1000);
        for (String key : keys) {
            try {
                storage.delete(recordingProperties.getBucket(), key);
            } catch (RuntimeException failure) {
                log.warn("Could not delete Speaking recording during user deletion: userId={}, errorType={}",
                        userId, failure.getClass().getSimpleName());
            }
        }
    }
}
