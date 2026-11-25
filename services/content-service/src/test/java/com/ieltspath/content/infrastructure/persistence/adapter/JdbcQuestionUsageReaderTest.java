package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionUsageReader.Kind;
import com.ieltspath.content.application.port.QuestionUsageReader.Usage;
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

/** The sections and lesson blocks that hold any version of a question, read on the real schema. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcQuestionUsageReaderTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static JdbcQuestionUsageReader reader;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        reader = new JdbcQuestionUsageReader(jdbc);
    }

    @Test
    void listsPackageSectionsAndLessonBlocksThatHoldAnyVersionOfTheQuestion() {
        UUID question = question();
        UUID v1 = version(question, 1);
        UUID v2 = version(question, 2);
        UUID packageId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type) VALUES(?,?,'Exam A','MOCK_TEST')", packageId, "USE_P_" + packageId);
        UUID pv1 = packageVersion(packageId, 1);
        UUID pv2 = packageVersion(packageId, 2);
        link(pv1, "Reading 1", v1);
        link(pv2, "Reading 1", v2);
        UUID lesson = lesson("Grammar basics");
        UUID block = UUID.randomUUID();
        jdbc.update("INSERT INTO lesson_blocks(id,lesson_id,sort_order,block_type) VALUES(?,?,3,'EXERCISE')", block, lesson);
        jdbc.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES(?,?,1)", block, v1);
        link(packageVersion(otherPackage(), 1), "Unrelated", version(question(), 1));

        List<Usage> found = reader.usagesOf(question);

        assertThat(found).hasSize(3);
        assertThat(found).filteredOn(u -> u.kind() == Kind.PACKAGE).extracting(Usage::targetVersionNumber, Usage::questionVersionNumber)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(2, 2), org.assertj.core.groups.Tuple.tuple(1, 1));
        Usage inLesson = found.stream().filter(u -> u.kind() == Kind.LESSON).findFirst().orElseThrow();
        assertThat(inLesson.targetTitle()).isEqualTo("Grammar basics");
        assertThat(inLesson.location()).isEqualTo("Khối 3");
        assertThat(inLesson.targetVersionNumber()).isNull();
    }

    @Test
    void aQuestionUsedNowhereHasNoUsages() {
        assertThat(reader.usagesOf(question())).isEmpty();
    }

    private static UUID question() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id, code, question_type, skill, purpose, status) "
                + "VALUES(?, 'Q-' || replace(?::text, '-', ''), 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED')", id, id);
        return id;
    }

    private static UUID version(UUID question, int number) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id, question_id, version_number, stem, status) VALUES(?, ?, ?, 'Stem', 'PUBLISHED')", id, question, number);
        return id;
    }

    private static UUID otherPackage() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type) VALUES(?,?,'Other','MOCK_TEST')", id, "USE_O_" + id);
        return id;
    }

    private static UUID packageVersion(UUID packageId, int number) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number) VALUES(?,?,?)", id, packageId, number);
        return id;
    }

    private static void link(UUID packageVersion, String sectionTitle, UUID questionVersion) {
        UUID section = UUID.randomUUID();
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,skill,sort_order) VALUES(?,?,?,'READING',1)", section, packageVersion, sectionTitle);
        jdbc.update("INSERT INTO section_questions(id,section_id,question_version_id,sort_order) VALUES(?,?,?,1)", UUID.randomUUID(), section, questionVersion);
    }

    private static UUID lesson(String title) {
        UUID topic = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order) VALUES(?,?,?,?)", topic, "USE_T_" + topic, "Fixture", 800_000 + (int) (Math.random() * 90_000));
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order) VALUES(?,?,?,?,1)", id, topic, "USE_L_" + id, title);
        return id;
    }
}
