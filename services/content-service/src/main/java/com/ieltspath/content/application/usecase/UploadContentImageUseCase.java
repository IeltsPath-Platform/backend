package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.content.application.port.ContentMediaStorage;
import com.ieltspath.content.application.result.StoredMediaResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Validates an author-uploaded image from its bytes and stores it under {@code content-image/{userId}/}. */
@Service
public class UploadContentImageUseCase {
    static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    static final String KEY_PREFIX = "content-image";
    private static final UploadPolicy POLICY = UploadPolicy.of(MAX_IMAGE_BYTES,
            "image/jpeg", "image/png", "image/webp");

    private final ContentMediaStorage storage;

    public UploadContentImageUseCase(ContentMediaStorage storage) {
        this.storage = storage;
    }

    public StoredMediaResult execute(UUID userId, String declaredContentType, byte[] content) {
        String contentType = POLICY.check(content, declaredContentType);
        String key = ObjectKeys.forUser(KEY_PREFIX, userId, ContentTypes.extensionFor(contentType));
        return storage.store(key, content, contentType);
    }
}
