package com.group01.learningsupport.infrastructure.persistence;

import com.group01.learningsupport.application.command.UpsertVideoProgressCommand;
import com.group01.learningsupport.application.usecase.UpsertVideoProgressUseCase;
import com.group01.learningsupport.domain.vo.VideoProgressStatus;
import com.group01.learningsupport.infrastructure.adapter.VideoLearningProgressRepositoryAdapter;
import com.group01.learningsupport.infrastructure.persistence.mapper.VideoLearningProgressMapperImpl;
import com.group01.learningsupport.infrastructure.persistence.repository.VideoLearningProgressJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UpsertVideoProgressUseCase.class, VideoLearningProgressRepositoryAdapter.class, VideoLearningProgressMapperImpl.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class VideoProgressUpsertTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private UpsertVideoProgressUseCase useCase;

    @Autowired
    private VideoLearningProgressJpaRepository repository;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void secondUpsertUpdatesTheSameRow() {
        UUID userId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        useCase.execute(command(userId, videoId, 10, new BigDecimal("10.00")));
        useCase.execute(command(userId, videoId, 40, new BigDecimal("40.00")));

        assertEquals(1, repository.count());
        assertEquals(40, repository.findByUserIdAndVideoId(userId, videoId).orElseThrow().getLastPositionMs());
    }

    private static UpsertVideoProgressCommand command(UUID userId, UUID videoId, int position, BigDecimal percent) {
        return new UpsertVideoProgressCommand(
                userId,
                videoId,
                position,
                0,
                percent,
                VideoProgressStatus.IN_PROGRESS,
                null,
                null,
                null
        );
    }
}
