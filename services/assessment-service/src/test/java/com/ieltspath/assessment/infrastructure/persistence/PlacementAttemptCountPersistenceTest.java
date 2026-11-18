package com.ieltspath.assessment.infrastructure.persistence;

import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The started-attempt limit counts the placement attempts a learner really took. A cancelled attempt does not count:
 * Learning gives the slot back when its assignment is released, and the two services must agree on how many are left.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="})
class PlacementAttemptCountPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired AssessmentAttemptRepository attempts;
    @Autowired JdbcTemplate jdbc;

    @Test
    void cancelledAttemptsOldAttemptsAndOtherTypesAreNotCounted() {
        UUID learner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Instant now = Instant.now();
        insert(learner, "PLACEMENT", "IN_PROGRESS", now.minusSeconds(60));
        insert(learner, "PLACEMENT", "SUBMITTED", now.minusSeconds(3600));
        insert(learner, "PLACEMENT", "CANCELLED", now.minusSeconds(120));
        insert(learner, "PLACEMENT", "SUBMITTED", now.minusSeconds(90000));
        insert(learner, "TOPIC_GATE", "IN_PROGRESS", now.minusSeconds(30));
        insert(other, "PLACEMENT", "IN_PROGRESS", now.minusSeconds(30));

        assertEquals(2, attempts.countPlacementAttemptsSince(learner, now.minusSeconds(86400)));
    }

    private void insert(UUID learner, String type, String status, Instant createdAt) {
        jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status,created_at,updated_at) "
                        + "VALUES(?,?,?,?,'STANDARD','WEB',?,?,?)", UUID.randomUUID(), learner, UUID.randomUUID(), type, status,
                java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt));
    }
}
