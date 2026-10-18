package com.ieltspath.notification.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class NotificationMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test void migrationCreatesOwnerScopedStorageAndEventDeduplication() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("filesystem:src/main/resources/db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID event=UUID.randomUUID(), owner=UUID.randomUUID(), actor=UUID.randomUUID();
        String insert="INSERT INTO notifications(id,source_event_id,recipient_id,actor_id,event_type,created_at) VALUES(?,?,?,?,?,?) ON CONFLICT(source_event_id) DO NOTHING";
        jdbc.update(insert,event,event,owner,actor,"community.user-followed.v1",java.sql.Timestamp.from(Instant.now()));
        jdbc.update(insert,event,event,owner,actor,"community.user-followed.v1",java.sql.Timestamp.from(Instant.now()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?",Integer.class,owner));
    }

    @Test void mentionNotificationsAreUniquePerCommentAndRecipient() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("filesystem:src/main/resources/db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID comment=UUID.randomUUID(), owner=UUID.randomUUID(), actor=UUID.randomUUID();
        String insert="INSERT INTO notifications(id,source_event_id,recipient_id,actor_id,event_type,comment_id,created_at) VALUES(?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
        jdbc.update(insert,UUID.randomUUID(),UUID.randomUUID(),owner,actor,"community.comment-mentioned.v1",comment,java.sql.Timestamp.from(Instant.now()));
        jdbc.update(insert,UUID.randomUUID(),UUID.randomUUID(),owner,actor,"community.comment-mentioned.v1",comment,java.sql.Timestamp.from(Instant.now()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE comment_id=? AND recipient_id=?",Integer.class,comment,owner));
    }
}
