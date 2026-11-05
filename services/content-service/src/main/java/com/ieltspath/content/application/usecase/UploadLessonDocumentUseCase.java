package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.content.application.port.ContentMediaStorage;
import com.ieltspath.content.application.result.StoredMediaResult;
import com.ieltspath.content.domain.aggregate.LessonResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Accepts a PDF from an author and stores it in the private bucket under {@code lesson-document/{userId}/}. The file
 * is checked by its bytes, not by the type the client claims. Attaching it to a lesson is a separate step.
 */
@Service
public class UploadLessonDocumentUseCase {
    static final long MAX_DOCUMENT_BYTES = 20L * 1024 * 1024;
    private static final UploadPolicy POLICY = UploadPolicy.of(MAX_DOCUMENT_BYTES, "application/pdf");

    private final ContentMediaStorage storage;
    private final String bucket;

    @Autowired
    public UploadLessonDocumentUseCase(ContentMediaStorage storage,
                                       @Value("${content.media.premium-bucket:media-premium}") String bucket) {
        this.storage = storage;
        this.bucket = bucket;
    }

    public StoredMediaResult execute(UUID userId, String declaredContentType, byte[] content) {
        String contentType = POLICY.check(content, declaredContentType);
        String key = ObjectKeys.forUser(LessonResource.FILE_KEY_PREFIX, userId, ContentTypes.extensionFor(contentType));
        return storage.store(bucket, key, content, contentType);
    }
}
