package com.ieltspath.community.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class CommentReactionMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void upgradesExistingV10PostsCommentsAndReactionsWithoutChangingRows() {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(source).target("10").load().migrate();
        var jdbc = new JdbcTemplate(source);
        UUID post = UUID.randomUUID(), comment = UUID.randomUUID(), reply = UUID.randomUUID(), user = UUID.randomUUID();
        jdbc.update("INSERT INTO posts(id,author_id,category,body,status,created_at,updated_at) VALUES (?,?,'GENERAL','existing post','ACTIVE',now(),now())", post, user);
        jdbc.update("INSERT INTO comments(id,post_id,author_id,body,status,created_at,updated_at) VALUES (?,?,?,'existing comment','ACTIVE',now(),now())", comment, post, user);
        jdbc.update("INSERT INTO comments(id,post_id,author_id,parent_comment_id,body,status,created_at,updated_at) VALUES (?,?,?,?,'existing reply','ACTIVE',now(),now())", reply, post, user, comment);
        jdbc.update("INSERT INTO post_reactions(post_id,user_id,reaction_type,created_at) VALUES (?,?,'LOVE',now())", post, user);
        var beforePost = jdbc.queryForMap("SELECT * FROM posts WHERE id=?", post);
        var beforeComments = jdbc.queryForList("SELECT * FROM comments ORDER BY id");
        var beforeReaction = jdbc.queryForMap("SELECT * FROM post_reactions WHERE post_id=?", post);

        Flyway.configure().dataSource(source).load().migrate();

        assertEquals(beforePost, jdbc.queryForMap("SELECT * FROM posts WHERE id=?", post));
        assertEquals(beforeComments, jdbc.queryForList("SELECT * FROM comments ORDER BY id"));
        assertEquals(beforeReaction, jdbc.queryForMap("SELECT * FROM post_reactions WHERE post_id=?", post));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comment_reactions", Integer.class));
        for (String type : new String[] {"LIKE", "LOVE", "HELPFUL", "ACCURATE"}) {
            jdbc.update("INSERT INTO comment_reactions(comment_id,user_id,reaction_type,created_at) VALUES (?,?,?,now())", comment, UUID.randomUUID(), type);
        }
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO comment_reactions(comment_id,user_id,reaction_type,created_at) VALUES (?,?,?,now())", comment, UUID.randomUUID(), "INVALID"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO comment_reactions(comment_id,user_id,reaction_type,created_at) VALUES (?,?,?,now())", UUID.randomUUID(), user, "LIKE"));
        var index = jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname='idx_comment_reactions_comment_type'", String.class);
        assertTrue(index.contains("(comment_id, reaction_type)"));
    }
}
