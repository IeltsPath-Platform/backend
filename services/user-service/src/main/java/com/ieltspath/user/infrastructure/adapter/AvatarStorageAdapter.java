package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.user.application.port.AvatarStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AvatarStorageAdapter implements AvatarStoragePort {
    private final ObjectStorage storage;
    private final String bucket;

    public AvatarStorageAdapter(ObjectStorage storage,
                                @Value("${app.user.avatar.bucket:avatars}") String bucket) {
        this.storage = storage;
        this.bucket = bucket;
    }

    @Override
    public String put(String key, byte[] bytes, String contentType) {
        try {
            storage.put(bucket, key, bytes, contentType);
            return storage.publicUrl(bucket, key);
        } catch (StorageException e) {
            if (e.outcomeUnknown()) {
                try { storage.delete(bucket, key); } catch (Exception ignored) { }
            }
            throw e;
        }
    }

    @Override public String publicUrl(String key) { return storage.publicUrl(bucket, key); }
    @Override public void delete(String key) { storage.delete(bucket, key); }
}
