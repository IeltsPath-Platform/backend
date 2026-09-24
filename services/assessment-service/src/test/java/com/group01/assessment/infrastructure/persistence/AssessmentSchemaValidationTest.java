package com.group01.assessment.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers(disabledWithoutDocker = true)
class AssessmentSchemaValidationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void flywaySchemaMatchesEntitiesAndStoresV5AssessmentRows() throws Exception {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load()
                .migrate();

        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactory.setPackagesToScan("com.group01.assessment.infrastructure.persistence.entity");
        // Same naming strategies Spring Boot applies at runtime, so validation sees the real column names.
        entityManagerFactory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", "validate",
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
                "hibernate.implicit_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"));
        entityManagerFactory.afterPropertiesSet();

        try {
            insertV5Rows();
            assertStoredV5Rows();
        } finally {
            entityManagerFactory.destroy();
        }
    }

    private void insertV5Rows() throws Exception {
        try (Connection connection = postgres.createConnection(""); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO assessment_attempts
                        (id, user_id, package_version_id, attempt_type, mode, channel, status, started_at)
                    VALUES ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
                            '20000000-0000-0000-0000-000000000001', 'PLACEMENT', 'STANDARD', 'WEB', 'SUBMITTED', CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO attempt_sections (id, attempt_id, content_section_id, sort_order, section_snapshot)
                    VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
                            '20000000-0000-0000-0000-000000000002', 0, '{\"section\": 1}')
                    """);
            statement.executeUpdate("""
                    INSERT INTO attempt_items (id, attempt_section_id, question_version_id, sort_order, question_snapshot)
                    VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002',
                            '20000000-0000-0000-0000-000000000003', 0, '{\"question\": 1}')
                    """);
            statement.executeUpdate("""
                    INSERT INTO assessment_results (id, attempt_id, result_version, status)
                    VALUES ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 1, 'COMPLETED')
                    """);
            statement.executeUpdate("""
                    INSERT INTO item_results
                        (id, result_id, attempt_item_id, score, is_correct, duration_milliseconds, feedback_snapshot)
                    VALUES ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000004',
                            '00000000-0000-0000-0000-000000000003', 4.25, TRUE, 1200, '{\"feedback\": \"ok\"}')
                    """);
            statement.executeUpdate("""
                    INSERT INTO error_analysis_items (id, item_result_id, knowledge_point_id, error_type, explanation)
                    VALUES ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000005',
                            '20000000-0000-0000-0000-000000000006', 'LEXICAL', 'word choice')
                    """);
            statement.executeUpdate("""
                    INSERT INTO video_practice_attempts
                        (id, user_id, video_id, segment_id, practice_type, reference_text_snapshot, status)
                    VALUES ('00000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001',
                            '20000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000008',
                            'DICTATION', 'reference words', 'IN_PROGRESS')
                    """);
        }
    }

    private void assertStoredV5Rows() throws Exception {
        try (Connection connection = postgres.createConnection(""); Statement statement = connection.createStatement()) {
            try (var result = statement.executeQuery("""
                    SELECT ir.score, ir.duration_milliseconds, ir.feedback_snapshot::text,
                           ea.item_result_id, ea.knowledge_point_id, ea.error_type, ea.explanation,
                           vpa.segment_id, vpa.reference_text_snapshot, vpa.result_payload::text
                    FROM item_results ir
                    JOIN error_analysis_items ea ON ea.item_result_id = ir.id
                    JOIN video_practice_attempts vpa ON vpa.id = '00000000-0000-0000-0000-000000000007'
                    WHERE ir.id = '00000000-0000-0000-0000-000000000005'
                    """)) {
                result.next();
                assertEquals(4.25, result.getDouble("score"));
                assertEquals(1200, result.getLong("duration_milliseconds"));
                assertEquals("{\"feedback\": \"ok\"}", result.getString("feedback_snapshot"));
                assertEquals("00000000-0000-0000-0000-000000000005", result.getString("item_result_id"));
                assertEquals("20000000-0000-0000-0000-000000000006", result.getString("knowledge_point_id"));
                assertEquals("LEXICAL", result.getString("error_type"));
                assertEquals("word choice", result.getString("explanation"));
                assertEquals("20000000-0000-0000-0000-000000000008", result.getString("segment_id"));
                assertEquals("reference words", result.getString("reference_text_snapshot"));
                assertNull(result.getString("result_payload"));
            }
        }
    }
}
