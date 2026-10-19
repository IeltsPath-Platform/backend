package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.community.application.port.CommunityStoragePort;
import com.ieltspath.community.application.result.StoredMediaResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommunityStorageAdapter implements CommunityStoragePort {

    private final ObjectStorage objectStorage;
    private final String bucket;

    public CommunityStorageAdapter(
            ObjectStorage objectStorage,
            @Value("${community.media.bucket:community}") String bucket
    ) {
        this.objectStorage = objectStorage;
        this.bucket = bucket;
    }

    @Override
    public StoredMediaResult storeImage(String key, byte[] content, String contentType) {
        try {
            StoredObject stored = objectStorage.put(bucket, key, content, contentType);
            String url = objectStorage.publicUrl(bucket, key);
            return new StoredMediaResult(url, stored.key(), stored.contentType(), stored.sizeBytes());
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
        return objectStorage.publicUrl(bucket, key);
    }

    @Override
    public void delete(String key) {
        objectStorage.delete(bucket, key);
    }
}
