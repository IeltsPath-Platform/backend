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
import java.util.List;

/** Removes expired recordings and their transcripts, analyses, grades and rubric snapshots after the retention window. */
@Component
@EnableScheduling
public class SpeakingRecordingRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(SpeakingRecordingRetentionJob.class);
    /** Submissions read and purged at a time; a run repeats until nothing is left or {@code MAX_ROUNDS} is reached. */
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ROUNDS = 50;
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
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<SpeakingSubmission> expired = submissions.findExpiredBefore(cutoff, BATCH_SIZE);
            if (expired.isEmpty()) return;
            int purged = 0;
            for (SpeakingSubmission submission : expired) {
                if (purge(submission, cutoff)) purged++;
            }
            // Rows that cannot be purged come back first in the next read; stop instead of reading them again.
            if (purged == 0) return;
        }
    }

    private boolean purge(SpeakingSubmission submission, Instant cutoff) {
        try {
            for (var recording : submission.recordings()) {
                if (recording.objectKey() != null) storage.delete(settings.bucket(), recording.objectKey());
            }
            return submissions.deleteExpired(submission.id(), cutoff);
        } catch (RuntimeException exception) {
            log.warn("Expired Speaking data cleanup deferred: submissionId={}, reason={}", submission.id(),
                    exception.getClass().getSimpleName());
            return false;
        }
    }
}
