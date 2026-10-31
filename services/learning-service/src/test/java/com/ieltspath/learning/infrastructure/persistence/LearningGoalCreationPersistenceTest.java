package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.command.CreateLearningGoalCommand;
import com.ieltspath.learning.application.usecase.CreateLearningGoalUseCase;
import com.ieltspath.learning.infrastructure.adapter.LearningGoalRepositoryAdapter;
import com.ieltspath.learning.infrastructure.persistence.mapper.LearningGoalMapperImpl;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/** Creating a learning goal through the real JPA mapping, where Hibernate decides persist versus merge. */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({LearningGoalRepositoryAdapter.class, LearningGoalMapperImpl.class})
class LearningGoalCreationPersistenceTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired LearningGoalRepositoryAdapter goals;
    @Autowired JdbcTemplate jdbc;

    @Test
    void newGoalIsInsertedAndReplacesTheActiveOne() {
        UUID userId = UUID.randomUUID();
        CreateLearningGoalUseCase useCase = new CreateLearningGoalUseCase(goals,
                mock(com.ieltspath.learning.application.usecase.ScheduleStudyGoalUseCase.class));

        var first = useCase.execute(new CreateLearningGoalCommand(userId, new BigDecimal("6.5"), null, 30));
        var second = useCase.execute(new CreateLearningGoalCommand(userId, new BigDecimal("7.0"), null, 45));

        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM learning_goals WHERE id = ?", String.class, second.id()));
        assertEquals("PAUSED", jdbc.queryForObject("SELECT status FROM learning_goals WHERE id = ?", String.class, first.id()));
    }
}
