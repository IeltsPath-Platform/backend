package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/** Removes expired recordings and their transcripts, analyses, grades and rubric snapshots after the retention window. */
@Component
@EnableScheduling
public class SpeakingRecordingRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(SpeakingRecordingRetentionJob.class);
    private final SpeakingSubmissionRepository submissions;
    private final ObjectStorage storage;
    private final SpeakingPracticeSettings settings;
    private final Clock clock = Clock.systemUTC();

    public SpeakingRecordingRetentionJob(SpeakingSubmissionRepository submissions, @Lazy ObjectStorage storage,
                                         SpeakingPracticeSettings settings) {
        this.submissions = submissions;
        this.storage = storage;
        this.settings = settings;
    }

    @Scheduled(cron = "${learning.speaking.retention-cron:0 30 3 * * *}", zone = "UTC")
    public void purgeBatch() {
        Instant cutoff = Instant.now(clock).minusSeconds(settings.retentionDays() * 86_400L);
        for (SpeakingSubmission submission : submissions.findExpiredBefore(cutoff, 100)) {
            purge(submission, cutoff);
        }
    }

    private void purge(SpeakingSubmission submission, Instant cutoff) {
        try {
            for (var recording : submission.recordings()) {
                if (recording.objectKey() != null) storage.delete(settings.bucket(), recording.objectKey());
            }
            submissions.deleteExpired(submission.id(), cutoff);
        } catch (RuntimeException exception) {
            log.warn("Expired Speaking data cleanup deferred: submissionId={}, reason={}", submission.id(),
                    exception.getClass().getSimpleName());
        }
    }
}
