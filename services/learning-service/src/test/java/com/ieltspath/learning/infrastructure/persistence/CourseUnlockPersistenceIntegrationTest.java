package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false", "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Testcontainers(disabledWithoutDocker = true)
class CourseUnlockPersistenceIntegrationTest {
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
    @Autowired LearnerCourseUnlockRepository unlocks;
    @MockitoBean AccessClient access;

    private final UUID user = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM learner_course_unlocks");
    }

    @Test
    void pendingRowsAreNotOwnershipAndOnlyActiveSkillPaymentsCountTowardCombo() {
        UUID pendingId = UUID.randomUUID();
        unlocks.createPending(user, course, LearningSkill.READING, 130, pendingId);
        assertThat(unlocks.findActive(user, course)).isEmpty();

        unlocks.activate(pendingId, UUID.randomUUID());
        assertThat(unlocks.activeSkillPayments(user, course)).containsEntry(LearningSkill.READING, 130);
    }

    @Test
    void nonCancelledUnlockTargetIsUniqueAndCancellationReleasesIt() {
        unlocks.createPending(user, course, LearningSkill.READING, 130, UUID.randomUUID());

        assertThatThrownBy(() -> unlocks.createPending(user, course, LearningSkill.READING, 90, UUID.randomUUID()))
                .isInstanceOf(DuplicateKeyException.class);

        UUID pendingRequest = unlocks.findPending(user).getFirst().requestId();
        unlocks.cancel(pendingRequest);
        var replacement = unlocks.createPending(user, course, LearningSkill.READING, 90, UUID.randomUUID());
        assertThat(replacement.status()).isEqualTo(LearnerCourseUnlock.Status.PENDING);
    }

    @Test
    void comboAndSkillTargetsAreStoredIndependently() {
        var skill = unlocks.createPending(user, course, LearningSkill.READING, 130, UUID.randomUUID());
        var combo = unlocks.createPending(user, course, null, 300, UUID.randomUUID());

        assertThat(unlocks.findPending(user)).extracting(LearnerCourseUnlock::id)
                .containsExactlyInAnyOrder(skill.id(), combo.id());
    }
}
