package com.ieltspath.access.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.application.usecase.CreditPointsUseCase;
import com.ieltspath.access.domain.vo.LearningRewardReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class PointCreditIntegrationTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CreditPointsUseCase credits;

    @Test
    void learnerReceivesAGrantedReward() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO point_wallets(user_id, balance, total_credited, total_debited) VALUES (?, 0, 0, 0)",
                userId);

        PointCreditResult result = credits.executeLearningReward(UUID.randomUUID(), userId, 5,
                LearningRewardReason.KP_MASTERED, "kp-123");

        assertEquals(5, result.granted());
        assertEquals(false, result.capped());
        assertEquals(5, result.balance());
        assertNotNull(result.transactionId());
    }

    @Test
    void badgeCreditsBypassAndDoNotConsumeTheDailyLimitWhileDailyQuestStillUsesIt() {
        UUID userId = UUID.randomUUID();
        seedDailyEarnings(userId, 18, "STREAK_MILESTONE");

        PointCreditResult firstBadge = grant(userId, 50, "badge:STREAK_7", LearningRewardReason.BADGE);
        assertEquals(50, firstBadge.granted());
        assertEquals(false, firstBadge.capped());
        assertEquals(68, firstBadge.balance());

        PointCreditResult dailyQuestWithRoom = grant(userId, 5, "quest:daily:1", LearningRewardReason.DAILY_QUEST);
        assertEquals(2, dailyQuestWithRoom.granted());
        assertEquals(true, dailyQuestWithRoom.capped());
        assertEquals(70, dailyQuestWithRoom.balance());

        PointCreditResult badgeAfterCap = grant(userId, 50, "badge:QUEST_COMPLETE", LearningRewardReason.BADGE);
        assertEquals(50, badgeAfterCap.granted());
        assertEquals(false, badgeAfterCap.capped());
        assertEquals(120, badgeAfterCap.balance());

        PointCreditResult dailyQuestAfterCap = grant(userId, 1, "quest:daily:2", LearningRewardReason.DAILY_QUEST);
        assertEquals(0, dailyQuestAfterCap.granted());
        assertEquals(true, dailyQuestAfterCap.capped());
        assertEquals(120, dailyQuestAfterCap.balance());
        assertNull(dailyQuestAfterCap.transactionId());

        assertEquals(100L, sumGrantedForReason(userId, "BADGE"));
        assertEquals(20L, sumDailyLimitEligibleRewards(userId));
    }

    @Test
    void theLimitIsARollingDayNotACalendarDay() {
        UUID recent = UUID.randomUUID();
        seedEarningsAt(recent, 20, "KP_MASTERED", Instant.now().minus(java.time.Duration.ofHours(23)));
        PointCreditResult stillCapped = grant(recent, 5, "kp-recent", LearningRewardReason.KP_MASTERED);
        assertEquals(0, stillCapped.granted(), "20 points earned 23 hours ago still count, whatever the date is");
        assertEquals(true, stillCapped.capped());

        UUID old = UUID.randomUUID();
        seedEarningsAt(old, 20, "KP_MASTERED", Instant.now().minus(java.time.Duration.ofHours(25)));
        PointCreditResult freed = grant(old, 5, "kp-old", LearningRewardReason.KP_MASTERED);
        assertEquals(5, freed.granted(), "points earned more than a day ago no longer count");
        assertEquals(false, freed.capped());
    }

    @Test
    void creditingPointsOverHttpIsNotExposed() {
        UUID userId = UUID.randomUUID();
        String body = "{\"userId\":\"" + userId + "\",\"amount\":5,\"reason\":\"BADGE\","
                + "\"referenceId\":\"badge:STREAK_7\",\"idempotencyKey\":\"http-credit\"}";

        ResponseEntity<String> response = rest.postForEntity("/internal/access/points/credit",
                request(body, signedInternalToken(userId)), String.class);

        assertEquals(404, response.getStatusCode().value());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM point_ledger_entries WHERE user_id = ?",
                Integer.class, userId));
    }

    @Test
    void concurrentGrantWithSameIdAppliesTheDailyCapOnlyOnce() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO point_wallets(user_id, balance, total_credited, total_debited) VALUES (?, 18, 18, 0)",
                userId);
        UUID existingLedgerId = UUID.randomUUID();
        Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusSeconds(1).toInstant(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO point_ledger_entries
                    (id, user_id, delta, balance_after, transaction_type, reference_type, reference_id,
                     idempotency_key, created_at)
                VALUES (?, ?, 18, 18, 'LEARNING_REWARD', 'KP_MASTERED', 'kp-prior', 'prior-reward', ?)
                """, existingLedgerId, userId, java.sql.Timestamp.from(today));

        UUID grantId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<PointCreditResult> first = executor.submit(() -> {
                start.await();
                return credits.executeLearningReward(grantId, userId, 5, LearningRewardReason.KP_MASTERED, "kp-456");
            });
            Future<PointCreditResult> second = executor.submit(() -> {
                start.await();
                return credits.executeLearningReward(grantId, userId, 5, LearningRewardReason.KP_MASTERED, "kp-456");
            });
            start.countDown();
            PointCreditResult firstResult = first.get(30, TimeUnit.SECONDS);
            PointCreditResult secondResult = second.get(30, TimeUnit.SECONDS);

            assertEquals(2, firstResult.granted());
            assertEquals(true, firstResult.capped());
            assertEquals(20, firstResult.balance());
            assertEquals(firstResult.transactionId(), secondResult.transactionId());
            assertEquals(20L, jdbc.queryForObject("SELECT balance FROM point_wallets WHERE user_id = ?", Long.class, userId));
            assertEquals(20L, jdbc.queryForObject("""
                    SELECT COALESCE(SUM(delta), 0) FROM point_ledger_entries
                    WHERE user_id = ? AND transaction_type = 'LEARNING_REWARD'
                    """, Long.class, userId));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM point_credit_requests WHERE idempotency_key = ?",
                    Integer.class, grantId.toString()));
        } finally {
            executor.shutdownNow();
        }

        UUID fullGrant = UUID.randomUUID();
        PointCreditResult fullyCapped = credits.executeLearningReward(fullGrant, userId, 3,
                LearningRewardReason.KP_MASTERED, "kp-789");
        assertEquals(0, fullyCapped.granted());
        assertEquals(true, fullyCapped.capped());
        assertEquals(20, fullyCapped.balance());
        assertNull(fullyCapped.transactionId());

        PointCreditResult replay = credits.executeLearningReward(fullGrant, userId, 3,
                LearningRewardReason.KP_MASTERED, "kp-789");
        assertEquals(fullyCapped, replay);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM point_ledger_entries WHERE idempotency_key = ?",
                Integer.class, fullGrant.toString()));
    }

    @Test
    void debitMapsNewReferencesToTheirLedgerTypesAndMigrationRejectsUnknownType() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO point_wallets(user_id, balance, total_credited, total_debited) VALUES (?, 5, 5, 0)",
                userId);
        String token = signedInternalToken(userId);
        for (String[] pair : new String[][]{{"HEART_REFILL", "HEART_REFILL_DEBIT"},
                {"STREAK_FREEZE", "STREAK_FREEZE_DEBIT"},
                {"COURSE_UNLOCK", "COURSE_UNLOCK_DEBIT"}}) {
            String body = "{\"userId\":\"" + userId + "\",\"amount\":1,\"referenceType\":\""
                    + pair[0] + "\",\"referenceId\":\"" + UUID.randomUUID()
                    + "\",\"idempotencyKey\":\"debit-" + pair[0] + "\"}";
            ResponseEntity<String> response = rest.postForEntity("/internal/access/points/debit",
                    request(body, token), String.class);
            assertEquals(200, response.getStatusCode().value());
            assertEquals(pair[1], objectMapper.readTree(response.getBody()).path("transactionType").asText());
        }

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO point_ledger_entries
                    (user_id, delta, balance_after, transaction_type, reference_type, reference_id, idempotency_key)
                VALUES (?, 1, 1, 'INVALID_TYPE', 'TEST', 'test', 'invalid-transaction-type')
                """, userId));
    }

    private static HttpEntity<String> request(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private PointCreditResult grant(UUID userId, int amount, String referenceId, LearningRewardReason reason) {
        return credits.executeLearningReward(UUID.randomUUID(), userId, amount, reason, referenceId);
    }

    private void seedDailyEarnings(UUID userId, int amount, String reason) {
        seedEarningsAt(userId, amount, reason,
                LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusSeconds(1).toInstant(ZoneOffset.UTC));
    }

    private void seedEarningsAt(UUID userId, int amount, String reason, Instant today) {
        jdbc.update("INSERT INTO point_wallets(user_id, balance, total_credited, total_debited) VALUES (?, ?, ?, 0)",
                userId, amount, amount);
        jdbc.update("""
                INSERT INTO point_ledger_entries
                    (id, user_id, delta, balance_after, transaction_type, reference_type, reference_id,
                     idempotency_key, created_at)
                VALUES (?, ?, ?, ?, 'LEARNING_REWARD', ?, 'prior-reward', ?, ?)
                """, UUID.randomUUID(), userId, amount, amount, reason, "prior-" + userId,
                java.sql.Timestamp.from(today));
    }

    private long sumGrantedForReason(UUID userId, String reason) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(delta), 0) FROM point_ledger_entries
                WHERE user_id = ? AND transaction_type = 'LEARNING_REWARD' AND reference_type = ?
                """, Long.class, userId, reason);
    }

    private long sumDailyLimitEligibleRewards(UUID userId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(delta), 0) FROM point_ledger_entries
                WHERE user_id = ? AND transaction_type = 'LEARNING_REWARD' AND reference_type <> 'BADGE'
                """, Long.class, userId);
    }

    private static String signedInternalToken(UUID subject) {
        long now = Instant.now().getEpochSecond();
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"iss\":\"urn:code-base:api-gateway\",\"sub\":\"" + subject
                + "\",\"iat\":" + now + ",\"exp\":" + (now + 300) + ",\"roles\":[\"CUSTOMER\"]}");
        String input = header + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256"));
            return input + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(input.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
