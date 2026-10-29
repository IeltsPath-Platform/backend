package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.domain.exception.DraftVersionExistsException;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A new draft made from a version of the seeded content, on the real schema. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CopyPackageVersionToDraftUseCase.class, ContentPackageRepositoryAdapter.class, ContentPackagePersistenceMapper.class})
@Testcontainers(disabledWithoutDocker = true)
class CopyPackageVersionToDraftIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired CopyPackageVersionToDraftUseCase copy;

    private record Source(UUID packageId, UUID versionId) {}

    /** A version with groups and linked media, in a package that has no draft yet. */
    private Source richVersionWithoutDraft() {
        return jdbc.queryForObject("""
                SELECT v.package_id, v.id FROM content_package_versions v
                WHERE NOT EXISTS (SELECT 1 FROM content_package_versions d WHERE d.package_id = v.package_id AND d.status = 'DRAFT')
                  AND EXISTS (SELECT 1 FROM content_sections s JOIN section_question_groups g ON g.section_id = s.id WHERE s.package_version_id = v.id)
                  AND EXISTS (SELECT 1 FROM content_sections s JOIN content_asset_links l ON l.section_id = s.id WHERE s.package_version_id = v.id)
                ORDER BY v.id LIMIT 1
                """, (rs, row) -> new Source(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)));
    }

    private int count(String table, String sectionColumn, UUID versionId) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " t JOIN content_sections s ON s.id = t." + sectionColumn
                + " WHERE s.package_version_id = ?", Integer.class, versionId);
    }

    @Test
    void theNewDraftHasTheSameSectionsGroupsQuestionsAndMediaAsItsSource() {
        Source source = richVersionWithoutDraft();

        AuthorVersionResult draft = copy.execute(source.packageId(), source.versionId());

        assertThat(draft.id()).isNotEqualTo(source.versionId());
        assertThat(jdbc.queryForObject("SELECT status FROM content_package_versions WHERE id = ?", String.class, draft.id()))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT version_number FROM content_package_versions WHERE id = ?", Integer.class, draft.id()))
                .isEqualTo(jdbc.queryForObject("SELECT max(version_number) FROM content_package_versions WHERE package_id = ?",
                        Integer.class, source.packageId()));
        for (String[] table : new String[][] {{"section_questions", "section_id"}, {"section_question_groups", "section_id"},
                {"content_asset_links", "section_id"}}) {
            assertThat(count(table[0], table[1], draft.id())).as(table[0])
                    .isEqualTo(count(table[0], table[1], source.versionId())).isPositive();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_sections WHERE package_version_id = ?", Integer.class, draft.id()))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM content_sections WHERE package_version_id = ?", Integer.class, source.versionId()));
    }

    @Test
    void sectionsAreCopiesWithTheirOwnIdsAndTheSourceIsUntouched() {
        Source source = richVersionWithoutDraft();
        int before = count("section_questions", "section_id", source.versionId());

        AuthorVersionResult draft = copy.execute(source.packageId(), source.versionId());

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM content_sections a JOIN content_sections b ON b.id = a.id
                WHERE a.package_version_id = ? AND b.package_version_id = ?
                """, Integer.class, draft.id(), source.versionId())).isZero();
        assertThat(count("section_questions", "section_id", source.versionId())).isEqualTo(before);
        // Each copied question stays in the group copied from its own section: the composite key would refuse otherwise.
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM section_questions q JOIN content_sections s ON s.id = q.section_id
                LEFT JOIN section_question_groups g ON g.id = q.group_id AND g.section_id = q.section_id
                WHERE s.package_version_id = ? AND q.group_id IS NOT NULL AND g.id IS NULL
                """, Integer.class, draft.id())).isZero();
    }

    @Test
    void aSecondDraftIsRefusedWhileOneIsOpen() {
        Source source = richVersionWithoutDraft();
        copy.execute(source.packageId(), source.versionId());

        assertThatThrownBy(() -> copy.execute(source.packageId(), source.versionId()))
                .isInstanceOf(DraftVersionExistsException.class);
    }

    @Test
    void aVersionOfAnotherPackageIsNotFound() {
        Source source = richVersionWithoutDraft();
        UUID foreign = jdbc.queryForObject("SELECT id FROM content_package_versions WHERE package_id <> ? LIMIT 1",
                UUID.class, source.packageId());

        assertThatThrownBy(() -> copy.execute(source.packageId(), foreign))
                .isInstanceOf(PackageVersionNotFoundException.class);
    }
}
