package com.ieltspath.community.infrastructure;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddReactionCommand;
import com.ieltspath.community.application.command.CreateCommentCommand;
import com.ieltspath.community.application.command.CreatePostCommand;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.application.usecase.AddReactionUseCase;
import com.ieltspath.community.application.usecase.CreateCommentUseCase;
import com.ieltspath.community.application.usecase.CreatePostUseCase;
import com.ieltspath.community.application.usecase.CommunityQueriesUseCase;
import com.ieltspath.community.domain.vo.PostBackground;
import com.ieltspath.community.application.usecase.ManageSocialUseCase;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.rabbitmq.dynamic=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "community.outbox.relay.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.auth.internal-jwt-secret=SGs1cjhXY1lBOEo4am8ycnVJQkNFWFJjSGhVZlNEVXFQMTZIMjlQMXN4bz0="
})
@Import(CommunityOutboxUseCaseIntegrationTest.TestProfiles.class)
class CommunityOutboxUseCaseIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final UUID AUTHOR = UUID.fromString("10000000-0000-4000-8000-000000000021");
    private static final UUID ACTOR = UUID.fromString("10000000-0000-4000-8000-000000000022");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ManageSocialUseCase social;
    @Autowired CreateCommentUseCase comments;
    @Autowired AddReactionUseCase reactions;
    @Autowired CreatePostUseCase posts;
    @Autowired CommunityQueriesUseCase queries;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired com.ieltspath.community.domain.repository.CommentReactionRepository commentReactions;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Test
    void commentReactionReplaceRepeatAndWrongTypeRemovePreserveOneRowAndNoOpTimestamp() {
        Comment comment = commentRepository.save(Comment.create(savePost().getId(), AUTHOR, null, "comment"));
        UUID id = comment.getId();
        assertEquals(true, transactions.execute(status -> commentReactions.add(id, ACTOR, ReactionType.LIKE)));
        jdbc.update("UPDATE comment_reactions SET created_at='2026-10-16T10:00:00Z' WHERE comment_id=?", id);
        var timestamp = jdbc.queryForObject("SELECT created_at FROM comment_reactions WHERE comment_id=?", java.time.OffsetDateTime.class, id);
        assertEquals(false, transactions.execute(status -> commentReactions.add(id, ACTOR, ReactionType.LIKE)));
        assertEquals(timestamp, jdbc.queryForObject("SELECT created_at FROM comment_reactions WHERE comment_id=?", java.time.OffsetDateTime.class, id));
        assertEquals(true, transactions.execute(status -> commentReactions.add(id, ACTOR, ReactionType.LOVE)));
        org.junit.jupiter.api.Assertions.assertNotEquals(timestamp,
                jdbc.queryForObject("SELECT created_at FROM comment_reactions WHERE comment_id=?", java.time.OffsetDateTime.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comment_reactions WHERE comment_id=?", Integer.class, id));
        assertEquals("LOVE", jdbc.queryForObject("SELECT reaction_type FROM comment_reactions WHERE comment_id=?", String.class, id));
        assertEquals(false, transactions.execute(status -> commentReactions.remove(id, ACTOR, ReactionType.LIKE)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comment_reactions WHERE comment_id=?", Integer.class, id));
        assertEquals(true, transactions.execute(status -> commentReactions.remove(id, ACTOR, ReactionType.LOVE)));
        assertEquals(false, transactions.execute(status -> commentReactions.remove(id, ACTOR, ReactionType.LOVE)));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comment_reactions WHERE comment_id=?", Integer.class, id));
    }

    @Test
    void twentyCommentsUseOneSqlForCountsAndViewerReaction() {
        Post post = savePost();
        var ids = new java.util.HashSet<UUID>();
        for (int i = 0; i < 20; i++) {
            UUID id = commentRepository.save(Comment.create(post.getId(), AUTHOR, null, "comment " + i)).getId();
            ids.add(id);
            transactions.executeWithoutResult(status -> {
                commentReactions.add(id, ACTOR, ReactionType.LIKE);
                commentReactions.add(id, AUTHOR, ReactionType.LOVE);
            });
        }
        var statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
        var summary = commentReactions.interactions(ids, ACTOR);
        assertEquals(1, statistics.getPrepareStatementCount(), "Counts and myReaction must share one batch query");
        assertEquals(20, summary.size());
        for (UUID id : ids) {
            assertEquals(java.util.Map.of(ReactionType.LIKE, 1L, ReactionType.LOVE, 1L,
                    ReactionType.HELPFUL, 0L, ReactionType.ACCURATE, 0L), summary.get(id).reactionCounts());
            assertEquals(ReactionType.LIKE, summary.get(id).myReaction());
        }
        assertEquals(ReactionType.LOVE, commentReactions.interactions(ids, AUTHOR).get(ids.iterator().next()).myReaction());
        assertEquals(1L, commentReactions.counts(ids).get(ids.iterator().next()).get(ReactionType.LIKE));
        assertEquals(ReactionType.LIKE, commentReactions.mine(ids, ACTOR).get(ids.iterator().next()));
        assertEquals(java.util.Map.of(), commentReactions.mine(ids, UUID.randomUUID()));
        org.junit.jupiter.api.Assertions.assertNull(commentReactions.interactions(ids, UUID.randomUUID()).get(ids.iterator().next()).myReaction());
        statistics.clear();
        assertEquals(java.util.Map.of(), commentReactions.interactions(java.util.Set.of(), ACTOR));
        assertEquals(0, statistics.getPrepareStatementCount());
    }

    @Test
    void concurrentDifferentReactionsBySameViewerLeaveExactlyOneRow() throws Exception {
        UUID id = commentRepository.save(Comment.create(savePost().getId(), AUTHOR, null, "concurrent")).getId();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (ReactionType type : java.util.List.of(ReactionType.LIKE, ReactionType.LOVE)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Start barrier timed out");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    transactions.executeWithoutResult(status -> commentReactions.add(id, ACTOR, type));
                }));
            }
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            for (var future : futures) future.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comment_reactions WHERE comment_id=? AND user_id=?", Integer.class, id, ACTOR));
        org.junit.jupiter.api.Assertions.assertTrue(java.util.Set.of("LIKE", "LOVE").contains(
                jdbc.queryForObject("SELECT reaction_type FROM comment_reactions WHERE comment_id=? AND user_id=?", String.class, id, ACTOR)));
    }

    @Test
    void softModerationKeepsCommentReactionsAndPhysicalDeleteCascades() {
        UUID id = commentRepository.save(Comment.create(savePost().getId(), AUTHOR, null, "moderated")).getId();
        transactions.executeWithoutResult(status -> commentReactions.add(id, ACTOR, ReactionType.ACCURATE));
        for (String state : java.util.List.of("HIDDEN", "DELETED")) {
            jdbc.update("UPDATE comments SET status=? WHERE id=?", state, id);
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comment_reactions WHERE comment_id=?", Integer.class, id));
        }
        jdbc.update("DELETE FROM comments WHERE id=?", id);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comment_reactions WHERE comment_id=?", Integer.class, id));
    }

    @BeforeEach
    void clearDatabase() {
        jdbc.execute("TRUNCATE TABLE outbox_events, user_follows, comments, post_reactions, posts CASCADE");
    }

    @Test
    void followUseCaseWritesTimestampedOutboxEventToPostgres() {
        social.follow(new CommunityActor(ACTOR, false), AUTHOR);

        assertEquals(1, outboxCount("community.user-followed.v1"));
        assertNotNull(outboxCreatedAt("community.user-followed.v1"));
    }

    @Test
    void commentOnPostUseCaseWritesTimestampedOutboxEventToPostgres() {
        Post post = savePost();

        comments.execute(new CommunityActor(ACTOR, false), new CreateCommentCommand(post.getId(), null, "hello"));

        assertEquals(1, outboxCount("community.post-commented.v1"));
        assertNotNull(outboxCreatedAt("community.post-commented.v1"));
    }

    @Test
    void replyUseCaseWritesTimestampedOutboxEventToPostgres() {
        Post post = savePost();
        Comment parent = commentRepository.save(Comment.create(post.getId(), AUTHOR, null, "root"));

        comments.execute(new CommunityActor(ACTOR, false), new CreateCommentCommand(post.getId(), parent.getId(), "reply"));

        assertEquals(1, outboxCount("community.comment-replied.v1"));
        assertNotNull(outboxCreatedAt("community.comment-replied.v1"));
    }

    @Test
    void commentMentionWritesOneRealOutboxRowPerDistinctUserAndSkipsSelf() {
        Post post = savePost();
        UUID mentioned = UUID.randomUUID();
        String body = "Hello @[Member](" + mentioned + ") and @[Again](" + mentioned + ") @[Me](" + ACTOR + ")";
        var result = comments.execute(new CommunityActor(ACTOR, false), new CreateCommentCommand(post.getId(), null, body));
        assertEquals(1, outboxCount("community.comment-mentioned.v1"));
        assertEquals(mentioned.toString(), jdbc.queryForObject("SELECT payload->>'recipientId' FROM outbox_events WHERE event_type=?", String.class, "community.comment-mentioned.v1"));
        assertEquals(result.id().toString(), jdbc.queryForObject("SELECT payload->>'commentId' FROM outbox_events WHERE event_type=?", String.class, "community.comment-mentioned.v1"));
    }

    @Test
    void reactionUseCaseWritesTimestampedOutboxEventToPostgres() {
        Post post = savePost();

        reactions.execute(new CommunityActor(ACTOR, false), new AddReactionCommand(post.getId(), ReactionType.LIKE));

        assertEquals(1, outboxCount("community.post-reacted.v1"));
        assertNotNull(outboxCreatedAt("community.post-reacted.v1"));
    }

    @Test
    void createPostUseCasePersistsPostWithoutInventingAnOutboxRecipient() {
        var result = posts.execute(new CommunityActor(ACTOR, false),
                new CreatePostCommand(PostCategory.GENERAL, "title", "body", java.util.List.of(), null, null, null));

        assertNotNull(postRepository.findById(result.id()).orElseThrow());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class));
    }

    @Test
    void createPostWithBackgroundCanBeReadAgainThroughFeed() {
        var created = posts.execute(new CommunityActor(AUTHOR, false),
                new CreatePostCommand(PostCategory.GENERAL, "Background post", "Short body",
                        java.util.List.of(), null, null, PostBackground.AURORA));

        var viewer = new CommunityActor(ACTOR, false);
        var feed = queries.feed(viewer, null, false, false,
                PostCategory.GENERAL, null, 10);

        var returned = feed.content().stream().filter(post -> post.id().equals(created.id())).findFirst().orElseThrow();
        assertEquals(PostBackground.AURORA, returned.background());

        jdbc.update("INSERT INTO user_follows(follower_id,followee_id,created_at) VALUES (?,?,now())", ACTOR, AUTHOR);
        var following = queries.feed(viewer, null, false, true, PostCategory.GENERAL, null, 10);
        assertEquals(PostBackground.AURORA, following.content().stream()
                .filter(post -> post.id().equals(created.id())).findFirst().orElseThrow().background());

        jdbc.update("INSERT INTO post_bookmarks(user_id,post_id) VALUES (?,?)", ACTOR, created.id());
        var bookmarked = queries.feed(viewer, null, true, false, PostCategory.GENERAL, null, 10);
        assertEquals(PostBackground.AURORA, bookmarked.content().stream()
                .filter(post -> post.id().equals(created.id())).findFirst().orElseThrow().background());

        var search = queries.searchFeed(viewer, "Background post", true, PostCategory.GENERAL, null, 10);
        assertEquals(PostBackground.AURORA, search.content().stream()
                .filter(post -> post.id().equals(created.id())).findFirst().orElseThrow().background());
    }

    @Test
    void commentMentioningPostAuthorEmitsOnlyMentionEventAndSuppressesPostCommented() {
        Post post = savePost();
        String body = "Hello @[Author](" + AUTHOR + ")";
        comments.execute(new CommunityActor(ACTOR, false), new CreateCommentCommand(post.getId(), null, body));

        assertEquals(1, outboxCount("community.comment-mentioned.v1"));
        assertEquals(0, outboxCount("community.post-commented.v1"));
        assertEquals(AUTHOR.toString(), jdbc.queryForObject(
                "SELECT payload->>'recipientId' FROM outbox_events WHERE event_type=?",
                String.class, "community.comment-mentioned.v1"));
    }

    @Test
    void replyMentioningParentCommentAuthorEmitsOnlyMentionEventAndSuppressesCommentReplied() {
        Post post = savePost();
        UUID parentAuthor = UUID.randomUUID();
        Comment parent = commentRepository.save(Comment.create(post.getId(), parentAuthor, null, "root"));

        String body = "@[Alice](" + parentAuthor + ") cảm ơn";
        comments.execute(new CommunityActor(ACTOR, false), new CreateCommentCommand(post.getId(), parent.getId(), body));

        assertEquals(1, outboxCount("community.comment-mentioned.v1"));
        assertEquals(0, outboxCount("community.comment-replied.v1"));
        assertEquals(0, outboxCount("community.post-commented.v1"));
        assertEquals(parentAuthor.toString(), jdbc.queryForObject(
                "SELECT payload->>'recipientId' FROM outbox_events WHERE event_type=?",
                String.class, "community.comment-mentioned.v1"));
    }

    private Post savePost() {
        return postRepository.save(Post.create(AUTHOR, PostCategory.GENERAL, "title", "body"));
    }

    private int outboxCount(String eventType) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE event_type=?", Integer.class, eventType);
    }

    private java.sql.Timestamp outboxCreatedAt(String eventType) {
        return jdbc.queryForObject("SELECT created_at FROM outbox_events WHERE event_type=?", java.sql.Timestamp.class, eventType);
    }

    @TestConfiguration
    static class TestProfiles {
        @Bean
        @Primary
        PublicProfileClient testPublicProfileClient() {
            return ids -> ids.stream().collect(java.util.stream.Collectors.toMap(id -> id, PublicProfile::anonymous));
        }
    }
}
