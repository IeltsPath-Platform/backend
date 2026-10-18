package com.ieltspath.community.infrastructure;

import com.ieltspath.community.infrastructure.adapter.ContentReportRepositoryAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class ContentReportMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcTemplate jdbc;

    @BeforeAll static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        org.flywaydb.core.Flyway.configure().dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test void migrationCreatesReportTableAndQueueIndex() {
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_name='community_reports'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname='idx_community_reports_queue'", Integer.class));
    }

    @Test void uniqueConstraintAllowsOnlyOneReportPerReporterAndContent() {
        UUID reporter=UUID.randomUUID(), target=UUID.randomUUID();
        insert(reporter,"POST",target);

        assertThrows(DataIntegrityViolationException.class,()->insert(reporter,"POST",target));

        insert(UUID.randomUUID(),"POST",target);
        insert(reporter,"COMMENT",target);
        assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM community_reports WHERE content_id=?",Integer.class,target));
    }

    @Test void pendingQueueQueryReturnsPostAndCommentPreviewFromPostgres() {
        UUID author=UUID.randomUUID(), post=UUID.randomUUID(), comment=UUID.randomUUID();
        jdbc.update("INSERT INTO posts(id,author_id,category,title,body,status,created_at,updated_at) VALUES(?,?, 'GENERAL', ?, ?, 'ACTIVE', now(), now())",
                post,author,"Report title","Post body");
        jdbc.update("INSERT INTO comments(id,post_id,author_id,body,status,created_at,updated_at) VALUES(?,?,?,?,'ACTIVE',now(),now())",
                comment,post,author,"Comment preview");
        insert(UUID.randomUUID(),"POST",post);
        insert(UUID.randomUUID(),"COMMENT",comment);

        var page=new ContentReportRepositoryAdapter(jdbc).findPending(0,20);

        assertEquals(2,page.items().stream().filter(entry -> entry.contentId().equals(post) || entry.contentId().equals(comment)).count());
        assertEquals("Report title",page.items().stream().filter(entry -> entry.contentId().equals(post)).findFirst().orElseThrow().contentPreview());
        assertEquals("Comment preview",page.items().stream().filter(entry -> entry.contentId().equals(comment)).findFirst().orElseThrow().contentPreview());
    }

    private void insert(UUID reporter, String type, UUID contentId) {
        jdbc.update("""
                INSERT INTO community_reports(id,reporter_id,content_type,content_id,reason,status,created_at)
                VALUES(?,?,?,?,'Spam','PENDING',CURRENT_TIMESTAMP)
                """,UUID.randomUUID(),reporter,type,contentId);
    }
}
