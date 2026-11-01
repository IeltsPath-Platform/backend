package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.content.application.port.ContentMediaStorage;
import com.ieltspath.content.application.result.StoredMediaResult;
import com.ieltspath.content.domain.vo.AccessTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Accepts an audio file from an author and stores it under {@code content-audio/{userId}/}. The file is checked by
 * its bytes, not by the type the client claims. Attaching it to a section is a separate step.
 */
@Service
public class UploadContentAudioUseCase {

    static final long MAX_AUDIO_BYTES = 25L * 1024 * 1024;
    static final String KEY_PREFIX = "content-audio";
    private static final UploadPolicy POLICY = UploadPolicy.of(MAX_AUDIO_BYTES,
            "audio/mpeg", "audio/mp4", "audio/wav", "audio/ogg");

    private final ContentMediaStorage storage;
    private final String premiumBucket;

    @Autowired
    public UploadContentAudioUseCase(ContentMediaStorage storage,
                                    @Value("${content.media.premium-bucket:media-premium}") String premiumBucket) {
        this.storage = storage;
        this.premiumBucket = premiumBucket;
    }

    public UploadContentAudioUseCase(ContentMediaStorage storage) {
        this(storage, "media-premium");
    }

    public StoredMediaResult execute(UUID userId, String declaredContentType, byte[] content) {
        return execute(userId, declaredContentType, content, AccessTier.PUBLIC);
    }

    public StoredMediaResult execute(UUID userId, String declaredContentType, byte[] content, AccessTier tier) {
        String contentType = POLICY.check(content, declaredContentType);
        String key = ObjectKeys.forUser(KEY_PREFIX, userId, ContentTypes.extensionFor(contentType));
        return tier == AccessTier.PREMIUM
                ? storage.store(premiumBucket, key, content, contentType)
                : storage.store(key, content, contentType);
    }
}
