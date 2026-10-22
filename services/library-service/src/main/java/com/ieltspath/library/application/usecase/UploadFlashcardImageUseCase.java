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
public class UploadFlashcardImageUseCase {

    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final String IMAGE_PREFIX = "images";
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/png",
            "image/jpeg",
            "image/webp"
    };

    private final FlashcardImageStorage storage;

    public StoredImageResult execute(UUID userId, String declaredContentType, byte[] fileBytes) {
        UploadPolicy policy = UploadPolicy.of(MAX_IMAGE_SIZE, ALLOWED_IMAGE_TYPES);
        String validatedMime = policy.check(fileBytes, declaredContentType);
        String ext = ContentTypes.extensionFor(validatedMime);
        String key = ObjectKeys.forUser(IMAGE_PREFIX, userId, ext);

        return storage.store(key, fileBytes, validatedMime);
    }
}
