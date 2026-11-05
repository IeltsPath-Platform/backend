package com.ieltspath.content.infrastructure.storage;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import com.ieltspath.content.application.port.LessonDocumentFiles;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Keeps lesson document files in the premium bucket. Without storage credentials every call fails as unavailable. */
@Component
public class LessonDocumentFilesAdapter implements LessonDocumentFiles {
    private final ObjectStorage storage;
    private final String bucket;
    private final Duration urlTtl;

    @Autowired
    public LessonDocumentFilesAdapter(ObjectProvider<ObjectStorage> storage,
                                      @Value("${content.media.premium-bucket:media-premium}") String bucket,
                                      @Value("${content.media.premium-url-ttl-seconds:900}") long ttlSeconds) {
        this(storage.getIfAvailable(UnavailableObjectStorage::new), bucket, Duration.ofSeconds(ttlSeconds));
    }

    public LessonDocumentFilesAdapter(ObjectStorage storage, String bucket, Duration urlTtl) {
        if (urlTtl.toSeconds() < 1 || urlTtl.toSeconds() > 900) {
            throw new IllegalArgumentException("Premium media URL TTL must be between 1 and 900 seconds");
        }
        this.storage = storage != null ? storage : new UnavailableObjectStorage();
        this.bucket = bucket;
        this.urlTtl = urlTtl;
    }

    @Override
    public boolean exists(String fileKey) {
        return storage.exists(bucket, fileKey);
    }

    @Override
    public void delete(String fileKey) {
        storage.delete(bucket, fileKey);
    }

    @Override
    public String signedUrl(String fileKey) {
        return storage.signedDownloadUrl(bucket, fileKey, urlTtl);
    }
}
