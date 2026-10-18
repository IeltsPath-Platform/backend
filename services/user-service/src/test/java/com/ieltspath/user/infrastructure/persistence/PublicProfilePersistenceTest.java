package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.domain.vo.ProfileVisibility;
import com.ieltspath.user.infrastructure.adapter.PublicProfileRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class PublicProfilePersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static PublicProfileRepositoryAdapter repository;

    @BeforeAll
    static void setup() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new PublicProfileRepositoryAdapter(new NamedParameterJdbcTemplate(dataSource));
    }

    @Test
    void boundedJoinReturnsOneRowPerUserWithOnlyActiveTargetAndOptionalData() {
        UUID complete = user(), absent = user();
        jdbc.update("INSERT INTO learner_profiles(user_id,display_name,avatar_reference,bio,profile_visibility) VALUES (?,?,?,?,?)",
                complete, "Public display", "https://images.test/avatar.png", "About me", "COMMUNITY");
        jdbc.update("INSERT INTO learning_goals(user_id,target_band,status) VALUES (?,7.5,'ACTIVE'),(?,8.5,'PAUSED')", complete, complete);
        jdbc.update("INSERT INTO streaks(user_id,current_days,longest_days,timezone) VALUES (?,4,6,'UTC')", complete);

        var rows = repository.findByUserIds(Set.of(complete, absent, UUID.randomUUID()));

        assertEquals(2, rows.size());
        var profile = rows.stream().filter(row -> row.id().equals(complete)).findFirst().orElseThrow();
        assertEquals("Public display", profile.displayName());
        assertEquals(new BigDecimal("7.5"), profile.targetBand());
        assertEquals(4, profile.streak());
        assertEquals(ProfileVisibility.COMMUNITY, profile.visibility());
        var noProfile = rows.stream().filter(row -> row.id().equals(absent)).findFirst().orElseThrow();
        assertNull(noProfile.displayName());
        assertNull(noProfile.targetBand());
        assertNull(noProfile.streak());
        assertNull(noProfile.visibility());
        assertEquals("Account name", noProfile.defaultDisplayName());
    }

    @Test
    void nameSearchIsBoundedAndOmitsPrivateUsersFromOtherViewers() {
        UUID viewer=UUID.randomUUID(), visible=user(), hidden=user(), unrelated=user();
        jdbc.update("INSERT INTO learner_profiles(user_id,display_name,profile_visibility) VALUES (?,?,?),(?,?,?)",
                visible,"Mina IELTS","COMMUNITY",hidden,"Mina private","PRIVATE");
        jdbc.update("UPDATE users SET full_name='Mina account' WHERE id=?", unrelated);
        var results=repository.searchVisibleByName(viewer,"mina",2);
        assertEquals(2, results.size());
        assertTrue(results.stream().noneMatch(profile -> profile.id().equals(hidden)));
        assertTrue(results.stream().anyMatch(profile -> profile.id().equals(visible)));
        assertTrue(results.stream().anyMatch(profile -> profile.id().equals(unrelated)));
    }

    @Test
    void likeWildcardsAreEscapedAndDoNotMatchArbitraryUsers() {
        UUID viewer = UUID.randomUUID();
        UUID normal = user();
        UUID userWithPercent = user();
        UUID userWithUnderscore = user();
        jdbc.update("UPDATE users SET full_name='100%% guaranteed' WHERE id=?", userWithPercent);
        jdbc.update("UPDATE users SET full_name='user__name' WHERE id=?", userWithUnderscore);

        var percentResults = repository.searchVisibleByName(viewer, "%%", 10);
        assertEquals(1, percentResults.size());
        assertEquals(userWithPercent, percentResults.getFirst().id());

        var underscoreResults = repository.searchVisibleByName(viewer, "__", 10);
        assertEquals(1, underscoreResults.size());
        assertEquals(userWithUnderscore, underscoreResults.getFirst().id());
    }

    @Test
    void nonActiveUsersAreOmittedFromLookupAndSearch() {
        UUID viewer = UUID.randomUUID(), inactiveUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,'Inactive User','test-only','INACTIVE')",
                inactiveUser, inactiveUser + "@example.test");
        jdbc.update("INSERT INTO learner_profiles(user_id,display_name,profile_visibility) VALUES (?,?,'PUBLIC')",
                inactiveUser, "Inactive Display");

        var byIds = repository.findByUserIds(Set.of(inactiveUser));
        assertTrue(byIds.isEmpty());

        var searchResults = repository.searchVisibleByName(viewer, "Inactive", 10);
        assertTrue(searchResults.isEmpty());
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,'Account name','test-only','ACTIVE')",
                id, id + "@example.test");
        return id;
    }
}
