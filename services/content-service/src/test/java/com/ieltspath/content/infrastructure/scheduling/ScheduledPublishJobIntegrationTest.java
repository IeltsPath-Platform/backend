package com.ieltspath.content.infrastructure.scheduling;

import com.ieltspath.content.application.usecase.RecordScheduledPublicationFailureUseCase;
import com.ieltspath.content.application.usecase.ScheduledPackagePublicationProcessor;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
class ScheduledPublishJobIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static DataSource dataSource;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        dataSource = source;
        jdbc = new JdbcTemplate(source);
    }

    @BeforeEach
    void clearScheduledRows() {
        jdbc.update("DELETE FROM content_packages WHERE code LIKE 'SCHEDULED-TEST-%'");
    }

    @Test
    void parallelRunsPublishADueVersionOnlyOnceUnderTheDatabaseLock() throws Exception {
        DueIds due = insertDueDraft("SCHEDULED-TEST-LOCK");
        CountDownLatch publishing = new CountDownLatch(1);
        CountDownLatch continuePublish = new CountDownLatch(1);
        ScheduledPackagePublicationProcessor processor = mock(ScheduledPackagePublicationProcessor.class);
        doAnswer(invocation -> {
            publishing.countDown();
            assertThat(continuePublish.await(5, TimeUnit.SECONDS)).isTrue();
            jdbc.update("UPDATE content_package_versions SET status='PUBLISHED', publish_at=NULL WHERE id=?",
                    due.versionId());
            return null;
        }).when(processor).publish(due.packageId(), due.versionId());

        ScheduledPublishJob job = job(processor, mock(RecordScheduledPublicationFailureUseCase.class));
        try (var executor = Executors.newSingleThreadExecutor()) {
            var firstRun = executor.submit(job::runOnce);
            assertThat(publishing.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(job.runOnce()).isFalse();
            continuePublish.countDown();
            assertThat(firstRun.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            continuePublish.countDown();
        }

        verify(processor, times(1)).publish(due.packageId(), due.versionId());
        assertThat(status(due.versionId())).isEqualTo("PUBLISHED");
    }

    @Test
    void validationFailureLeavesDraftAndStoresOnlyASafeCode() {
        DueIds due = insertDueDraft("SCHEDULED-TEST-FAILURE");
        ScheduledPackagePublicationProcessor processor = mock(ScheduledPackagePublicationProcessor.class);
        doThrow(new InvalidContentStateException("private question content must not be stored"))
                .when(processor).publish(due.packageId(), due.versionId());
        RecordScheduledPublicationFailureUseCase recordFailure = mock(RecordScheduledPublicationFailureUseCase.class);
        doAnswer(invocation -> {
            jdbc.update("UPDATE content_package_versions SET last_publish_error=? WHERE id=?",
                    invocation.getArgument(2), due.versionId());
            return null;
        }).when(recordFailure).execute(eq(due.packageId()), eq(due.versionId()), any());

        job(processor, recordFailure).runOnce();

        assertThat(status(due.versionId())).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT last_publish_error FROM content_package_versions WHERE id=?",
                String.class, due.versionId())).isEqualTo("INVALID_CONTENT_STATE");
        verify(recordFailure).execute(due.packageId(), due.versionId(), "INVALID_CONTENT_STATE");
    }

    private ScheduledPublishJob job(ScheduledPackagePublicationProcessor processor,
                                    RecordScheduledPublicationFailureUseCase recordFailure) {
        return new ScheduledPublishJob(jdbc, new PostgresAdvisoryLock(dataSource), processor, recordFailure);
    }

    private DueIds insertDueDraft(String suffix) {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,status) VALUES (?,?,?,?, 'DRAFT')",
                packageId, suffix + "-" + packageId, "Scheduled test package", "PRACTICE_SET");
        jdbc.update("""
                INSERT INTO content_package_versions(id,package_id,version_number,status,publish_at)
                VALUES (?,?,1,'DRAFT',CURRENT_TIMESTAMP - INTERVAL '1 second')
                """, versionId, packageId);
        return new DueIds(packageId, versionId);
    }

    private String status(UUID versionId) {
        return jdbc.queryForObject("SELECT status FROM content_package_versions WHERE id=?", String.class, versionId);
    }

    private record DueIds(UUID packageId, UUID versionId) {}
}
