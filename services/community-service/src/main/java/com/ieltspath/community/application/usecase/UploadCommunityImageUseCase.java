package com.ieltspath.community.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.CommunityStoragePort;
import com.ieltspath.community.application.port.MediaPolicy;
import com.ieltspath.community.application.port.MediaRateLimiter;
import com.ieltspath.community.application.result.StoredMediaResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UploadCommunityImageUseCase {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final String IMAGE_PREFIX = MediaPolicy.IMAGE_KEY_PREFIX;
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    };

    private final CommunityStoragePort storagePort;
    private final MediaRateLimiter rateLimiter;

    public StoredMediaResult execute(CommunityActor actor, String declaredContentType, byte[] fileBytes) {
        rateLimiter.checkAndAcquire(actor.userId());

        UploadPolicy policy = UploadPolicy.of(MAX_IMAGE_SIZE, ALLOWED_IMAGE_TYPES);
        String validatedMime = policy.check(fileBytes, declaredContentType);
        String ext = ContentTypes.extensionFor(validatedMime);
        String key = ObjectKeys.forUser(IMAGE_PREFIX, actor.userId(), ext);

        return storagePort.storeImage(key, fileBytes, validatedMime);
    }
}

