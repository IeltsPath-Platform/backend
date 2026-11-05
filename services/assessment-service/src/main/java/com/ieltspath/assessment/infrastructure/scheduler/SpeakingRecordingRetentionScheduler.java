package com.ieltspath.assessment.infrastructure.scheduler;

import com.ieltspath.assessment.application.usecase.PurgeExpiredSpeakingRecordingsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SpeakingRecordingRetentionScheduler {
    private static final Logger log = LoggerFactory.getLogger(SpeakingRecordingRetentionScheduler.class);
    private final PurgeExpiredSpeakingRecordingsUseCase purge;

    public SpeakingRecordingRetentionScheduler(PurgeExpiredSpeakingRecordingsUseCase purge) {
        this.purge = purge;
    }

    @Scheduled(fixedDelayString = "${assessment.speaking.retention-poll-interval:PT1H}",
            initialDelayString = "${assessment.speaking.retention-initial-delay:PT1H}")
    public void purgeExpiredRecordings() {
        try {
            int count = purge.execute(Instant.now());
            if (count > 0) log.info("Purged {} expired Speaking recording(s)", count);
        } catch (RuntimeException failure) {
            log.warn("Speaking recording retention check failed");
        }
    }
}
