package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * The consumer writes the learner's band in one transaction and then matches the assignment. A SQL error in the match
 * must not leave PostgreSQL refusing the rest of the consumer's transaction, so the match runs in its own.
 */
@Testcontainers(disabledWithoutDocker = true)
class PlacementAssignmentCompleterTransactionTest {
    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static AnnotationConfigApplicationContext context;

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            var source = new PGSimpleDataSource();
            source.setURL(POSTGRES.getJdbcUrl());
            source.setUser(POSTGRES.getUsername());
            source.setPassword(POSTGRES.getPassword());
            return source;
        }

        @Bean JdbcTemplate jdbc(DataSource dataSource) { return new JdbcTemplate(dataSource); }

        @Bean PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        /** Every lookup fails with a SQL error, on the connection of the transaction it is called in. */
        @Bean PlacementAssignmentRepository assignments(JdbcTemplate jdbc) {
            return mock(PlacementAssignmentRepository.class, invocation -> {
                jdbc.execute("SELECT 1/0");
                return null;
            });
        }

        @Bean PlacementAssignmentCompleter completer(PlacementAssignmentRepository assignments) {
            return new PlacementAssignmentCompleter(assignments);
        }
    }

    @BeforeAll
    static void start() {
        context = new AnnotationConfigApplicationContext(Config.class);
        context.getBean(JdbcTemplate.class).execute("CREATE TABLE band_writes(value INT)");
    }

    @AfterAll
    static void stop() { context.close(); }

    @Test
    void aFailingMatchStaysInItsOwnTransactionAndTheBandIsStillCommitted() {
        var jdbc = context.getBean(JdbcTemplate.class);
        var completer = context.getBean(PlacementAssignmentCompleter.class);
        jdbc.update("DELETE FROM band_writes");

        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO band_writes VALUES (1)");
            assertThatThrownBy(() -> completer.complete(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now()))
                    .isInstanceOf(DataAccessException.class);
            // The consumer's transaction is still usable.
            jdbc.update("INSERT INTO band_writes VALUES (2)");
        });

        assertThat(jdbc.queryForObject("SELECT count(*) FROM band_writes", Integer.class)).isEqualTo(2);
    }

    @Test
    void theSameFailureInsideTheConsumersTransactionWouldHaveCostTheBand() {
        var jdbc = context.getBean(JdbcTemplate.class);
        jdbc.update("DELETE FROM band_writes");

        assertThatThrownBy(() -> new TransactionTemplate(context.getBean(PlatformTransactionManager.class))
                .executeWithoutResult(status -> {
                    jdbc.update("INSERT INTO band_writes VALUES (1)");
                    try {
                        jdbc.execute("SELECT 1/0");
                    } catch (DataAccessException swallowed) {
                        // swallowing it, as a try/catch around the match would
                    }
                    jdbc.update("INSERT INTO band_writes VALUES (2)");
                })).isInstanceOf(DataAccessException.class).hasMessageContaining("current transaction is aborted");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM band_writes", Integer.class)).isZero();
    }
}
