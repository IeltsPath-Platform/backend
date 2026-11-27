package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.infrastructure.persistence.JdbcDeletedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Claims pending/expired user deletion jobs with a lease and purges learning data and Speaking recordings in batches. */
@Component
public class UserDeletionJobWorker {
    private static final Logger log = LoggerFactory.getLogger(UserDeletionJobWorker.class);
    private static final int BATCH_SIZE = 500;
    private static final int FILE_BATCH_SIZE = 100;
    /** Tables with no rows pointing at them, emptied in batches after the linked records are gone. */
    static final List<String> USER_TABLES = List.of(
            "kp_evidence",
            "question_attempt_events",
            "learning_activities",
            "study_activity_intervals",
            "study_daily_totals",
            "study_goal_history",
            "learning_goals",
            "lesson_exercise_submissions",
            "topic_test_assignments",
            "practice_test_assignments",
            "course_test_assignments",
            "lesson_practice_passes",
            "lesson_progress",
            "topic_progress",
            "course_progress",
            "course_outcomes",
            "course_study_activity",
            "learner_course_unlocks",
            "kp_mastery_profiles",
            "assessment_result_versions",
            "remediation_flags",
            "learner_flags",
            "learner_placements",
            "placement_assignments",
            "placement_speaking_jobs",
            "xp_ledger",
            "learner_xp",
            "learner_hearts",
            "learner_streaks",
            "learner_settings",
            "llm_daily_usage",
            "reward_grants",
            "learner_weekly_snapshots",
            "learner_monthly_snapshots",
            "league_members",
            "learner_league",
            "daily_quests",
            "learner_badges"
    );

    /** Tables removed by the linked-record, Speaking and study-session steps; listed for the coverage test. */
    static final List<String> LINKED_TABLES = List.of(
            "diagnostic_sessions", "review_sets", "review_theory_checks", "review_items",
            "practice_sessions", "practice_attempts", "human_review_requests", "human_review_outcomes",
            "lesson_writing_submissions", "speaking_submissions", "study_sessions");
    private final JdbcDeletedUserRepository repository;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final SpeakingPracticeSettings speakingSettings;
    private final TransactionTemplate transaction;
    private final long leaseSeconds;
    private final int maxAttempts;

    public UserDeletionJobWorker(JdbcDeletedUserRepository repository,
                                 ObjectProvider<ObjectStorage> storageProvider,
                                 SpeakingPracticeSettings speakingSettings,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${learning.user-deletion.lease-seconds:300}") long leaseSeconds,
                                 @Value("${learning.user-deletion.max-attempts:10}") int maxAttempts) {
        this.repository = repository;
        this.storageProvider = storageProvider;
        this.speakingSettings = speakingSettings;
        this.transaction = new TransactionTemplate(transactionManager);
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${learning.user-deletion.poll-delay-ms:10000}")
    public void processPending() {
        List<JdbcDeletedUserRepository.Job> claimed = transaction.execute(
                status -> repository.claimPendingJobs(5, leaseSeconds, maxAttempts));
        if (claimed == null || claimed.isEmpty()) return;
        for (var job : claimed) {
            try {
                purgeUser(job);
            } catch (RuntimeException failure) {
                handleFailure(job, failure);
            }
        }
    }

    public void purgeUser(JdbcDeletedUserRepository.Job job) {
        UUID userId = job.userId();
        deleteAllSpeakingRecordings(userId);
        transaction.executeWithoutResult(status -> {
            repository.deleteSpeakingRecordingsForUser(userId);
            repository.deleteLinkedLearningRecords(userId);
            repository.deleteStudyHeartbeatReceipts(userId);
        });
        for (String table : USER_TABLES) {
            int deleted;
            do {
                Integer count = transaction.execute(status ->
                        repository.deleteBatchByUserId(table, "user_id", userId, BATCH_SIZE));
                deleted = count == null ? 0 : count;
            } while (deleted == BATCH_SIZE);
        }
        // Work items name the learner they are about; staff-facing rows go with the learner.
        int removed;
        do {
            Integer count = transaction.execute(status ->
                    repository.deleteBatchByUserId("work_items", "subject_user_id", userId, BATCH_SIZE));
            removed = count == null ? 0 : count;
        } while (removed == BATCH_SIZE);
        transaction.executeWithoutResult(status -> repository.markJobDone(job.eventId()));
    }

    private void deleteAllSpeakingRecordings(UUID userId) {
        while (true) {
            List<JdbcDeletedUserRepository.SpeakingRecordingRef> batch =
                    repository.findSpeakingRecordingsBatch(userId, FILE_BATCH_SIZE);
            if (batch.isEmpty()) return;

            ObjectStorage storage = storageProvider.getIfAvailable();
            if (storage == null) {
                throw new IllegalStateException("STORAGE_UNAVAILABLE");
            }
            List<JdbcDeletedUserRepository.SpeakingRecordingRef> deletedRefs = new ArrayList<>();
            RuntimeException firstFailure = null;
            for (var ref : batch) {
                if (deleteObjectOrNotFound(storage, speakingSettings.bucket(), ref.objectKey(), userId)) {
                    deletedRefs.add(ref);
                } else if (firstFailure == null) {
                    firstFailure = new IllegalStateException("STORAGE_DELETE_FAILED");
                }
            }
            if (!deletedRefs.isEmpty()) {
                transaction.executeWithoutResult(status -> repository.deleteSpeakingRecordings(deletedRefs));
            }
            if (firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private boolean deleteObjectOrNotFound(ObjectStorage storage, String bucket, String key, UUID userId) {
        try {
            storage.delete(bucket, key);
            return true;
        } catch (StorageException storageFailure) {
            if (storageFailure.reason() == StorageException.Reason.NOT_FOUND) {
                return true;
            }
            log.warn("Speaking recording deletion failed: userId={}, errorCode={}", userId, storageFailure.reason());
            return false;
        } catch (RuntimeException failure) {
            log.warn("Speaking recording deletion failed: userId={}, errorCode={}",
                    userId, failure.getClass().getSimpleName());
            return false;
        }
    }

    private void handleFailure(JdbcDeletedUserRepository.Job job, RuntimeException failure) {
        String errorCode = failure instanceof StorageException se
                ? se.reason().name()
                : failure.getClass().getSimpleName();
        if (job.attempts() >= maxAttempts) {
            log.error("User deletion job permanently failed: eventId={}, userId={}, attempts={}, errorCode={}",
                    job.eventId(), job.userId(), job.attempts(), errorCode);
            transaction.executeWithoutResult(status -> repository.markJobFailed(job.eventId(), errorCode));
        } else {
            log.warn("User deletion job will be retried after lease: eventId={}, userId={}, attempts={}, errorCode={}",
                    job.eventId(), job.userId(), job.attempts(), errorCode);
            transaction.executeWithoutResult(status -> repository.recordJobError(job.eventId(), errorCode));
        }
    }
}
