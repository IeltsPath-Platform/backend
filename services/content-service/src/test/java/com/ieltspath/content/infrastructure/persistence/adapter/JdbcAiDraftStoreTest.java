package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.DraftKind;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class JdbcAiDraftStoreTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void dailyAllowanceStopsAtTheLimitAndRefundReopensIt() {
        JdbcAiUsageQuota quota = new JdbcAiUsageQuota(jdbc);
        UUID author = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 10, 23);

        assertTrue(quota.tryConsume(author, day, AiUsageKind.AI, 2));
        assertTrue(quota.tryConsume(author, day, AiUsageKind.AI, 2));
        assertFalse(quota.tryConsume(author, day, AiUsageKind.AI, 2));

        quota.refund(author, day, AiUsageKind.AI);
        assertTrue(quota.tryConsume(author, day, AiUsageKind.AI, 2));
    }

    @Test
    void aPracticeSetDraftIsStoredAndReadBackWithItsKind() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);

        UUID id = drafts.save(AiDraft.request(DraftKind.PRACTICE_SET, UUID.randomUUID(), "{\"count\":5}",
                Instant.parse("2026-10-28T10:00:00Z"))).getId();

        assertEquals(DraftKind.PRACTICE_SET, drafts.findById(id).orElseThrow().getKind());
    }

    @Test
    void importAllowanceIsCountedSeparatelyFromGeneration() {
        JdbcAiUsageQuota quota = new JdbcAiUsageQuota(jdbc);
        UUID author = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 10, 23);

        assertTrue(quota.tryConsume(author, day, AiUsageKind.AI, 1));
        assertFalse(quota.tryConsume(author, day, AiUsageKind.AI, 1));
        assertTrue(quota.tryConsume(author, day, AiUsageKind.IMPORT, 1));
    }

    /**
     * The container is shared by the class, so other tests may leave queued drafts behind. The claim is checked
     * against this test's own drafts: every one is claimed exactly once, and none is claimed twice.
     */
    @Test
    void claimTakesEachQueuedDraftOnceAndNeverTwice() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        Instant now = Instant.parse("2026-10-23T10:00:00Z");
        List<UUID> mine = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mine.add(drafts.save(AiDraft.request(DraftKind.QUESTIONS, UUID.randomUUID(), "{\"n\":" + i + "}", now)).getId());
        }

        List<UUID> claimed = new ArrayList<>();
        for (List<UUID> batch = drafts.claimQueued(2); !batch.isEmpty(); batch = drafts.claimQueued(2)) {
            claimed.addAll(batch);
        }

        assertEquals(claimed.size(), new HashSet<>(claimed).size());
        assertTrue(claimed.containsAll(mine));
        assertEquals("GENERATING", drafts.findById(mine.get(0)).orElseThrow().getStatus().name());
    }

    @Test
    void staleGeneratingDraftsReturnToTheQueue() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        drafts.save(AiDraft.request(DraftKind.COURSE, UUID.randomUUID(), "{}", Instant.now()));
        drafts.claimQueued(1);

        int requeued = drafts.requeueStaleGenerating(Instant.now().plusSeconds(60));

        assertTrue(requeued >= 1);
    }
}
