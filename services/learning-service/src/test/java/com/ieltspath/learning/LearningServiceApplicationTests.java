package com.ieltspath.learning;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "app.storage.enabled=true",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class LearningServiceApplicationTests {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db")
            .withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;

    @Test
    void contextStartsWithMigratedSchemaAndPublicHealth() throws Exception {
        assertEquals("62", flyway.info().current().getVersion().getVersion());
        assertEquals(68, jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'",
                Integer.class));
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void premiumGradingConstraintsAllowFreeRowsAndRejectPaidRowsWithoutALedgerEntry() {
        UUID userId = UUID.randomUUID();
        UUID writingId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO lesson_writing_submissions
                    (id,user_id,lesson_id,block_id,question_version_id,knowledge_point_ids,request_id,essay_text,
                     word_count,prompt_snapshot,status,point_cost,result,overall_band,passed,grading_started_at,submitted_at)
                VALUES (?,?,?,?,?,ARRAY[]::uuid[],?,'essay',1,'{}','GRADED',0,'{}',6.0,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, writingId, userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM lesson_writing_submissions WHERE id=?", Integer.class,
                writingId));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO lesson_writing_submissions
                    (id,user_id,lesson_id,block_id,question_version_id,knowledge_point_ids,request_id,essay_text,
                     word_count,prompt_snapshot,status,point_cost,result,overall_band,passed,grading_started_at,submitted_at)
                VALUES (?,?,?,?,?,ARRAY[]::uuid[],?,'essay',1,'{}','GRADED',3,'{}',6.0,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        UUID speakingId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO speaking_submissions
                    (id,user_id,request_id,package_version_id,scope,status,point_cost,consent_at,created_at,updated_at,result)
                VALUES (?,?,?,?,'PART','GRADED',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'{}')
                """, speakingId, userId, UUID.randomUUID(), UUID.randomUUID());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM speaking_submissions WHERE id=?", Integer.class,
                speakingId));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO speaking_submissions
                    (id,user_id,request_id,package_version_id,scope,status,point_cost,consent_at,created_at,updated_at,result)
                VALUES (?,?,?,?,'PART','GRADED',5,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'{}')
                """, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }
}
