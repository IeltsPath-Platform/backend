package com.ieltspath.assessment.infrastructure.scheduler;

import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.infrastructure.persistence.adapter.JdbcDeletedUserStore;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Claims pending/expired user deletion jobs with a lease and purges assessment data and Speaking recordings. */
@Component
public class UserDeletionJobWorker {
    private static final Logger log = LoggerFactory.getLogger(UserDeletionJobWorker.class);
    private static final int FILE_BATCH_SIZE = 100;

    private final JdbcDeletedUserStore store;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final SpeakingRecordingProperties recordingProperties;
    private final TransactionTemplate transaction;
    private final long leaseSeconds;
    private final int maxAttempts;

    public UserDeletionJobWorker(JdbcDeletedUserStore store,
                                 ObjectProvider<ObjectStorage> storageProvider,
                                 SpeakingRecordingProperties recordingProperties,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${assessment.user-deletion.lease-seconds:300}") long leaseSeconds,
                                 @Value("${assessment.user-deletion.max-attempts:10}") int maxAttempts) {
        this.store = store;
        this.storageProvider = storageProvider;
        this.recordingProperties = recordingProperties;
        this.transaction = new TransactionTemplate(transactionManager);
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${assessment.user-deletion.poll-delay-ms:10000}")
    public void processPending() {
        List<JdbcDeletedUserStore.Job> claimed = transaction.execute(
                status -> store.claimPendingJobs(5, leaseSeconds, maxAttempts));
        if (claimed == null || claimed.isEmpty()) return;
        for (var job : claimed) {
            try {
                purgeUser(job);
            } catch (RuntimeException failure) {
                handleFailure(job, failure);
            }
        }
    }

    public void purgeUser(JdbcDeletedUserStore.Job job) {
        UUID userId = job.userId();
        deleteAllSpeakingRecordings(userId);
        transaction.executeWithoutResult(status -> {
            store.deleteUserAssessmentData(userId);
            store.markJobDone(job.eventId());
        });
    }

    private void deleteAllSpeakingRecordings(UUID userId) {
        while (true) {
            List<JdbcDeletedUserStore.SpeakingSubmissionRef> batch =
                    store.findSpeakingSubmissionsBatch(userId, FILE_BATCH_SIZE);
            if (batch.isEmpty()) return;

            ObjectStorage storage = storageProvider.getIfAvailable();
            if (storage == null) {
                throw new IllegalStateException("STORAGE_UNAVAILABLE");
            }
            List<UUID> clearedIds = new ArrayList<>();
            RuntimeException firstFailure = null;
            for (var ref : batch) {
                if (deleteObjectOrNotFound(storage, recordingProperties.getBucket(), ref.mediaReference(), userId)) {
                    clearedIds.add(ref.id());
                } else if (firstFailure == null) {
                    firstFailure = new IllegalStateException("STORAGE_DELETE_FAILED");
                }
            }
            if (!clearedIds.isEmpty()) {
                transaction.executeWithoutResult(status -> store.clearMediaReferenceByIds(clearedIds));
            }
            if (firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private boolean deleteObjectOrNotFound(ObjectStorage storage, String bucket, String key, UUID userId) {
        try {
            storage.delete(bucket, key);
            return true;
        } catch (StorageException storageFailure) {
            if (storageFailure.reason() == StorageException.Reason.NOT_FOUND) {
                return true;
            }
            log.warn("Speaking recording deletion failed: userId={}, errorCode={}", userId, storageFailure.reason());
            return false;
        } catch (RuntimeException failure) {
            log.warn("Speaking recording deletion failed: userId={}, errorCode={}",
                    userId, failure.getClass().getSimpleName());
            return false;
        }
    }

    private void handleFailure(JdbcDeletedUserStore.Job job, RuntimeException failure) {
        String errorCode = failure instanceof StorageException se
                ? se.reason().name()
                : failure.getClass().getSimpleName();
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
