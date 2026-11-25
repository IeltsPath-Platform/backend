package com.ieltspath.access.api.controller;

import com.ieltspath.access.application.port.KeyDistributionStore.Batch;
import com.ieltspath.access.application.port.KeyDistributionStore.KeyRow;
import com.ieltspath.access.application.usecase.KeyBatchUseCases;
import com.ieltspath.access.application.port.SaleDirectory;
import com.ieltspath.access.application.port.SaleDirectory.SaleAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may call which screen's API, checked with real gateway-style tokens: a role that is not the screen's own gets 403,
 * and the learner's refusal for a key nobody has handed out reads the same as for a code that does not exist.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class KeyDistributionSecurityIntegrationTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String KEY_CODE_SECRET = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII));

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
        registry.add("access.key-code.secret", () -> KEY_CODE_SECRET);
    }

    @MockBean
    SaleDirectory directory;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired KeyBatchUseCases batches;

    private static String token(UUID subject, String... roles) {
        long now = Instant.now().getEpochSecond();
        String roleList = "\"" + String.join("\",\"", roles) + "\"";
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"iss\":\"urn:code-base:api-gateway\",\"sub\":\"" + subject
                + "\",\"iat\":" + now + ",\"exp\":" + (now + 300) + ",\"roles\":[" + roleList + "]}");
        String input = header + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256"));
            return input + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(input.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void onlyStaffManagerReachesTheManagerScreens() throws Exception {
        when(directory.sales()).thenReturn(List.of(new SaleAccount(UUID.randomUUID(), "Sale A", true)));
        UUID manager = UUID.randomUUID();
        String asManager = "Bearer " + token(manager, "STAFF_MANAGER");

        mvc.perform(get("/api/access/manager/batches").header("Authorization", asManager))
                .andExpect(status().isOk());
        mvc.perform(get("/api/access/manager/batches").header("Authorization", "Bearer " + token(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/access/manager/batches").header("Authorization", "Bearer " + token(UUID.randomUUID(), "SALES_STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/access/manager/batches").header("Authorization", "Bearer " + token(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlySalesStaffReachesTheSaleScreens() throws Exception {
        UUID sale = UUID.randomUUID();
        mvc.perform(get("/api/access/sale/stock").header("Authorization", "Bearer " + token(sale, "SALES_STAFF")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/access/sale/stock").header("Authorization", "Bearer " + token(UUID.randomUUID(), "STAFF_MANAGER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/access/sale/stock").header("Authorization", "Bearer " + token(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void issuingWithoutAnIdempotencyKeyAsksForOneInVietnamese() throws Exception {
        UUID sale = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        jdbc.update("INSERT INTO key_products (id, code, name, key_type, points_amount, status) VALUES (?, ?, ?, 'POINTS', 50, 'ACTIVE')",
                product, "P-" + product, "Gói 50");

        mvc.perform(post("/api/access/sale/issue")
                        .header("Authorization", "Bearer " + token(sale, "SALES_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + product + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Thiếu mã chống gửi trùng")));
    }

    @Test
    void aLearnerTryingAKeyNobodyHandedOutGetsTheSameAnswerAsAMissingCode() throws Exception {
        UUID manager = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        jdbc.update("INSERT INTO key_products (id, code, name, key_type, points_amount, status) VALUES (?, ?, ?, 'POINTS', 100, 'ACTIVE')",
                product, "P-" + product, "Gói 100");
        Batch batch = batches.createBatch(manager, product, 1, null, null);
        KeyRow key = batches.batchKeys(batch.code(), null, null, false, false, null, 0, 10).items().get(0);
        String code = batches.reveal(manager, key.id());

        UUID learner = UUID.randomUUID();
        jdbc.update("INSERT INTO user_email_verification_state (user_id, verified, occurred_at) VALUES (?, true, CURRENT_TIMESTAMP)", learner);
        String body = "{\"rawKey\":\"" + code + "\",\"idempotencyKey\":\"learner-http-1\"}";

        mvc.perform(post("/api/access/me/keys/activate")
                        .header("Authorization", "Bearer " + token(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KEY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Mã kích hoạt không hợp lệ"));

        mvc.perform(post("/api/access/me/keys/activate")
                        .header("Authorization", "Bearer " + token(learner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawKey\":\"IP-NOPE-NOPE-NOPE\",\"idempotencyKey\":\"learner-http-2\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KEY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Mã kích hoạt không hợp lệ"));
        assertThat(batches.batch(batch.code()).pool()).isEqualTo(1);
    }
}
