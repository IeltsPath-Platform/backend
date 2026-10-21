package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.api.dto.internal.LessonContentResponse;
import com.ieltspath.content.application.usecase.GetLessonContentUseCase;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class LessonVocabularyBlockSeedTest {
    private static final List<String> LESSON_CODES = List.of("L1", "L2", "L3", "L4");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static PGSimpleDataSource dataSource;
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void eachDemoReadingLessonGetsItsApprovedVocabularyBlockAtTheBeginning() {
        GetLessonContentUseCase useCase = new GetLessonContentUseCase(reader,
                new MediaReferencePolicy("https://media.example.test/ieltspath"));
        Map<String, List<UUID>> expectedByLesson = Map.of(
                "L1", senseIds(1, 8),
                "L2", senseIds(9, 16),
                "L3", senseIds(17, 24),
                "L4", senseIds(25, 32));

        for (String code : LESSON_CODES) {
            UUID lessonId = jdbc.queryForObject("SELECT id FROM lessons WHERE code = :code", Map.of("code", code), UUID.class);
            LessonContentResponse response = LessonContentResponse.from(useCase.execute(lessonId));
            List<LessonContentResponse.Block> vocabularyBlocks = response.blocks().stream()
                    .filter(block -> block.blockType() == BlockType.VOCABULARY)
                    .toList();

            assertThat(vocabularyBlocks).as(code + " vocabulary block").hasSize(1);
            LessonContentResponse.Block vocabulary = vocabularyBlocks.getFirst();
            assertThat(vocabulary.sortOrder()).as(code + " block order")
                    .isLessThan(response.blocks().stream().filter(block -> block != vocabulary)
                            .mapToInt(LessonContentResponse.Block::sortOrder).min().orElse(Integer.MAX_VALUE));
            assertThat(vocabulary.vocabularySenseIds()).as(code + " sense IDs")
                    .containsExactlyElementsOf(expectedByLesson.get(code));
            assertThat(vocabulary.knowledgePointIds()).isEmpty();
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM lesson_blocks b
                    JOIN lessons l ON l.id = b.lesson_id
                    JOIN topics t ON t.id = l.topic_id
                    WHERE l.code = :code AND t.code = 'DEMO_READING' AND b.block_type = 'VOCABULARY'
                    """, Map.of("code", code), Integer.class)).isEqualTo(1);
        }
    }

    @Test
    void rerunningTheVocabularySeedDoesNotDuplicateBlocksOrSenseLinks() throws Exception {
        int blocksBefore = count("SELECT count(*) FROM lesson_blocks WHERE id::text LIKE '7c000000-%'");
        int linksBefore = count("SELECT count(*) FROM lesson_block_vocabulary WHERE block_id::text LIKE '7c000000-%'");

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V72__seed_lesson_vocabulary_blocks.sql"));
        }

        assertThat(count("SELECT count(*) FROM lesson_blocks WHERE id::text LIKE '7c000000-%'")).isEqualTo(blocksBefore);
        assertThat(count("SELECT count(*) FROM lesson_block_vocabulary WHERE block_id::text LIKE '7c000000-%'")).isEqualTo(linksBefore);
        assertThat(blocksBefore).isEqualTo(4);
        assertThat(linksBefore).isEqualTo(32);
    }

    private static List<UUID> senseIds(int first, int last) {
        return IntStream.rangeClosed(first, last)
                .mapToObj(index -> UUID.fromString("7a000000-0000-4000-8000-%012d".formatted(index)))
                .toList();
    }

    private static int count(String query) {
        return jdbc.getJdbcTemplate().queryForObject(query, Integer.class);
    }
}
