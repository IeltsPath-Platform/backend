package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.application.usecase.GetInternalLearningGoalUseCase;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.infrastructure.adapter.LearningGoalRepositoryAdapter;
import com.ieltspath.user.infrastructure.adapter.UserRepositoryAdapter;
import com.ieltspath.user.infrastructure.persistence.mapper.LearningGoalMapperImpl;
import com.ieltspath.user.infrastructure.persistence.mapper.RoleMapperImpl;
import com.ieltspath.user.infrastructure.persistence.mapper.UserMapperImpl;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GetInternalLearningGoalUseCase.class, LearningGoalRepositoryAdapter.class,
        LearningGoalMapperImpl.class, UserRepositoryAdapter.class, UserMapperImpl.class, RoleMapperImpl.class})
class InternalLearningGoalUseCaseIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GetInternalLearningGoalUseCase useCase;
    @Autowired JdbcTemplate jdbc;

    @Test
    void distinguishesUnknownUsersFromExistingUsersWithoutGoalsAndReturnsTheActiveGoal() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.execute(userId)).isInstanceOf(UserNotFoundException.class);

        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,?,'hash','ACTIVE')",
                userId, userId + "@example.test", "Learner");
        var noGoal = useCase.execute(userId);
        assertThat(noGoal.targetBand()).isNull();
        assertThat(noGoal.examDate()).isNull();

        LocalDate examDate = LocalDate.of(2027, 1, 20);
        jdbc.update("INSERT INTO learning_goals(id,user_id,target_band,exam_date,status) VALUES (?,?,6.5,?,'ACTIVE')",
                UUID.randomUUID(), userId, examDate);
        var active = useCase.execute(userId);
        assertThat(active.targetBand()).isEqualByComparingTo(BigDecimal.valueOf(6.5));
        assertThat(active.examDate()).isEqualTo(examDate);
    }
}
