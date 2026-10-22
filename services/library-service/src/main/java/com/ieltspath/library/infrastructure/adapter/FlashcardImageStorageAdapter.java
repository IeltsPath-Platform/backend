package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FlashcardImageStorageAdapter implements FlashcardImageStorage {

    private final ObjectStorage objectStorage;
    private final String bucket;

    public FlashcardImageStorageAdapter(
            ObjectStorage objectStorage,
            @Value("${library.flashcard-images.bucket:flashcards}") String bucket
    ) {
        this.objectStorage = objectStorage;
        this.bucket = bucket;
    }

    @Override
    public StoredImageResult store(String key, byte[] content, String contentType) {
        try {
            StoredObject stored = objectStorage.put(bucket, key, content, contentType);
            String url = objectStorage.publicUrl(bucket, key);
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
        return objectStorage.publicUrl(bucket, key);
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
            objectStorage.delete(bucket, key);
        }
    }
}
