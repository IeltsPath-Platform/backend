package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.infrastructure.adapter.JdbcUserDisplayNameReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class UserDisplayNameReaderIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static JdbcUserDisplayNameReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        reader = new JdbcUserDisplayNameReader(new NamedParameterJdbcTemplate(source));
    }

    @Test
    void batchUsesProfileNameOrFullNameAndOmitsUnknownIdsWithoutReturningEmail() {
        UUID profiledUser = UUID.randomUUID();
        UUID userWithoutProfile = UUID.randomUUID();
        UUID unknownUser = UUID.randomUUID();
        insertUser(profiledUser, "Profile fallback", "profiled");
        insertUser(userWithoutProfile, "Account Full Name", "account");
        jdbc.update("INSERT INTO learner_profiles(user_id,display_name) VALUES (?,?)", profiledUser, "Mina Display");

        var result = reader.findDisplayNamesByIds(List.of(profiledUser, userWithoutProfile, unknownUser));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                profiledUser, "Mina Display", userWithoutProfile, "Account Full Name"));
        assertThat(result).doesNotContainKey(unknownUser);
        assertThat(result.values()).doesNotContain("profiled@example.test", "account@example.test");
    }

    private void insertUser(UUID id, String fullName, String localPart) {
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,?,'hash','ACTIVE')",
                id, localPart + "@example.test", fullName);
    }
}
