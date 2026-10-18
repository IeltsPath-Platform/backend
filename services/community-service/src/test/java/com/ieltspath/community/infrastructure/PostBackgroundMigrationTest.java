package com.ieltspath.community.infrastructure;

import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.vo.PostBackground;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.infrastructure.persistence.mapper.PostPersistenceMapper;
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
class PostBackgroundMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcTemplate jdbc;

    @BeforeAll static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        org.flywaydb.core.Flyway.configure().dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    private void insert(String body, String images, String video, String background) {
        jdbc.update("INSERT INTO posts(id,author_id,category,body,status,created_at,updated_at,image_urls,video_url,background_style) "
                        + "VALUES(?,?,'GENERAL',?,'ACTIVE',now(),now(),CAST(? AS jsonb),?,?)",
                UUID.randomUUID(), UUID.randomUUID(), body, images, video, background);
    }

    @Test void existingStylePostsWithoutBackgroundStillInsert() {
        insert("plain", "[]", null, null);
        insert("with image", "[\"https://images.unsplash.com/a.png\"]", null, null);
    }

    @Test void databaseRejectsUnknownBackgroundMediaAndLongBody() {
        insert("short", "[]", null, "SUNSET");
        assertThrows(DataIntegrityViolationException.class, () -> insert("short", "[]", null, "NEON"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insert("short", "[\"https://images.unsplash.com/a.png\"]", null, "SUNSET"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insert("short", "[]", "https://www.youtube.com/watch?v=abc", "SUNSET"));
        assertThrows(DataIntegrityViolationException.class, () -> insert("x".repeat(281), "[]", null, "SUNSET"));
    }

    @Test void mapperRoundTripsBackground() {
        var mapper = new PostPersistenceMapper();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "Mapped");
        post.setBackground(PostBackground.AURORA);
        assertEquals(PostBackground.AURORA, mapper.toDomain(mapper.toEntity(post)).getBackground());
    }
}
