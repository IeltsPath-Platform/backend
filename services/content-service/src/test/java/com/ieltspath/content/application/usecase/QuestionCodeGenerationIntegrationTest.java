package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CreateQuestionUseCase.class, QuestionCodeGenerator.class,
        com.ieltspath.content.infrastructure.config.CodePatternConfiguration.class,
        com.ieltspath.content.infrastructure.persistence.adapter.JdbcCodeSettingsStore.class,
        com.ieltspath.content.infrastructure.persistence.adapter.QuestionRepositoryAdapter.class,
        com.ieltspath.content.infrastructure.persistence.mapper.QuestionPersistenceMapper.class,
        JacksonAutoConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionCodeGenerationIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired CreateQuestionUseCase createQuestion;

    @Test
    void concurrentCreatesReceiveDistinctStableCodes() throws Exception {
        int count = 8;
        var executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<String>> futures = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting to start");
                    return createQuestion.execute(new CreateQuestionCommand(QuestionType.MULTIPLE_CHOICE, null, null,
                            null, null))
                            .code();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<String> codes = new ArrayList<>();
            for (var future : futures) codes.add(future.get(30, TimeUnit.SECONDS));

            assertThat(codes).hasSize(count).doesNotHaveDuplicates().allMatch(code -> code.matches("Q-\\d{6,}"));
        } finally {
            executor.shutdownNow();
        }
    }
}
