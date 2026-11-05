package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class WritingTaskGuidesMigrationTest {

    private static final String PRACTICE_WRITING_TASKS = "SELECT p.code AS code, s.part_no AS part_no, qv.stem AS stem, qv.guide AS guide "
            + "FROM content_sections s JOIN content_packages p ON p.current_published_version_id = s.package_version_id "
            + "JOIN section_question_groups g ON g.section_id = s.id JOIN section_questions sq ON sq.group_id = g.id "
            + "JOIN question_versions qv ON qv.id = sq.question_version_id "
            + "WHERE s.skill = 'WRITING' AND p.code LIKE 'PT-WRITING-%' ORDER BY p.code, s.part_no";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static List<Map<String, Object>> tasks;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        tasks = new NamedParameterJdbcTemplate(source).queryForList(PRACTICE_WRITING_TASKS, Collections.emptyMap());
    }

    @Test
    void everyPromptHasItsOwnGuide() {
        assertThat(tasks).hasSize(24);
        assertThat(tasks).extracting(task -> task.get("guide")).doesNotContainNull().doesNotHaveDuplicates();
    }

    @Test
    void everyGuideIsLongerThanTheOldTemplateAndKeepsTheThreePartsTheScreenRelyOn() {
        assertThat(tasks).allSatisfy(task -> {
            String guide = (String) task.get("guide");
            assertThat(guide).hasSizeGreaterThan(1000).contains("## 1. Giải thích đề", "## 2.", "## 3.", "## 5.", "Lưu ý");
        });
    }

    @Test
    void taskOneGuidesCoverOverviewAndTheLengthRule() {
        assertThat(tasks).filteredOn(task -> Integer.valueOf(1).equals(task.get("part_no"))).hasSize(12).allSatisfy(task -> {
            String guide = (String) task.get("guide");
            assertThat(guide).contains("Overview", "Body 1", "Body 2", "Viết tối thiểu 150 từ trong khoảng 20 phút.");
        });
    }

    @Test
    void taskTwoGuidesCoverTheFourParagraphsAndTheLengthRule() {
        assertThat(tasks).filteredOn(task -> Integer.valueOf(2).equals(task.get("part_no"))).hasSize(12).allSatisfy(task -> {
            String guide = (String) task.get("guide");
            assertThat(guide).contains("Introduction", "Body 1", "Body 2", "Conclusion", "Viết tối thiểu 250 từ trong khoảng 40 phút.");
        });
    }

    @Test
    void aTaskOneGuideNamesTheFiguresOfItsOwnPrompt() {
        Map<String, Object> table = tasks.stream()
                .filter(task -> "PT-WRITING-07".equals(task.get("code")) && Integer.valueOf(1).equals(task.get("part_no"))).findFirst().orElseThrow();
        assertThat((String) table.get("stem")).contains("65 and over: 11 and 12");
        assertThat((String) table.get("guide")).contains("65 tuổi trở lên", "11 và 12 lượt", "từ 9 xuống 6 lượt");

        Map<String, Object> pies = tasks.stream()
                .filter(task -> "PT-WRITING-12".equals(task.get("code")) && Integer.valueOf(1).equals(task.get("part_no"))).findFirst().orElseThrow();
        assertThat((String) pies.get("guide")).contains("từ 70% xuống 28%", "từ 15% lên 42%");
    }
}
