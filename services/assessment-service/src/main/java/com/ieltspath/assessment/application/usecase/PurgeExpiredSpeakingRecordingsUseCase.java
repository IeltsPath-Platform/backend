package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.SpeakingRecordingStore;
import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.key.ObjectKeys;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PurgeExpiredSpeakingRecordingsUseCase {
    private static final Logger log = LoggerFactory.getLogger(PurgeExpiredSpeakingRecordingsUseCase.class);
    private static final int BATCH_SIZE = 100;

    private final SpeakingRecordingStore recordings;
    private final ObjectStorage storage;
    private final SpeakingRecordingProperties properties;

    public PurgeExpiredSpeakingRecordingsUseCase(SpeakingRecordingStore recordings, @Lazy ObjectStorage storage,
                                                 SpeakingRecordingProperties properties) {
        this.recordings = recordings;
        this.storage = storage;
        this.properties = properties;
    }

    public int execute(Instant now) {
        var expired = recordings.findExpired(now.minusSeconds(properties.getRetentionDays() * 86_400L), BATCH_SIZE);
        int deleted = 0;
        for (var recording : expired) {
            try {
                ObjectKeys.requireOwnedBy(recording.mediaReference(), "speaking", recording.userId());
                storage.delete(properties.getBucket(), recording.mediaReference());
                recordings.markDeleted(recording.id(), now);
                deleted++;
            } catch (RuntimeException failure) {
                log.warn("Could not purge an expired Speaking recording");
            }
        }
        return deleted;
    }
}
