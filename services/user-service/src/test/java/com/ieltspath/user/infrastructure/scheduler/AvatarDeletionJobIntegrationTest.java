package com.ieltspath.user.infrastructure.scheduler;

import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.infrastructure.adapter.JdbcAccountDeletionCleanupAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** The avatar of a deleted account is queued with the deletion and removed by the job, retried until storage agrees. */
@Testcontainers(disabledWithoutDocker = true)
class AvatarDeletionJobIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM pending_avatar_deletions");
    }

    private static AvatarDeletionJob job(AvatarStoragePort storage, long leaseSeconds) {
        return new AvatarDeletionJob(jdbc,
                new StaticListableBeanFactory(Map.of("storage", storage)).getBeanProvider(AvatarStoragePort.class),
                leaseSeconds, 2, 360);
    }

    @Test
    void removesTheObjectAndForgetsIt() {
        UUID userId = UUID.randomUUID();
        new JdbcAccountDeletionCleanupAdapter(jdbc).queueAvatarDeletion(userId, "images/" + userId + "/a.png");
        AvatarStoragePort storage = mock(AvatarStoragePort.class);

        job(storage, 300).run();

        verify(storage).delete("images/" + userId + "/a.png");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_avatar_deletions", Integer.class)).isZero();
    }

    @Test
    void neverGivesUpOnAFailingObjectAndWaitsLongerEachTime() {
        UUID userId = UUID.randomUUID();
        String key = "images/" + userId + "/b.png";
        new JdbcAccountDeletionCleanupAdapter(jdbc).queueAvatarDeletion(userId, key);
        AvatarStoragePort storage = mock(AvatarStoragePort.class);
        doThrow(new StorageException("delete", StorageException.Reason.UNAVAILABLE, 503, true, "ref")).when(storage).delete(key);

        for (int attempt = 1; attempt <= 12; attempt++) {
            job(storage, 300).run();
            assertThat(jdbc.queryForMap("SELECT status, attempts, last_error FROM pending_avatar_deletions"))
                    .containsEntry("status", "PENDING").containsEntry("attempts", attempt)
                    .containsEntry("last_error", "UNAVAILABLE");
            // Make the row due again instead of waiting for the back-off.
            jdbc.update("UPDATE pending_avatar_deletions SET next_attempt_at = CURRENT_TIMESTAMP - interval '1 second'");
        }
        verify(storage, org.mockito.Mockito.times(12)).delete(key);
    }

    @Test
    void waitsBeforeRetryingAFailedObject() {
        UUID userId = UUID.randomUUID();
        String key = "images/" + userId + "/d.png";
        new JdbcAccountDeletionCleanupAdapter(jdbc).queueAvatarDeletion(userId, key);
        AvatarStoragePort storage = mock(AvatarStoragePort.class);
        doThrow(new StorageException("delete", StorageException.Reason.UNAVAILABLE, 503, true, "ref")).when(storage).delete(key);

        job(storage, 300).run();
        job(storage, 300).run();

        verify(storage, org.mockito.Mockito.times(1)).delete(key);
        assertThat(jdbc.queryForObject("SELECT next_attempt_at > CURRENT_TIMESTAMP FROM pending_avatar_deletions",
                Boolean.class)).isTrue();
    }

    @Test
    void treatsAnObjectAlreadyGoneAsDoneOnceStorageConfirmsIt() {
        UUID userId = UUID.randomUUID();
        String key = "images/" + userId + "/c.png";
        new JdbcAccountDeletionCleanupAdapter(jdbc).queueAvatarDeletion(userId, key);
        AvatarStoragePort storage = mock(AvatarStoragePort.class);
        doThrow(new StorageException("delete", StorageException.Reason.NOT_FOUND, 404, false, "ref")).when(storage).delete(key);
        org.mockito.Mockito.when(storage.exists(key)).thenReturn(false);

        job(storage, 300).run();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_avatar_deletions", Integer.class)).isZero();
    }

    @Test
    void keepsTheObjectWhenA404ComesFromAWrongBucketOrKey() {
        UUID userId = UUID.randomUUID();
        String key = "images/" + userId + "/e.png";
        new JdbcAccountDeletionCleanupAdapter(jdbc).queueAvatarDeletion(userId, key);
        AvatarStoragePort storage = mock(AvatarStoragePort.class);
        doThrow(new StorageException("delete", StorageException.Reason.NOT_FOUND, 404, false, "ref")).when(storage).delete(key);
        // The existence check confirms the bucket first and fails when it is not reachable.
        doThrow(new StorageException("exists", StorageException.Reason.NOT_FOUND, 404, false, "ref")).when(storage).exists(key);

        job(storage, 300).run();

        assertThat(jdbc.queryForMap("SELECT status, attempts, last_error FROM pending_avatar_deletions"))
                .containsEntry("status", "PENDING").containsEntry("attempts", 1).containsEntry("last_error", "NOT_FOUND");
    }
}
