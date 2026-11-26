package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.infrastructure.persistence.JdbcDeletedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

/** Claims pending user deletion jobs with SKIP LOCKED and purges learning data and Speaking recordings in batches. */
@Component
public class UserDeletionJobWorker {
    private static final Logger log = LoggerFactory.getLogger(UserDeletionJobWorker.class);
    private static final int BATCH_SIZE = 500;
    private static final List<String> USER_TABLES = List.of(
            "kp_evidence",
            "question_attempt_events",
            "learning_activities",
            "study_activity_intervals",
            "study_daily_totals",
            "study_goal_history",
            "learning_goals",
            "lesson_exercise_submissions",
            "lesson_writing_submissions",
            "review_sets",
            "review_theory_checks",
            "review_items",
            "topic_test_assignments",
            "practice_test_assignments",
            "course_test_assignments",
            "lesson_practice_passes",
            "lesson_progress",
            "topic_progress",
            "course_progress",
            "course_study_activity",
            "learner_course_unlocks",
            "kp_mastery_profiles",
            "assessment_result_versions",
            "remediation_flags",
            "learner_placements",
            "placement_assignments",
            "placement_speaking_jobs",
            "human_review_outcomes",
            "xp_ledger",
            "reward_grants",
            "learner_weekly_snapshots",
            "learner_monthly_snapshots",
            "league_members",
            "learner_league",
            "daily_quests",
            "learner_badges"
    );

    private final JdbcDeletedUserRepository repository;
    private final ObjectProvider<ObjectStorage> storageProvider;
    private final SpeakingPracticeSettings speakingSettings;
    private final TransactionTemplate transaction;

    public UserDeletionJobWorker(JdbcDeletedUserRepository repository,
                                 ObjectProvider<ObjectStorage> storageProvider,
                                 SpeakingPracticeSettings speakingSettings,
                                 PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.storageProvider = storageProvider;
        this.speakingSettings = speakingSettings;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${learning.user-deletion.poll-delay-ms:10000}")
    public void processPending() {
        List<JdbcDeletedUserRepository.Job> claimed = transaction.execute(status -> repository.claimPendingJobs(5));
        if (claimed == null || claimed.isEmpty()) return;
        for (var job : claimed) {
            purgeUser(job);
        }
    }

    public void purgeUser(JdbcDeletedUserRepository.Job job) {
        UUID userId = job.userId();
        deleteSpeakingRecordingsFromStorage(userId);
        transaction.executeWithoutResult(status -> {
            repository.deleteSpeakingRecordingsForUser(userId);
            repository.deleteDiagnosticsForUser(userId);
            repository.deletePracticeSessionsAndAttempts(userId);
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
        transaction.executeWithoutResult(status -> repository.markJobDone(job.eventId()));
    }

    private void deleteSpeakingRecordingsFromStorage(UUID userId) {
        ObjectStorage storage = storageProvider.getIfAvailable();
        if (storage == null) return;
        List<String> keys = repository.findSpeakingRecordingKeys(userId, 1000);
        for (String key : keys) {
            try {
                storage.delete(speakingSettings.bucket(), key);
            } catch (RuntimeException failure) {
                log.warn("Could not delete Speaking recording during user deletion: userId={}, errorType={}",
                        userId, failure.getClass().getSimpleName());
            }
        }
    }
}
