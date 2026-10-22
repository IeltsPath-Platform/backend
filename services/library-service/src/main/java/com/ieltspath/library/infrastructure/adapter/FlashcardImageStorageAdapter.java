package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class FlashcardImageStorageAdapter implements FlashcardImageStorage {

    private static final Logger log = LoggerFactory.getLogger(FlashcardImageStorageAdapter.class);

    private final ObjectStorage objectStorage;
    private final String bucket;

    @org.springframework.beans.factory.annotation.Autowired
    public FlashcardImageStorageAdapter(
            ObjectProvider<ObjectStorage> objectStorageProvider,
            @Value("${library.flashcard-images.bucket:flashcards}") String bucket
    ) {
        this(objectStorageProvider.getIfAvailable(UnavailableObjectStorage::new), bucket);
    }

    public FlashcardImageStorageAdapter(ObjectStorage objectStorage, String bucket) {
        this.objectStorage = objectStorage != null ? objectStorage : new UnavailableObjectStorage();
        this.bucket = bucket;
    }

    @Override
    public StoredImageResult store(String key, byte[] content, String contentType) {
        try {
            StoredObject stored = objectStorage.put(bucket, key, content, contentType);
            String url = publicUrl(key);
            return new StoredImageResult(stored.key(), url, stored.contentType(), stored.sizeBytes());
        } catch (StorageException e) {
            if (e.outcomeUnknown()) {
                try {
                    objectStorage.delete(bucket, key);
                } catch (Exception ignored) {
                    // best-effort cleanup on uncertain outcome
                }
            }
            throw e;
        }
    }

    @Override
    public String publicUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return objectStorage.publicUrl(bucket, key);
        } catch (Exception e) {
            log.debug("Could not resolve publicUrl for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return objectStorage.exists(bucket, key);
    }

    @Override
    public void delete(String key) {
        if (key != null && !key.isBlank()) {
            try {
                objectStorage.delete(bucket, key);
            } catch (Exception e) {
                log.warn("Failed to delete object from storage: {}", e.getMessage());
            }
        }
    }

    @Override
    public void deleteAfterCommit(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete(key);
                }
            });
        } else {
            delete(key);
        }
    }
}
