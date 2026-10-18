package com.ieltspath.community.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.community.domain.vo.FeedCursor;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReactionType;
import com.ieltspath.community.infrastructure.adapter.CommunityReadRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class CommunityReadRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static JdbcTemplate jdbc;
    static CommunityReadRepositoryAdapter reads;
    static final UUID VIEWER = UUID.fromString("10000000-0000-4000-8000-000000000001");
    static final UUID AUTHOR = UUID.fromString("10000000-0000-4000-8000-000000000002");
    static final UUID OLDEST = UUID.fromString("10000000-0000-4000-8000-000000000003");
    static final UUID MIDDLE = UUID.fromString("10000000-0000-4000-8000-000000000004");
    static final UUID NEWEST = UUID.fromString("10000000-0000-4000-8000-000000000005");
    static final UUID UNFOLLOWED_AUTHOR = UUID.fromString("10000000-0000-4000-8000-000000000006");
    static final UUID UNFOLLOWED_POST = UUID.fromString("10000000-0000-4000-8000-000000000007");
    static final UUID SEARCH_OLDER = UUID.fromString("10000000-0000-4000-8000-000000000008");
    static final UUID SEARCH_NEWER = UUID.fromString("10000000-0000-4000-8000-000000000009");
    static final UUID SEARCH_RUNNING = UUID.fromString("10000000-0000-4000-8000-000000000010");
    static final Instant CREATED = Instant.parse("2026-10-16T10:00:00Z");

    @BeforeAll
    static void prepareDatabase() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(source);
        reads = new CommunityReadRepositoryAdapter(new NamedParameterJdbcTemplate(jdbc), new ObjectMapper());
        for (UUID id : Set.of(OLDEST, MIDDLE, NEWEST)) {
            jdbc.update("INSERT INTO posts(id,author_id,category,body,status,created_at,updated_at) "
                            + "VALUES (?,?,'GENERAL','body','ACTIVE',?,?)", id, AUTHOR,
                    java.sql.Timestamp.from(CREATED), java.sql.Timestamp.from(CREATED));
        }
        jdbc.update("INSERT INTO posts(id,author_id,category,body,status,created_at,updated_at) "
                        + "VALUES (?,?,'QUESTION','unfollowed','ACTIVE',?,?)", UNFOLLOWED_POST, UNFOLLOWED_AUTHOR,
                java.sql.Timestamp.from(CREATED), java.sql.Timestamp.from(CREATED));
        insertSearchPost(SEARCH_OLDER, "Reading strategies", "Use a reading strategy for IELTS.");
        insertSearchPost(SEARCH_NEWER, null, "Reading strategy matters for exams.");
        insertSearchPost(SEARCH_RUNNING, null, "Running exercises help with focus.");
        jdbc.update("INSERT INTO post_reactions(post_id,user_id,reaction_type,created_at) VALUES (?,?,?,?)",
                MIDDLE, VIEWER, "LIKE", java.sql.Timestamp.from(CREATED));
        jdbc.update("INSERT INTO comments(id,post_id,author_id,body,status,created_at,updated_at) VALUES "
                + "('10000000-0000-4000-8000-000000000011',?,?, 'root','ACTIVE',?,?),"
                + "('10000000-0000-4000-8000-000000000012',?,?, 'reply','ACTIVE',?,?)",
                MIDDLE, AUTHOR, java.sql.Timestamp.from(CREATED), java.sql.Timestamp.from(CREATED),
                MIDDLE, VIEWER, java.sql.Timestamp.from(CREATED), java.sql.Timestamp.from(CREATED));
        jdbc.update("UPDATE comments SET parent_comment_id='10000000-0000-4000-8000-000000000011' "
                + "WHERE id='10000000-0000-4000-8000-000000000012'");
        jdbc.update("INSERT INTO post_bookmarks(user_id,post_id) VALUES (?,?)", VIEWER, MIDDLE);
        jdbc.update("INSERT INTO user_follows(follower_id,followee_id,created_at) VALUES (?,?,?)",
                VIEWER, AUTHOR, java.sql.Timestamp.from(CREATED));
    }

    @Test
    void keysetUsesIdToBreakTimestampTiesAndBatchSummaryIncludesInteractions() {
        var first = reads.feed(VIEWER, null, false, false, PostCategory.GENERAL, null, 2);
        assertEquals(java.util.List.of(NEWEST, MIDDLE), first.stream().map(post -> post.getId()).toList());

        var next = reads.feed(VIEWER, null, false, false, PostCategory.GENERAL,
                new FeedCursor(CREATED, MIDDLE), 2);
        assertEquals(java.util.List.of(OLDEST), next.stream().map(post -> post.getId()).toList());

        var summary = reads.interactions(Set.of(MIDDLE), VIEWER).get(MIDDLE);
        assertEquals(1L, summary.reactionCounts().get(ReactionType.LIKE));
        assertEquals(2L, summary.commentCount());
        assertEquals(ReactionType.LIKE, summary.myReaction());
        assertEquals(true, summary.bookmarkedByMe());
        assertEquals(true, summary.followingAuthor());
    }

    @Test
    void followingFeedFiltersBeforeApplyingKeysetCursor() {
        var first = reads.feed(VIEWER, null, false, true, PostCategory.GENERAL, null, 2);
        assertEquals(java.util.List.of(NEWEST, MIDDLE), first.stream().map(post -> post.getId()).toList());

        var next = reads.feed(VIEWER, null, false, true, PostCategory.GENERAL,
                new FeedCursor(CREATED, MIDDLE), 2);
        assertEquals(java.util.List.of(OLDEST), next.stream().map(post -> post.getId()).toList());

        assertEquals(java.util.List.of(), reads.feed(UUID.randomUUID(), null, false, true,
                PostCategory.GENERAL, null, 10));
    }

    @Test
    void fullTextSearchUsesSimpleLanguageEscapesInputAndContinuesWithKeysetCursor() {
        var first = reads.search(VIEWER, true, PostCategory.QUESTION, "reading strategy", null, 1);
        assertEquals(java.util.List.of(SEARCH_NEWER), first.stream().map(post -> post.getId()).toList());

        var next = reads.search(VIEWER, true, PostCategory.QUESTION, "reading strategy",
                new FeedCursor(CREATED, SEARCH_NEWER), 1);
        assertEquals(java.util.List.of(SEARCH_OLDER), next.stream().map(post -> post.getId()).toList());

        assertEquals(java.util.List.of(), reads.search(VIEWER, false, null, "run", null, 10),
                "simple configuration does not stem running to run");
        assertEquals(java.util.List.of(), reads.search(VIEWER, false, null,
                "reading:* OR ' OR 1=1 --", null, 10));
    }

    private static void insertSearchPost(UUID id, String title, String body) {
        jdbc.update("INSERT INTO posts(id,author_id,category,title,body,status,created_at,updated_at) "
                        + "VALUES (?,?,'QUESTION',?,?,'ACTIVE',?,?)", id, AUTHOR, title, body,
                java.sql.Timestamp.from(CREATED), java.sql.Timestamp.from(CREATED));
    }
}
