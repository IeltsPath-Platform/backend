package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StoredObject;
import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UploadSpeakingRecordingUseCase {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final UploadPolicy POLICY = UploadPolicy.of(MAX_BYTES,
            "audio/webm", "audio/mp4", "audio/ogg", "audio/mpeg", "audio/wav");

    private final ObjectStorage storage;
    private final SpeakingRecordingProperties properties;

    public UploadSpeakingRecordingUseCase(@Lazy ObjectStorage storage, SpeakingRecordingProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    public RecordingResult execute(UUID userId, String declaredContentType, byte[] content) {
        String contentType = POLICY.check(content, declaredContentType);
        String key = ObjectKeys.forUser("speaking", userId, ContentTypes.extensionFor(contentType));
        StoredObject stored = storage.put(properties.getBucket(), key, content, contentType);
        return new RecordingResult(stored.key(), stored.contentType(), stored.sizeBytes());
    }

    public record RecordingResult(String mediaReference, String contentType, long sizeBytes) { }
}
