package com.ieltspath.library.infrastructure.scheduler;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.library.infrastructure.persistence.JdbcDeletedUserStore;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Claims pending/expired user deletion jobs with a lease and purges personal library data and flashcard images. */
@Component
public class UserDeletionJobWorker {
    private static final Logger log = LoggerFactory.getLogger(UserDeletionJobWorker.class);
    private static final int FILE_BATCH_SIZE = 100;

    private final JdbcDeletedUserStore store;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final String flashcardsBucket;
    private final TransactionTemplate transaction;
    private final long leaseSeconds;
    private final int maxAttempts;

    public UserDeletionJobWorker(JdbcDeletedUserStore store,
                                 ObjectProvider<ObjectStorage> storageProvider,
                                 @Value("${library.flashcard-media.bucket:flashcards}") String flashcardsBucket,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${library.user-deletion.lease-seconds:300}") long leaseSeconds,
                                 @Value("${library.user-deletion.max-attempts:10}") int maxAttempts) {
        this.store = store;
        this.storageProvider = storageProvider;
        this.flashcardsBucket = flashcardsBucket;
        this.transaction = new TransactionTemplate(transactionManager);
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${library.user-deletion.poll-delay-ms:10000}")
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
        deleteMediaInBatches(userId,
                () -> store.findFlashcardImagesBatch(userId, FILE_BATCH_SIZE),
                store::clearFlashcardImageKeys);
        deleteMediaInBatches(userId,
                () -> store.findDeckCoverImagesBatch(userId, FILE_BATCH_SIZE),
                store::clearDeckCoverImageKeys);
        transaction.executeWithoutResult(status -> {
            store.deletePersonalLibraryData(userId);
            store.markJobDone(job.eventId());
        });
    }

    private void deleteMediaInBatches(UUID userId,
                                      Supplier<List<JdbcDeletedUserStore.MediaRef>> batchSupplier,
                                      Consumer<List<UUID>> clearKeys) {
        while (true) {
            List<JdbcDeletedUserStore.MediaRef> batch = batchSupplier.get();
            if (batch.isEmpty()) return;

            ObjectStorage storage = storageProvider.getIfAvailable();
            if (storage == null) {
                throw new IllegalStateException("STORAGE_UNAVAILABLE");
            }
            List<UUID> clearedIds = new ArrayList<>();
            RuntimeException firstFailure = null;
            for (var ref : batch) {
                if (deleteObjectOrNotFound(storage, flashcardsBucket, ref.key(), userId)) {
                    clearedIds.add(ref.id());
                } else if (firstFailure == null) {
                    firstFailure = new IllegalStateException("STORAGE_DELETE_FAILED");
                }
            }
            if (!clearedIds.isEmpty()) {
                transaction.executeWithoutResult(status -> clearKeys.accept(clearedIds));
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
            log.warn("Flashcard image deletion failed: userId={}, errorCode={}", userId, storageFailure.reason());
            return false;
        } catch (RuntimeException failure) {
            log.warn("Flashcard image deletion failed: userId={}, errorCode={}",
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
