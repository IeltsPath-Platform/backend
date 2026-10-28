package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import com.ieltspath.user.infrastructure.adapter.LearnerProfileRepositoryAdapter;
import com.ieltspath.user.infrastructure.persistence.mapper.LearnerProfileMapperImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Saving a profile through the real JPA mapping: the first save inserts, later saves update. */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({LearnerProfileRepositoryAdapter.class, LearnerProfileMapperImpl.class})
class LearnerProfilePersistenceTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired LearnerProfileRepositoryAdapter profiles;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    @Test
    void firstSaveInsertsTheProfileRowAndLaterSavesUpdateIt() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, full_name, password_hash, status) VALUES (?, ?, 'Learner', 'hash', 'ACTIVE')",
                userId, userId + "@example.test");
        LearnerProfile profile = LearnerProfile.builder().userId(userId).displayName("Learner")
                .timezone("Asia/Ho_Chi_Minh").visibility(ProfileVisibility.PUBLIC)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        profiles.save(profile);
        em.flush();

        assertEquals("Learner", jdbc.queryForObject(
                "SELECT display_name FROM learner_profiles WHERE user_id = ?", String.class, userId));

        LearnerProfile stored = profiles.findByUserId(userId).orElseThrow();
        stored.updateAvatarReference("https://example.test/avatar.png");
        profiles.save(stored);
        em.flush();

        assertEquals("https://example.test/avatar.png", jdbc.queryForObject(
                "SELECT avatar_reference FROM learner_profiles WHERE user_id = ?", String.class, userId));
        stored.updateAvatarReference(null);
        profiles.save(stored);
        em.flush();
        assertNull(jdbc.queryForObject(
                "SELECT avatar_reference FROM learner_profiles WHERE user_id = ?", String.class, userId));
    }
}
