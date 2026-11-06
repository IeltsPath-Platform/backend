package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import com.ieltspath.library.application.port.CatalogMediaStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stores catalog media in the public media bucket and returns the URL a browser can open. */
@Component
public class CatalogMediaStorageAdapter implements CatalogMediaStorage {

    private static final Logger log = LoggerFactory.getLogger(CatalogMediaStorageAdapter.class);

    private final ObjectStorage objectStorage;
    private final String bucket;

    @Autowired
    public CatalogMediaStorageAdapter(
            ObjectProvider<ObjectStorage> objectStorageProvider,
            @Value("${library.catalog-media.bucket:media}") String bucket
    ) {
        this(objectStorageProvider.getIfAvailable(UnavailableObjectStorage::new), bucket);
    }

    public CatalogMediaStorageAdapter(ObjectStorage objectStorage, String bucket) {
        this.objectStorage = objectStorage != null ? objectStorage : new UnavailableObjectStorage();
        this.bucket = bucket;
    }

    @Override
    public StoredImageResult store(String key, byte[] content, String contentType) {
        try {
            StoredObject stored = objectStorage.put(bucket, key, content, contentType);
            return new StoredImageResult(stored.key(), publicUrl(key), stored.contentType(), stored.sizeBytes());
        } catch (StorageException e) {
            if (e.outcomeUnknown()) {
                try {
                    objectStorage.delete(bucket, key);
                } catch (Exception ignored) {
                    // best-effort cleanup when the upload outcome is unknown
                }
            }
            throw e;
        }
    }

    @Override
    public boolean exists(String key) {
        return objectStorage.exists(bucket, key);
    }

    private String publicUrl(String key) {
        try {
            return objectStorage.publicUrl(bucket, key);
        } catch (Exception e) {
            log.debug("Could not build a public URL for catalog media {}: {}", key, e.getMessage());
            return null;
        }
    }
}
