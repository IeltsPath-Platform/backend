package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.application.result.AiDraftOrigin;
import com.ieltspath.content.domain.vo.AiDraftClaim;
import org.flywaydb.core.Flyway;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void anAuthorCanHaveOnlyOneOpenDraftForASection() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID author = UUID.randomUUID();
        UUID section = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-28T10:00:00Z");
        String request = "{\"sectionId\":\"" + section + "\"}";
        AiDraft first = new AiDraft(UUID.randomUUID(), DraftKind.SECTION_QUESTIONS, DraftStatus.READY, author,
                request, "{\"items\":[]}", null, null, null, now, null, now, null, null, null);
        AiDraft second = new AiDraft(UUID.randomUUID(), DraftKind.SECTION_QUESTIONS, DraftStatus.QUEUED, author,
                request, null, null, null, null, now, null, null, null, null, null);

        drafts.save(first);

        assertEquals(first.getId(), drafts.findOpenSectionDraft(author, section).orElseThrow().getId());
        assertThrows(DataIntegrityViolationException.class, () -> drafts.save(second));

        first.approve(author, now.plusSeconds(1));
        drafts.save(first);
        drafts.save(second);
        assertEquals(second.getId(), drafts.findOpenSectionDraft(author, section).orElseThrow().getId());
    }

    @Test
    void anAuthorCanHaveOnlyOneOpenQuestionDraftForALesson() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID author = UUID.randomUUID();
        UUID lesson = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-25T10:00:00Z");
        String request = "{\"lessonId\":\"" + lesson + "\"}";
        AiDraft first = new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.READY, author,
                request, "{\"items\":[]}", null, null, null, now, null, now, null, null, null);
        AiDraft second = new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.QUEUED, author,
                request, null, null, null, null, now, null, null, null, null, null);

        drafts.save(first);
        assertEquals(first.getId(), drafts.findOpenLessonQuestionDraft(author, lesson).orElseThrow().getId());
        assertThrows(DataIntegrityViolationException.class, () -> drafts.save(second));

        first.approve(author, now.plusSeconds(1));
        drafts.save(first);
        drafts.save(second);
        assertEquals(second.getId(), drafts.findOpenLessonQuestionDraft(author, lesson).orElseThrow().getId());
    }

    @Test
    void readyLessonQuestionDraftsExpireAfterTheirRetentionPeriod() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID author = UUID.randomUUID();
        Instant created = Instant.parse("2026-10-01T00:00:00Z");
        AiDraft lessonDraft = new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.READY, author,
                "{\"lessonId\":\"" + UUID.randomUUID() + "\"}", "{\"items\":[]}", null, null, null,
                created, null, created, null, null, null);
        AiDraft bankDraft = new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.READY, author,
                "{}", "{\"items\":[]}", null, null, null, created, null, created, null, null, null);
        drafts.save(lessonDraft);
        drafts.save(bankDraft);

        assertEquals(1, drafts.expireReadySectionDrafts(Instant.parse("2026-10-15T00:00:00Z")));
        assertEquals(DraftStatus.EXPIRED, drafts.findById(lessonDraft.getId()).orElseThrow().getStatus());
        assertEquals(DraftStatus.READY, drafts.findById(bankDraft.getId()).orElseThrow().getStatus());
    }

    @Test
    void deletedSectionSourcesRemainVisibleAsUnavailableOrigins() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID author = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        AiDraft draft = drafts.save(new AiDraft(UUID.randomUUID(), DraftKind.SECTION_QUESTIONS, DraftStatus.READY,
                author, "{\"packageId\":\"" + packageId + "\",\"versionId\":\"" + versionId
                + "\",\"sectionId\":\"" + sectionId + "\",\"source\":{\"text\":\"private text\"}}",
                "{\"items\":[]}", null, null, null, Instant.now(), null, Instant.now(), null, null, null));

        AiDraftOrigin origin = new JdbcAiDraftOriginReader(jdbc).findOrigins(List.of(draft)).get(draft.getId());

        assertEquals(false, origin.exists());
        assertEquals("SECTION", origin.type());
        assertEquals(sectionId, origin.sectionId());
    }

    @Test
    void deletedLessonSourcesRemainVisibleAsUnavailableOrigins() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID author = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        AiDraft draft = drafts.save(new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.READY,
                author, "{\"lessonId\":\"" + lessonId + "\",\"source\":{\"text\":\"private text\"}}",
                "{\"items\":[]}", null, null, null, Instant.now(), null, Instant.now(), null, null, null));

        AiDraftOrigin origin = new JdbcAiDraftOriginReader(jdbc).findOrigins(List.of(draft)).get(draft.getId());

        assertEquals(false, origin.exists());
        assertEquals("LESSON", origin.type());
        assertEquals(lessonId, origin.lessonId());
    }

    @Test
    void deleteRemovesTheRowOnlyWhileItsStatusIsAllowed() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        Instant now = Instant.parse("2026-10-23T10:00:00Z");
        UUID author = UUID.randomUUID();
        UUID ready = drafts.save(AiDraft.manual(DraftKind.COURSE, author, "{\"topics\":[]}", now)).getId();
        UUID queued = drafts.save(AiDraft.request(DraftKind.COURSE, author, "{}", now)).getId();

        assertEquals(0, drafts.deleteIfStatusIn(queued, Set.of(DraftStatus.READY, DraftStatus.FAILED)));
        assertTrue(drafts.findById(queued).isPresent());

        assertEquals(1, drafts.deleteIfStatusIn(ready, Set.of(DraftStatus.READY, DraftStatus.FAILED)));
        assertTrue(drafts.findById(ready).isEmpty());
        assertEquals(0, drafts.deleteIfStatusIn(ready, Set.of(DraftStatus.READY)));
        assertEquals(0, drafts.deleteIfStatusIn(queued, Set.of()));
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

        List<AiDraftClaim> claimed = new ArrayList<>();
        for (List<AiDraftClaim> batch = drafts.claimQueued(2); !batch.isEmpty(); batch = drafts.claimQueued(2)) {
            claimed.addAll(batch);
        }

        List<UUID> ids = claimed.stream().map(AiDraftClaim::draftId).toList();
        assertEquals(ids.size(), new HashSet<>(ids).size());
        assertTrue(ids.containsAll(mine));
        assertEquals(claimed.size(), claimed.stream().map(AiDraftClaim::claimToken).distinct().count());
        assertEquals("GENERATING", drafts.findById(mine.get(0)).orElseThrow().getStatus().name());
    }

    @Test
    void staleGeneratingDraftsReturnToTheQueue() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID id = drafts.save(AiDraft.request(DraftKind.COURSE, UUID.randomUUID(), "{}", Instant.now())).getId();
        AiDraftClaim first = claimFor(drafts, id);

        Instant staleTime = Instant.now().plusSeconds(60);
        assertTrue(drafts.requeueStaleGenerating(staleTime, 3, staleTime).isEmpty());
        assertEquals("QUEUED", drafts.findById(first.draftId()).orElseThrow().getStatus().name());

        claimFor(drafts, first.draftId());
        assertTrue(drafts.requeueStaleGenerating(staleTime, 3, staleTime).isEmpty());
        AiDraftClaim finalClaim = claimFor(drafts, first.draftId());
        List<AiDraft> exhausted = drafts.requeueStaleGenerating(staleTime, 3, staleTime);

        assertTrue(exhausted.stream().anyMatch(draft -> draft.getId().equals(first.draftId())));
        assertEquals(DraftStatus.FAILED, drafts.findById(finalClaim.draftId()).orElseThrow().getStatus());
        assertEquals(0, drafts.completeIfClaimed(finalClaim.draftId(), finalClaim.claimToken(), "{}", "m", "p", staleTime));
    }

    @Test
    void aReplacementClaimMakesLateWorkerUpdatesNoOps() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID id = drafts.save(AiDraft.request(DraftKind.QUESTIONS, UUID.randomUUID(), "{}", Instant.now())).getId();
        AiDraftClaim oldClaim = claimFor(drafts, id);
        Instant staleTime = Instant.now().plusSeconds(60);
        drafts.requeueStaleGenerating(staleTime, 3, staleTime);
        AiDraftClaim currentClaim = claimFor(drafts, id);

        assertEquals(0, drafts.completeIfClaimed(id, oldClaim.claimToken(), "{}", "old", "p", staleTime));
        assertEquals(1, drafts.completeIfClaimed(id, currentClaim.claimToken(), "{}", "new", "p", staleTime));
        assertEquals(0, drafts.failIfClaimed(id, currentClaim.claimToken(), "INTERNAL", staleTime));
        assertEquals("new", drafts.findById(id).orElseThrow().getModel());
    }

    @Test
    void deletingAClaimedDraftCannotBeUndoneByAWorker() {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        UUID id = drafts.save(AiDraft.request(DraftKind.QUESTIONS, UUID.randomUUID(), "{}", Instant.now())).getId();
        AiDraftClaim claim = claimFor(drafts, id);

        assertEquals(1, drafts.deleteIfStatusIn(id, Set.of(DraftStatus.GENERATING)));
        assertEquals(0, drafts.completeIfClaimed(id, claim.claimToken(), "{}", "m", "p", Instant.now()));
        assertTrue(drafts.findById(id).isEmpty());
    }

    @Test
    void separateWorkersNeverClaimTheSameDraft() throws Exception {
        JdbcAiDraftRepository drafts = new JdbcAiDraftRepository(jdbc);
        Instant now = Instant.now();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            ids.add(drafts.save(AiDraft.request(DraftKind.QUESTIONS, UUID.randomUUID(), "{}", now)).getId());
        }

        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Callable<List<UUID>> claimAll = () -> {
                List<UUID> claimed = new ArrayList<>();
                for (List<AiDraftClaim> batch = drafts.claimQueued(1); !batch.isEmpty(); batch = drafts.claimQueued(1)) {
                    claimed.add(batch.getFirst().draftId());
                }
                return claimed;
            };
            var first = workers.submit(claimAll);
            var second = workers.submit(claimAll);
            List<UUID> claimed = new ArrayList<>(first.get());
            claimed.addAll(second.get());

            assertEquals(ids.size(), claimed.stream().filter(ids::contains).distinct().count());
        } finally {
            workers.shutdownNow();
        }
    }

    private AiDraftClaim claimFor(JdbcAiDraftRepository drafts, UUID id) {
        for (List<AiDraftClaim> batch = drafts.claimQueued(1); !batch.isEmpty(); batch = drafts.claimQueued(1)) {
            AiDraftClaim claim = batch.getFirst();
            if (claim.draftId().equals(id)) return claim;
        }
        throw new AssertionError("Draft was not found in the queue");
    }
}
