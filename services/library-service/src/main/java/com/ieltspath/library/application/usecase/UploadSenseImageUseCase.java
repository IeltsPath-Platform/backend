package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.library.application.port.CatalogMediaStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import org.springframework.stereotype.Service;

/** A picture for one vocabulary sense, stored under {@code vocabulary/images/} in the public media bucket. */
@Service
public class UploadSenseImageUseCase {
    static final String KEY_PREFIX = "vocabulary/images";
    private static final UploadPolicy POLICY = UploadPolicy.of(2L * 1024 * 1024, "image/jpeg", "image/png", "image/webp");

    private final CatalogMediaStorage storage;

    public UploadSenseImageUseCase(CatalogMediaStorage storage) {
        this.storage = storage;
    }

    public StoredImageResult execute(String declaredContentType, byte[] content) {
        String contentType = POLICY.check(content, declaredContentType);
        return storage.store(ObjectKeys.of(KEY_PREFIX, ContentTypes.extensionFor(contentType)), content, contentType);
    }
}
