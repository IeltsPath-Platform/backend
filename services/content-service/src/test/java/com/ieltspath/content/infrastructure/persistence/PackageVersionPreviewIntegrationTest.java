package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PackageVersionPreviewIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        reader = new JdbcLearningContentReader(new NamedParameterJdbcTemplate(source));
    }

    @Test
    void draftPackageVersionCanBeLoadedForPreviewButNotAsPublishedContent() {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID questionVersionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,status) VALUES (?,?,?,?, 'DRAFT')",
                packageId, "PREVIEW-" + packageId, "Preview", "PRACTICE_SET");
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number,status) VALUES (?,?,1,'DRAFT')",
                versionId, packageId);
        jdbc.update("INSERT INTO questions(id,question_type,skill,status) VALUES (?,?,?,'DRAFT')",
                questionId, "MULTIPLE_CHOICE", "READING");
        jdbc.update("""
                INSERT INTO question_versions(id,question_id,version_number,stem,options,answer_spec,status)
                VALUES (?,?,1,?,?::jsonb,?::jsonb,'DRAFT')
                """, questionVersionId, questionId, "Which option?", "[{\"optionKey\":\"A\",\"content\":\"A\"}]",
                "{\"type\":\"CHOICE\",\"correct\":\"A\"}");
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,skill,sort_order) VALUES (?,?,?,?,1)",
                sectionId, versionId, "Reading", "READING");
        jdbc.update("INSERT INTO section_questions(id,section_id,question_version_id,sort_order,max_score) VALUES (?,?,?,1,1)",
                UUID.randomUUID(), sectionId, questionVersionId);

        assertThat(reader.publishedPackageVersion(versionId)).isEmpty();
        PackageVersionContentResult preview = reader.packageVersionContent(versionId).orElseThrow();
        assertThat(preview.packageId()).isEqualTo(packageId);
        assertThat(preview.sections()).singleElement().satisfies(section -> {
            assertThat(section.items()).singleElement().satisfies(item -> {
                assertThat(item.questionVersionId()).isEqualTo(questionVersionId);
                try {
                    assertThat(new ObjectMapper().readTree(item.answerSpecJson()).path("correct").asText())
                            .isEqualTo("A");
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            });
        });
    }
}
