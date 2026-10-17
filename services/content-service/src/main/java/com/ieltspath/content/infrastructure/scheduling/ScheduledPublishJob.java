package com.ieltspath.content.infrastructure.scheduling;

import com.ieltspath.content.application.usecase.RecordScheduledPublicationFailureUseCase;
import com.ieltspath.content.application.usecase.ScheduledPackagePublicationProcessor;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.exception.QuestionAlreadyUsedException;
import com.ieltspath.content.domain.exception.QuestionPurposeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ScheduledPublishJob {
    private static final Logger log = LoggerFactory.getLogger(ScheduledPublishJob.class);
    private static final long ADVISORY_LOCK_KEY = 261017008L;

    private final JdbcTemplate jdbc;
    private final PostgresAdvisoryLock advisoryLock;
    private final ScheduledPackagePublicationProcessor processor;
    private final RecordScheduledPublicationFailureUseCase recordFailure;

    public ScheduledPublishJob(JdbcTemplate jdbc, PostgresAdvisoryLock advisoryLock,
                               ScheduledPackagePublicationProcessor processor,
                               RecordScheduledPublicationFailureUseCase recordFailure) {
        this.jdbc = jdbc;
        this.advisoryLock = advisoryLock;
        this.processor = processor;
        this.recordFailure = recordFailure;
    }

    @Scheduled(fixedRateString = "${content.packages.scheduled-publish.fixed-rate-ms:60000}")
    public void runScheduled() {
        runOnce();
    }

    public boolean runOnce() {
        return advisoryLock.runIfAcquired(ADVISORY_LOCK_KEY, this::publishDueVersions);
    }

    private void publishDueVersions() {
        List<DueVersion> due = jdbc.query("""
                SELECT package_id, id
                FROM content_package_versions
                WHERE status='DRAFT' AND publish_at <= CURRENT_TIMESTAMP
                ORDER BY publish_at, id
                """, (rs, row) -> new DueVersion(rs.getObject("package_id", UUID.class),
                rs.getObject("id", UUID.class)));
        for (DueVersion item : due) {
            try {
                processor.publish(item.packageId(), item.versionId());
            } catch (RuntimeException failure) {
                String code = safeErrorCode(failure);
                try {
                    recordFailure.execute(item.packageId(), item.versionId(), code);
                    log.warn("Scheduled package publication failed for version {} with {}", item.versionId(), code);
                } catch (RuntimeException persistenceFailure) {
                    log.error("Could not record scheduled publication failure for version {}", item.versionId());
                }
            }
        }
    }

    private static String safeErrorCode(RuntimeException failure) {
        if (failure instanceof QuestionAlreadyUsedException) return "QUESTION_ALREADY_USED";
        if (failure instanceof QuestionPurposeMismatchException) return "QUESTION_PURPOSE_MISMATCH";
        if (failure instanceof InvalidPackageLessonException) return "INVALID_PACKAGE_LESSON";
        if (failure instanceof InvalidContentStateException) return "INVALID_CONTENT_STATE";
        return "PUBLISH_FAILED";
    }

    private record DueVersion(UUID packageId, UUID versionId) {}
}
