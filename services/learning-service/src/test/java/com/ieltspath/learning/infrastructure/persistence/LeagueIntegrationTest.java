package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.DisplayNameClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.LeagueService;
import com.ieltspath.learning.application.usecase.GetLeagueUseCase;
import com.ieltspath.learning.domain.repository.LeagueRepository;
import com.ieltspath.learning.domain.service.LeagueRules;
import com.ieltspath.learning.infrastructure.scheduler.LeagueCloseJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** League groups, the weekly close, the table a learner sees and the XP that reaches it, against a real database. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class LeagueIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired LeagueService league;
    @Autowired LeagueRepository repository;
    @Autowired LeagueCloseJob closeJob;
    @Autowired GetLeagueUseCase view;
    @Autowired GamificationRecorder gamification;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean DisplayNameClient names;
    @MockitoBean LearningContentClient content;

    private final LocalDate thisWeek = LeagueRules.weekStart(Instant.now());

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE league_members, league_groups, league_weeks, learner_league");
    }

    @Test
    void thirtyOneLearnersFillOneGroupAndOpenASecond() {
        for (int index = 0; index < 31; index++) league.addWeeklyXp(UUID.randomUUID(), 10);

        assertEquals(2, count("SELECT count(*) FROM league_groups"));
        assertEquals(30, count("SELECT max(c) FROM (SELECT count(*) c FROM league_members GROUP BY group_id) g"));
    }

    @Test
    void learnersJoiningAtTheSameTimeNeverOverfillAGroup() throws Exception {
        int learners = 40;
        ExecutorService pool = Executors.newFixedThreadPool(learners);
        CountDownLatch go = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        List<Future<?>> done = new ArrayList<>();
        for (int index = 0; index < learners; index++) {
            UUID user = UUID.randomUUID();
            done.add(pool.submit(() -> {
                go.await();
                transaction.executeWithoutResult(status -> league.addWeeklyXp(user, 10));
                return null;
            }));
        }
        go.countDown();
        for (Future<?> future : done) future.get();
        pool.shutdown();

        assertEquals(learners, count("SELECT count(*) FROM league_members"));
        assertTrue(count("SELECT max(c) FROM (SELECT count(*) c FROM league_members GROUP BY group_id) g") <= 30);
        assertEquals(2, count("SELECT count(*) FROM league_groups"));
    }

    @Test
    void anExtraAnswerAddsToTheLearnersWeekWithoutChangingTheirGroup() {
        UUID user = UUID.randomUUID();

        league.addWeeklyXp(user, 10);
        league.addWeeklyXp(user, 15);

        assertEquals(25, count("SELECT weekly_xp FROM league_members WHERE user_id = '" + user + "'"));
        assertEquals(1, count("SELECT count(*) FROM league_groups"));
    }

    @Test
    void closingAWeekPromotesTheTopSevenAndDemotesRanksTwentyThreeAndBelow() {
        LocalDate past = thisWeek.minusWeeks(2);
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<UUID> full = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            UUID user = UUID.randomUUID();
            full.add(user);
            // The first two share the lowest XP; the one who reached it first ranks higher.
            repository.join(user, past, 3, index == 1 ? 10 : (index + 1) * 10, base.plusSeconds(index));
        }
        List<UUID> small = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            UUID user = UUID.randomUUID();
            small.add(user);
            repository.join(user, past, 3, (index + 1) * 10, base.plusSeconds(100 + index));
        }

        int closed = closeJob.runOnce();

        assertEquals(1, closed);
        for (int index = 0; index < 30; index++) {
            int rank = rankOf(full, index);
            assertEquals(rank, count("SELECT final_rank FROM league_members WHERE user_id = '" + full.get(index) + "'"));
            LeagueRules.Outcome expected = LeagueRules.outcome(rank, 30);
            assertEquals(expected.name(), text("SELECT outcome FROM league_members WHERE user_id = '" + full.get(index)
                    + "'"));
            assertEquals(LeagueRules.next(com.ieltspath.learning.domain.vo.LeagueTier.GOLD, expected).level(),
                    count("SELECT tier FROM learner_league WHERE user_id = '" + full.get(index) + "'"));
        }
        assertEquals(29, count("SELECT final_rank FROM league_members WHERE user_id = '" + full.get(0) + "'"));
        assertEquals(30, count("SELECT final_rank FROM league_members WHERE user_id = '" + full.get(1) + "'"));
        assertEquals(0, count("SELECT count(*) FROM league_members WHERE outcome = 'DEMOTED' AND user_id IN ("
                + ids(small) + ")"), "a group under 23 demotes nobody");
        assertEquals(7, count("SELECT count(*) FROM league_members WHERE outcome = 'PROMOTED' AND user_id IN ("
                + ids(small) + ")"));
    }

    @Test
    void aSecondCloseChangesNothing() {
        LocalDate past = thisWeek.minusWeeks(2);
        UUID user = UUID.randomUUID();
        repository.join(user, past, 1, 50, Instant.now());

        assertEquals(1, closeJob.runOnce());
        int tier = count("SELECT tier FROM learner_league WHERE user_id = '" + user + "'");

        assertEquals(0, closeJob.runOnce());
        assertEquals(tier, count("SELECT tier FROM learner_league WHERE user_id = '" + user + "'"));
        assertEquals(2, tier, "the only learner of the group is in the top seven");
    }

    @Test
    void theCurrentWeekIsNotClosed() {
        league.addWeeklyXp(UUID.randomUUID(), 10);

        assertEquals(0, closeJob.runOnce());
        assertEquals(0, count("SELECT count(*) FROM league_weeks WHERE closed_at IS NOT NULL"));
    }

    @Test
    void theTableShowsDisplayNamesAndZonesAndAsksForNamesOnce() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        league.addWeeklyXp(other, 50);
        league.addWeeklyXp(me, 20);
        when(names.names(anyCollection())).thenReturn(Map.of(other, "Mina", me, "Lan"));

        GetLeagueUseCase.LeagueView table = view.view(me);

        assertTrue(table.joined());
        assertEquals("BRONZE", table.tier());
        assertEquals(List.of("Mina", "Lan"), table.table().stream().map(GetLeagueUseCase.Entry::displayName).toList());
        assertEquals(List.of(1, 2), table.table().stream().map(GetLeagueUseCase.Entry::rank).toList());
        assertTrue(table.table().get(1).you());
        assertFalse(table.table().get(0).you());
        assertEquals("PROMOTION", table.table().get(0).zone());
        verify(names, times(1)).names(anyCollection());
    }

    @Test
    void aLearnerWithoutXpThisWeekSeesAnEmptyTable() {
        GetLeagueUseCase.LeagueView table = view.view(UUID.randomUUID());

        assertFalse(table.joined());
        assertTrue(table.table().isEmpty());
        assertEquals("BRONZE", table.tier());
    }

    @Test
    void theHistoryListsClosedWeeksOnlyAndValidatesTheRange() {
        UUID user = UUID.randomUUID();
        repository.join(user, thisWeek.minusWeeks(2), 1, 50, Instant.now());
        closeJob.runOnce();
        league.addWeeklyXp(user, 10);

        List<GetLeagueUseCase.HistoryEntry> history = view.history(user, 8);

        assertEquals(1, history.size());
        assertEquals("PROMOTED", history.get(0).outcome());
        assertEquals(1, history.get(0).finalRank());
        assertEquals("INVALID_WEEKS", assertThrows(LearningRequestException.class, () -> view.history(user, 0)).getCode());
        assertEquals("INVALID_WEEKS", assertThrows(LearningRequestException.class, () -> view.history(user, 27)).getCode());
    }

    @Test
    void onlyXpThatCountedReachesTheLeague() {
        when(content.questionDifficulties(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            Map<UUID, String> difficulties = new HashMap<>();
            ids.forEach(id -> difficulties.put(id, "HARD"));
            return difficulties;
        });
        UUID user = UUID.randomUUID();
        List<UUID> correct = new ArrayList<>();
        for (int index = 0; index < 12; index++) correct.add(UUID.randomUUID());

        gamification.recordCorrectAnswers(user, UUID.randomUUID(), correct);

        assertEquals(100, count("SELECT weekly_xp FROM league_members WHERE user_id = '" + user + "'"),
                "twelve hard answers earn 240 but only the 100 under the daily cap count");
    }

    /** The rank the rules give member {@code index}: higher XP first, and on a tie the earlier join first. */
    private static int rankOf(List<UUID> members, int index) {
        if (index >= 2) return 30 - index;
        return index == 0 ? 29 : 30;
    }

    private static String ids(List<UUID> users) {
        return String.join(",", users.stream().map(user -> "'" + user + "'").toList());
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String text(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }
}
