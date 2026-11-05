package com.ieltspath.library.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.StoredImageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadCatalogCoverUseCase {
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"};

    private final FlashcardImageStorage storage;

    public StoredImageResult execute(UUID userId, String declaredContentType, byte[] fileBytes) {
        UploadPolicy policy = UploadPolicy.of(MAX_IMAGE_SIZE, ALLOWED_IMAGE_TYPES);
        String contentType = policy.check(fileBytes, declaredContentType);
        String extension = ContentTypes.extensionFor(contentType);
        String key = ObjectKeys.forUser("catalog-cover", userId, extension);
        return storage.store(key, fileBytes, contentType);
    }
}
