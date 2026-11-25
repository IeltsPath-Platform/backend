package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionPassageReader.Passage;
import com.ieltspath.content.domain.vo.AssetType;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The prompt image of a question version on the real schema: set, replaced, removed, and read back with its context. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcQuestionImageWriterTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static JdbcQuestionImageWriter writer;
    private static JdbcQuestionPassageReader reader;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        writer = new JdbcQuestionImageWriter(jdbc);
        reader = new JdbcQuestionPassageReader(jdbc);
    }

    @Test
    void theImageIsReadBackWithItsDescription() {
        UUID version = questionVersion();

        writer.replace(version, "content-image/user/chart.png", "Bar chart");

        List<Passage> found = reader.passagesOf(version);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).type()).isEqualTo(AssetType.IMAGE);
        assertThat(found.get(0).mediaReference()).isEqualTo("content-image/user/chart.png");
        assertThat(found.get(0).text()).isEqualTo("Bar chart");
    }

    @Test
    void replacingLeavesOneImageAndKeepsTheOldAsset() {
        UUID version = questionVersion();
        writer.replace(version, "content-image/user/first.png", "First");

        writer.replace(version, "content-image/user/second.png", null);

        assertThat(reader.passagesOf(version)).extracting(Passage::mediaReference).containsExactly("content-image/user/second.png");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_assets WHERE media_reference = 'content-image/user/first.png'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void aNewVersionStartsWithTheImageOfTheLatestEarlierVersionThatHasOne() {
        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id, code, question_type, skill, purpose, status) "
                + "VALUES(?, 'Q-' || replace(?::text, '-', ''), 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED')", question, question);
        UUID v1 = version(question, 1);
        UUID v2 = version(question, 2);
        UUID v3 = version(question, 3);
        writer.replace(v1, "content-image/user/old.png", "Old");
        writer.replace(v2, "content-image/user/new.png", "New");

        writer.copyFromPreviousVersion(v3);

        assertThat(reader.passagesOf(v3)).extracting(Passage::mediaReference).containsExactly("content-image/user/new.png");
        assertThat(reader.passagesOf(v2)).hasSize(1);
        UUID bare = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id, code, question_type, skill, purpose, status) "
                + "VALUES(?, 'Q-' || replace(?::text, '-', ''), 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED')", bare, bare);
        UUID first = version(bare, 1);
        writer.copyFromPreviousVersion(first);
        assertThat(reader.passagesOf(first)).isEmpty();
    }

    private static UUID version(UUID question, int number) {
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id, question_id, version_number, stem, status) VALUES(?, ?, ?, 'Describe.', 'DRAFT')",
                version, question, number);
        return version;
    }

    @Test
    void clearingRemovesOnlyThisVersionsImage() {
        UUID mine = questionVersion();
        UUID other = questionVersion();
        writer.replace(mine, "content-image/user/a.png", null);
        writer.replace(other, "content-image/user/b.png", null);

        writer.clear(mine);

        assertThat(reader.passagesOf(mine)).isEmpty();
        assertThat(reader.passagesOf(other)).hasSize(1);
    }

    private static UUID questionVersion() {
        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id, code, question_type, skill, purpose, status) "
                + "VALUES(?, 'Q-' || replace(?::text, '-', ''), 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED')", question, question);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id, question_id, version_number, stem, status) VALUES(?, ?, 1, 'Describe.', 'DRAFT')",
                version, question);
        return version;
    }
}
