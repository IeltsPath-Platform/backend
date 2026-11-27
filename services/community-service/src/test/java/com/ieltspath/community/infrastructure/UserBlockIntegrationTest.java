package com.ieltspath.community.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.CreateCommentCommand;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.application.usecase.CommunityQueriesUseCase;
import com.ieltspath.community.application.usecase.CreateCommentUseCase;
import com.ieltspath.community.application.usecase.ManageBlocksUseCase;
import com.ieltspath.community.application.usecase.ManageSocialUseCase;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.infrastructure.adapter.CommunityReadRepositoryAdapter;
import com.ieltspath.community.infrastructure.adapter.SocialRepositoryAdapter;
import com.ieltspath.community.infrastructure.adapter.UserBlockRepositoryAdapter;
import com.ieltspath.community.infrastructure.persistence.JdbcDeletedUserStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class UserBlockIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static JdbcTemplate jdbc;
    static CommunityReadRepositoryAdapter reads;
    static UserBlockRepositoryAdapter blocks;
    static SocialRepositoryAdapter social;
    static JdbcDeletedUserStore deletedUserStore;
    static TransactionTemplate tx;

    static final UUID USER_A = UUID.fromString("20000000-0000-4000-8000-000000000001");
    static final UUID USER_B = UUID.fromString("20000000-0000-4000-8000-000000000002");
    static final UUID USER_C = UUID.fromString("20000000-0000-4000-8000-000000000003");
    static final UUID POST_A = UUID.fromString("20000000-0000-4000-8000-000000000011");
    static final UUID POST_B = UUID.fromString("20000000-0000-4000-8000-000000000012");
    static final UUID POST_C = UUID.fromString("20000000-0000-4000-8000-000000000013");
    static final UUID COMMENT_A_ON_C = UUID.fromString("20000000-0000-4000-8000-000000000021");
    static final UUID COMMENT_B_ON_C = UUID.fromString("20000000-0000-4000-8000-000000000022");
    static final Instant NOW = Instant.parse("2026-11-27T08:00:00Z");

    @BeforeAll
    static void initDb() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(ds);
        var namedJdbc = new NamedParameterJdbcTemplate(jdbc);
        reads = new CommunityReadRepositoryAdapter(namedJdbc, new ObjectMapper());
        blocks = new UserBlockRepositoryAdapter(namedJdbc);
        social = new SocialRepositoryAdapter(jdbc);
        deletedUserStore = new JdbcDeletedUserStore(jdbc);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE TABLE user_blocks, user_follows, comments, posts CASCADE");
        insertPost(POST_A, USER_A, "IELTS Writing tips", "Post by A about writing");
        insertPost(POST_B, USER_B, "IELTS Speaking tips", "Post by B about speaking");
        insertPost(POST_C, USER_C, "IELTS Reading tips", "Post by C about reading");
        jdbc.update("INSERT INTO comments(id,post_id,author_id,body,status,created_at,updated_at) VALUES (?,?,?,'Comment from A','ACTIVE',?,?)",
                COMMENT_A_ON_C, POST_C, USER_A, Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("INSERT INTO comments(id,post_id,author_id,body,status,created_at,updated_at) VALUES (?,?,?,'Comment from B','ACTIVE',?,?)",
                COMMENT_B_ON_C, POST_C, USER_B, Timestamp.from(NOW.plusSeconds(1)), Timestamp.from(NOW.plusSeconds(1)));
        jdbc.update("INSERT INTO user_follows(follower_id,followee_id,created_at) VALUES (?,?,?),(?,?,?),(?,?,?)",
                USER_A, USER_B, Timestamp.from(NOW),
                USER_B, USER_A, Timestamp.from(NOW),
                USER_B, USER_C, Timestamp.from(NOW));
    }

    private static void insertPost(UUID id, UUID author, String title, String body) {
        jdbc.update("INSERT INTO posts(id,author_id,category,title,body,status,created_at,updated_at) VALUES (?,?,'GENERAL',?,?,'ACTIVE',?,?)",
                id, author, title, body, Timestamp.from(NOW), Timestamp.from(NOW));
    }

    private static PublicProfileClient stubProfiles() {
        return new PublicProfileClient() {
            @Override
            public Map<UUID, PublicProfile> findProfiles(Set<UUID> userIds) {
                var map = new java.util.HashMap<UUID, PublicProfile>();
                for (UUID id : userIds) {
                    map.put(id, new PublicProfile(id, "User " + id.toString().substring(32), null, null, null, null));
                }
                return map;
            }

            @Override
            public List<PublicProfile> searchProfiles(UUID viewerId, String query, int limit) {
                return List.of(
                        new PublicProfile(USER_A, "Alice", null, null, null, null),
                        new PublicProfile(USER_B, "Bob", null, null, null, null),
                        new PublicProfile(USER_C, "Charlie", null, null, null, null)
                );
            }
        };
    }

    @Test
    void blockingIsIdempotentCancelsMutualFollowsAndFiltersAllReadPathsTwoWays() {
        var manageBlocks = new ManageBlocksUseCase(blocks, social, stubProfiles(), tx);
        var actorA = new CommunityActor(USER_A, false);
        var actorB = new CommunityActor(USER_B, false);
        var actorC = new CommunityActor(USER_C, false);

        assertThrows(CommunityException.class, () -> manageBlocks.block(actorA, USER_A));

        manageBlocks.block(actorA, USER_B);
        manageBlocks.block(actorA, USER_B); // idempotent

        var blockedPage = manageBlocks.listBlocked(actorA, new PageQuery(0, 20));
        assertEquals(1, blockedPage.totalElements());
        assertEquals(USER_B, blockedPage.content().getFirst().id());

        // Mutual follows between A and B removed; B -> C remains
        assertFalse(reads.relationship(USER_A, USER_B).following());
        assertFalse(reads.relationship(USER_B, USER_A).following());
        assertTrue(reads.relationship(USER_B, USER_C).following());

        // Feed & search filtered two-way
        var feedA = reads.feed(USER_A, null, false, false, null, null, 10);
        assertEquals(Set.of(POST_A, POST_C), Set.copyOf(feedA.stream().map(p -> p.getId()).toList()));

        var feedB = reads.feed(USER_B, null, false, false, null, null, 10);
        assertEquals(Set.of(POST_B, POST_C), Set.copyOf(feedB.stream().map(p -> p.getId()).toList()));

        var searchA = reads.search(USER_A, false, null, "IELTS", null, 10);
        assertFalse(searchA.stream().anyMatch(p -> p.getId().equals(POST_B)));

        var searchB = reads.search(USER_B, false, null, "IELTS", null, 10);
        assertFalse(searchB.stream().anyMatch(p -> p.getId().equals(POST_A)));

        // Comments on C's post: A does not see B's comment; B does not see A's comment
        var threadA = reads.thread(USER_A, POST_C, null, 0, 10);
        assertEquals(List.of(COMMENT_A_ON_C), threadA.items().stream().map(c -> c.getId()).toList());
        assertEquals(1L, reads.interactions(Set.of(POST_C), USER_A).get(POST_C).commentCount());

        var threadB = reads.thread(USER_B, POST_C, null, 0, 10);
        assertEquals(List.of(COMMENT_B_ON_C), threadB.items().stream().map(c -> c.getId()).toList());

        // Mention suggestions for A exclude B
        var queries = new CommunityQueriesUseCase(reads, stubProfiles(), null, null, null, blocks);
        var suggestions = queries.mentionSuggestions(actorA, "user");
        assertEquals(List.of(USER_C), suggestions.stream().map(PublicProfile::id).toList());

        // B cannot follow A -> 403 USER_BLOCKED
        var manageSocial = new ManageSocialUseCase(social, null, stubProfiles(), (t, a, r, p, c) -> {}, blocks, tx);
        var followErr = assertThrows(CommunityForbiddenException.class, () -> manageSocial.follow(actorB, USER_A));
        assertEquals("USER_BLOCKED", followErr.getCode());

        // Unblock restores visibility
        manageBlocks.unblock(actorA, USER_B);
        manageBlocks.unblock(actorA, USER_B); // idempotent
        assertEquals(0, manageBlocks.listBlocked(actorA, new PageQuery(0, 20)).totalElements());
        assertEquals(3, reads.feed(USER_B, null, false, false, null, null, 10).size());

        // user.deleted.v1 deletes user_blocks in both directions
        manageBlocks.block(actorA, USER_B);
        manageBlocks.block(actorC, USER_B);
        deletedUserStore.anonymizeAndDeleteCommunityData(USER_B);
        assertEquals(0, manageBlocks.listBlocked(actorA, new PageQuery(0, 20)).totalElements());
        assertEquals(0, manageBlocks.listBlocked(actorC, new PageQuery(0, 20)).totalElements());
    }
}
