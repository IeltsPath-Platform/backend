package com.ieltspath.access.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void internalLearnerCanReceiveDailyCappedReward() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO point_wallets(user_id, balance, total_credited, total_debited) VALUES (?, 0, 0, 0)",
                userId);
        ResponseEntity<String> response = rest.postForEntity("/internal/access/points/credit",
                request(creditBody(userId, 5, "reward-123", "kp-123"), signedInternalToken(userId)), String.class);

        assertEquals(200, response.getStatusCode().value());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(5, json.path("granted").asInt());
        assertEquals(false, json.path("capped").asBoolean());
        assertEquals(5, json.path("balance").asLong());
        assertNotNull(json.path("transactionId").textValue());
    }

    @Test
    void badgeCreditsBypassAndDoNotConsumeTheDailyLimitWhileDailyQuestStillUsesIt() throws Exception {
        UUID userId = UUID.randomUUID();
        seedDailyEarnings(userId, 18, "STREAK_MILESTONE");
        String token = signedInternalToken(userId);

        JsonNode firstBadge = postCredit(userId, 50, "badge-before-cap", "badge:STREAK_7", "BADGE", token);
        assertEquals(50, firstBadge.path("granted").asInt());
        assertEquals(false, firstBadge.path("capped").asBoolean());
        assertEquals(68, firstBadge.path("balance").asLong());

        JsonNode dailyQuestWithRoom = postCredit(userId, 5, "quest-with-room", "quest:daily:1", "DAILY_QUEST", token);
        assertEquals(2, dailyQuestWithRoom.path("granted").asInt());
        assertEquals(true, dailyQuestWithRoom.path("capped").asBoolean());
        assertEquals(70, dailyQuestWithRoom.path("balance").asLong());

        JsonNode badgeAfterCap = postCredit(userId, 50, "badge-after-cap", "badge:QUEST_COMPLETE", "BADGE", token);
        assertEquals(50, badgeAfterCap.path("granted").asInt());
        assertEquals(false, badgeAfterCap.path("capped").asBoolean());
        assertEquals(120, badgeAfterCap.path("balance").asLong());

        JsonNode dailyQuestAfterCap = postCredit(userId, 1, "quest-after-cap", "quest:daily:2", "DAILY_QUEST", token);
        assertEquals(0, dailyQuestAfterCap.path("granted").asInt());
        assertEquals(true, dailyQuestAfterCap.path("capped").asBoolean());
        assertEquals(120, dailyQuestAfterCap.path("balance").asLong());
        assertEquals(true, dailyQuestAfterCap.path("transactionId").isNull());

        assertEquals(100L, sumGrantedForReason(userId, "BADGE"));
        assertEquals(20L, sumDailyLimitEligibleRewards(userId));
    }

    @Test
    void creditRejectsRequestsWithoutInternalJwt() {
        ResponseEntity<String> response = rest.postForEntity("/internal/access/points/credit",
                request("{}", null), String.class);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void concurrentCreditWithSameKeyAppliesTheDailyCapOnlyOnce() throws Exception {
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

        String token = signedInternalToken(userId);
        String body = creditBody(userId, 5, "reward-concurrent", "kp-456");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResponseEntity<String>> first = executor.submit(() -> {
                start.await();
                return rest.postForEntity("/internal/access/points/credit", request(body, token), String.class);
            });
            Future<ResponseEntity<String>> second = executor.submit(() -> {
                start.await();
                return rest.postForEntity("/internal/access/points/credit", request(body, token), String.class);
            });
            start.countDown();
            JsonNode firstJson = objectMapper.readTree(first.get(30, TimeUnit.SECONDS).getBody());
            JsonNode secondJson = objectMapper.readTree(second.get(30, TimeUnit.SECONDS).getBody());

            assertEquals(2, firstJson.path("granted").asInt());
            assertEquals(true, firstJson.path("capped").asBoolean());
            assertEquals(20, firstJson.path("balance").asLong());
            assertEquals(firstJson.path("transactionId").asText(), secondJson.path("transactionId").asText());
            assertEquals(20L, jdbc.queryForObject("SELECT balance FROM point_wallets WHERE user_id = ?", Long.class, userId));
            assertEquals(20L, jdbc.queryForObject("""
                    SELECT COALESCE(SUM(delta), 0) FROM point_ledger_entries
                    WHERE user_id = ? AND transaction_type = 'LEARNING_REWARD'
                    """, Long.class, userId));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM point_credit_requests WHERE idempotency_key = ?",
                    Integer.class, "reward-concurrent"));
        } finally {
            executor.shutdownNow();
        }

        ResponseEntity<String> fullyCapped = rest.postForEntity("/internal/access/points/credit",
                request(creditBody(userId, 3, "reward-full", "kp-789"), token), String.class);
        assertEquals(200, fullyCapped.getStatusCode().value());
        JsonNode fullyCappedJson = objectMapper.readTree(fullyCapped.getBody());
        assertEquals(0, fullyCappedJson.path("granted").asInt());
        assertEquals(true, fullyCappedJson.path("capped").asBoolean());
        assertEquals(20, fullyCappedJson.path("balance").asLong());
        assertEquals(true, fullyCappedJson.path("transactionId").isNull());

        ResponseEntity<String> replay = rest.postForEntity("/internal/access/points/credit",
                request(creditBody(userId, 3, "reward-full", "kp-789"), token), String.class);
        assertEquals(fullyCapped.getBody(), replay.getBody());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM point_ledger_entries WHERE idempotency_key = ?",
                Integer.class, "reward-full"));
    }

    @Test
    void debitMapsNewReferencesToTheirLedgerTypesAndMigrationRejectsUnknownType() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO point_wallets(user_id, balance, total_credited, total_debited) VALUES (?, 5, 5, 0)",
                userId);
        String token = signedInternalToken(userId);
        for (String[] pair : new String[][]{{"HEART_REFILL", "HEART_REFILL_DEBIT"},
                {"STREAK_FREEZE", "STREAK_FREEZE_DEBIT"}}) {
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

    private static String creditBody(UUID userId, int amount, String idempotencyKey, String referenceId) {
        return creditBody(userId, amount, idempotencyKey, referenceId, "KP_MASTERED");
    }

    private JsonNode postCredit(UUID userId, int amount, String idempotencyKey, String referenceId,
                                String reason, String token) throws Exception {
        ResponseEntity<String> response = rest.postForEntity("/internal/access/points/credit",
                request(creditBody(userId, amount, idempotencyKey, referenceId, reason), token), String.class);
        assertEquals(200, response.getStatusCode().value());
        return objectMapper.readTree(response.getBody());
    }

    private static String creditBody(UUID userId, int amount, String idempotencyKey, String referenceId, String reason) {
        return "{\"userId\":\"" + userId + "\",\"amount\":" + amount
                + ",\"reason\":\"" + reason + "\",\"referenceId\":\"" + referenceId
                + "\",\"idempotencyKey\":\"" + idempotencyKey + "\"}";
    }

    private void seedDailyEarnings(UUID userId, int amount, String reason) {
        jdbc.update("INSERT INTO point_wallets(user_id, balance, total_credited, total_debited) VALUES (?, ?, ?, 0)",
                userId, amount, amount);
        Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusSeconds(1).toInstant(ZoneOffset.UTC);
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
