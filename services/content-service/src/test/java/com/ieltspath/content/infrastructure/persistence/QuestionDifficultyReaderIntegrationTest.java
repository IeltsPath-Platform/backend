package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class QuestionDifficultyReaderIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static PGSimpleDataSource dataSource;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        reader = new JdbcLearningContentReader(new NamedParameterJdbcTemplate(dataSource));
    }

    @Test
    void readsManyDifficultyTagsInOneProjectionAndOmitsUnknownIds() {
        List<DifficultyRow> seeded = new NamedParameterJdbcTemplate(dataSource).query("""
                SELECT id, difficulty FROM question_versions
                WHERE difficulty IS NOT NULL
                ORDER BY id
                LIMIT 2
                """, Map.of(), (rs, row) -> new DifficultyRow(
                UUID.fromString(rs.getString("id")), rs.getString("difficulty")));
        assertThat(seeded).hasSize(2);

        UUID unknown = UUID.randomUUID();
        Map<UUID, QuestionDifficulty> result = reader.questionDifficulties(List.of(
                seeded.get(0).id(), seeded.get(1).id(), unknown));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                seeded.get(0).id(), QuestionDifficulty.valueOf(seeded.get(0).difficulty()),
                seeded.get(1).id(), QuestionDifficulty.valueOf(seeded.get(1).difficulty())));
        assertThat(result).doesNotContainKey(unknown);
    }

    private record DifficultyRow(UUID id, String difficulty) {}
}
