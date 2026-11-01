package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Turns the pronunciation audio reference stored on a vocabulary item into the URL a browser can play. The column holds
 * an object key in the public media bucket (for example {@code vocabulary/pronunciation/uk/democracy.mp3}), so moving the
 * files to another bucket or domain changes configuration, not data. A value that is already an {@code https://} URL
 * (older rows, or one an admin pasted) is returned as it is.
 */
@Component
public class VocabularyAudioUrlResolver {

    private static final Logger log = LoggerFactory.getLogger(VocabularyAudioUrlResolver.class);

    private final ObjectStorage objectStorage;
    private final String bucket;

    @Autowired
    public VocabularyAudioUrlResolver(
            ObjectProvider<ObjectStorage> objectStorageProvider,
            @Value("${library.vocabulary-audio.bucket:media}") String bucket
    ) {
        this(objectStorageProvider.getIfAvailable(UnavailableObjectStorage::new), bucket);
    }

    public VocabularyAudioUrlResolver(ObjectStorage objectStorage, String bucket) {
        this.objectStorage = objectStorage != null ? objectStorage : new UnavailableObjectStorage();
        this.bucket = bucket;
    }

    /** @return a playable URL, or null when there is no reference or the storage is not configured */
    public String resolve(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        if (reference.startsWith("https://")) {
            return reference;
        }
        try {
            return objectStorage.publicUrl(bucket, reference);
        } catch (Exception e) {
            log.debug("Could not build a public URL for a vocabulary audio key: {}", e.getMessage());
            return null;
        }
    }
}
