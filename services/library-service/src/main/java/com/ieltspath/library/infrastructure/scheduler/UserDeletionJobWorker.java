package com.ieltspath.library.infrastructure.scheduler;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.library.infrastructure.persistence.JdbcDeletedUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

/** Claims pending user deletion jobs with SKIP LOCKED and purges personal library data and flashcard images. */
@Component
public class UserDeletionJobWorker {
    private static final Logger log = LoggerFactory.getLogger(UserDeletionJobWorker.class);
    private final JdbcDeletedUserStore store;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final String flashcardsBucket;
    private final TransactionTemplate transaction;

    public UserDeletionJobWorker(JdbcDeletedUserStore store,
                                 ObjectProvider<ObjectStorage> storageProvider,
                                 @Value("${library.flashcard-media.bucket:flashcards}") String flashcardsBucket,
                                 PlatformTransactionManager transactionManager) {
        this.store = store;
        this.storageProvider = storageProvider;
        this.flashcardsBucket = flashcardsBucket;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${library.user-deletion.poll-delay-ms:10000}")
    public void processPending() {
        List<JdbcDeletedUserStore.Job> claimed = transaction.execute(status -> store.claimPendingJobs(5));
        if (claimed == null || claimed.isEmpty()) return;
        for (var job : claimed) {
            purgeUser(job);
        }
    }

    public void purgeUser(JdbcDeletedUserStore.Job job) {
        UUID userId = job.userId();
        deleteFlashcardImages(userId);
        transaction.executeWithoutResult(status -> {
            store.deletePersonalLibraryData(userId);
            store.markJobDone(job.eventId());
        });
    }

    private void deleteFlashcardImages(UUID userId) {
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) return;
        for (String key : store.findFlashcardImageKeys(userId)) {
            try {
                storage.delete(flashcardsBucket, key);
            } catch (RuntimeException failure) {
                log.warn("Could not delete flashcard image during user deletion: userId={}, errorType={}",
                        userId, failure.getClass().getSimpleName());
            }
        }
    }
}
