package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.command.UpsertVideoProgressCommand;
import com.ieltspath.library.application.usecase.UpsertVideoProgressUseCase;
import com.ieltspath.library.domain.vo.VideoProgressStatus;
import com.ieltspath.library.infrastructure.adapter.VideoLearningProgressRepositoryAdapter;
import com.ieltspath.library.infrastructure.persistence.mapper.VideoLearningProgressMapperImpl;
import com.ieltspath.library.infrastructure.persistence.repository.VideoLearningProgressJpaRepository;
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

import javax.sql.DataSource;
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

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void secondUpsertUpdatesTheSameRow() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "INSERT INTO learning_videos (id, youtube_video_id, youtube_url, title) VALUES (?, ?, ?, ?)")) {
            statement.setObject(1, videoId);
            statement.setString(2, videoId.toString().substring(0, 16));
            statement.setString(3, "https://example.test");
            statement.setString(4, "video");
            statement.executeUpdate();
        }
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
