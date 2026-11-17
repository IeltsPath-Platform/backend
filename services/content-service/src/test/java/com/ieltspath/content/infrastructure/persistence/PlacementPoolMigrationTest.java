package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class PlacementPoolMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void seedIsBackfilledAndConstraintsRejectInvalidPools() {
        assertThat(jdbc.queryForObject(
                "SELECT placement_pool FROM content_packages WHERE code='PLACEMENT-4SKILLS'", Map.of(), String.class))
                .isEqualTo("BAND_5_9");
        for (String[] invalid : List.of(new String[]{"PLACEMENT_TEST", "UNKNOWN"},
                new String[]{"PRACTICE_SET", "BAND_0_3"})) {
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO content_packages(id, code, title, package_type, placement_pool, status)
                    VALUES (:id, :code, 'Invalid', :type, :pool, 'DRAFT')
                    """, Map.of("id", UUID.randomUUID(), "code", UUID.randomUUID().toString(),
                    "type", invalid[0], "pool", invalid[1])))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Test
    void readerFiltersPublishedPoolsAndVersionCarriesPool() {
        String tag = UUID.randomUUID().toString();
        for (PlacementPool pool : PlacementPool.values()) {
            add(tag + pool.name(), pool, "PUBLISHED", "PUBLISHED");
        }
        add(tag + "draft", PlacementPool.BAND_0_3, "DRAFT", "DRAFT");
        add(tag + "archive", PlacementPool.BAND_0_3, "ARCHIVED", "PUBLISHED");
        add(tag + "draft-version", PlacementPool.BAND_0_3, "PUBLISHED", "DRAFT");

        var all = reader.placementTestPackages(null).stream().filter(p -> p.code().startsWith(tag)).toList();
        assertThat(all).hasSize(3).extracting(p -> p.pool()).containsExactlyInAnyOrder(PlacementPool.values());
        for (PlacementPool pool : PlacementPool.values()) {
            var rows = reader.placementTestPackages(pool);
            assertThat(rows).allSatisfy(p -> assertThat(p.pool()).isEqualTo(pool));
            var row = rows.stream().filter(p -> p.code().startsWith(tag)).findFirst().orElseThrow();
            assertThat(reader.publishedPackageVersion(row.packageVersionId()).orElseThrow().placementPool())
                    .isEqualTo(pool);
        }
    }

    private static void add(String code, PlacementPool pool, String packageStatus, String versionStatus) {
        UUID pkg = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO content_packages(id, code, title, package_type, placement_pool, status)
                VALUES (:id, :code, 'Pool test', 'PLACEMENT_TEST', :pool, :status)
                """, Map.of("id", pkg, "code", code, "pool", pool.name(), "status", packageStatus));
        jdbc.update("""
                INSERT INTO content_package_versions(id, package_id, version_number, status)
                VALUES (:id, :pkg, 1, :status)
                """, Map.of("id", version, "pkg", pkg, "status", versionStatus));
        jdbc.update("UPDATE content_packages SET current_published_version_id=:version WHERE id=:id",
                Map.of("id", pkg, "version", version));
    }
}
