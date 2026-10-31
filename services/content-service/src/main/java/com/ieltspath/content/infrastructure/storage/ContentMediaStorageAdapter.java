package com.ieltspath.content.infrastructure.storage;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import com.ieltspath.content.application.port.ContentMediaStorage;
import com.ieltspath.content.application.result.StoredMediaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stores author-uploaded audio in the media bucket. Without storage credentials every call fails as unavailable. */
@Component
public class ContentMediaStorageAdapter implements ContentMediaStorage {

    private static final Logger log = LoggerFactory.getLogger(ContentMediaStorageAdapter.class);

    private final ObjectStorage objectStorage;
    private final String bucket;

    @Autowired
    public ContentMediaStorageAdapter(ObjectProvider<ObjectStorage> objectStorageProvider,
                                      @Value("${content.media.bucket:media}") String bucket) {
        this(objectStorageProvider.getIfAvailable(UnavailableObjectStorage::new), bucket);
    }

    public ContentMediaStorageAdapter(ObjectStorage objectStorage, String bucket) {
        this.objectStorage = objectStorage != null ? objectStorage : new UnavailableObjectStorage();
        this.bucket = bucket;
    }

    @Override
    public StoredMediaResult store(String key, byte[] content, String contentType) {
        try {
            StoredObject stored = objectStorage.put(bucket, key, content, contentType);
            return new StoredMediaResult(stored.key(), publicUrl(stored.key()), stored.contentType(),
                    stored.sizeBytes());
        } catch (StorageException failure) {
            if (failure.outcomeUnknown()) {
                try {
                    objectStorage.delete(bucket, key);
                } catch (RuntimeException cleanupFailed) {
                    log.warn("Cleanup after an uncertain audio upload failed: ref={}", StorageException.ref(bucket, key));
                }
            }
            throw failure;
        }
    }

    private String publicUrl(String key) {
        try {
            return objectStorage.publicUrl(bucket, key);
        } catch (RuntimeException unresolvable) {
            return null;
        }
    }
}
