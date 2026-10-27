package com.ieltspath.user.infrastructure.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class JdbcExaminerDirectoryReaderTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static JdbcExaminerDirectoryReader reader;

    @BeforeAll
    static void setup() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        reader = new JdbcExaminerDirectoryReader(new NamedParameterJdbcTemplate(dataSource));
    }

    @Test
    void anExaminerWithoutAProfileRowIsListedWithTheDefaults() {
        UUID id = user("ACTIVE", "EXAMINER");

        var found = reader.findActive(id).orElseThrow();

        assertEquals("Account name", found.displayName());
        assertTrue(found.acceptingSubmissions());
        assertEquals(30, found.dailyLimit());
        assertNull(found.ieltsBand());
        assertEquals("[]", found.certificatesJson());
        assertTrue(reader.findAccepting().stream().anyMatch(examiner -> examiner.id().equals(id)));
    }

    @Test
    void theLearnerProfileSuppliesTheNamePhotoAndIntroduction() {
        UUID id = user("ACTIVE", "EXAMINER");
        jdbc.update("INSERT INTO learner_profiles(user_id,display_name,avatar_reference,bio) VALUES (?,?,?,?)",
                id, "Ms Lan", "https://images.test/lan.png", "Ten years of IELTS teaching");

        var found = reader.findActive(id).orElseThrow();

        assertEquals("Ms Lan", found.displayName());
        assertEquals("https://images.test/lan.png", found.avatarReference());
        assertEquals("Ten years of IELTS teaching", found.bio());
    }

    @Test
    void onlyActiveUsersWithTheExaminerRoleCount() {
        UUID learner = user("ACTIVE", "CUSTOMER");
        UUID locked = user("LOCKED", "EXAMINER");
        UUID inactive = user("INACTIVE", "EXAMINER");

        assertTrue(reader.findActive(learner).isEmpty());
        assertTrue(reader.findActive(locked).isEmpty());
        assertTrue(reader.findActive(inactive).isEmpty());
        assertTrue(reader.findAccepting().stream().noneMatch(e -> e.id().equals(learner) || e.id().equals(locked)
                || e.id().equals(inactive)));
        assertFalse(reader.saveCapacity(learner, 5));
    }

    @Test
    void severalExaminersAreReadInOneQueryAndWhoIsNotAnActiveExaminerIsLeftOut() {
        UUID accepting = user("ACTIVE", "EXAMINER");
        UUID paused = user("ACTIVE", "EXAMINER");
        reader.saveProfile(paused, null, "[]", false);
        UUID learner = user("ACTIVE", "CUSTOMER");
        UUID locked = user("LOCKED", "EXAMINER");

        var found = reader.findActive(java.util.List.of(accepting, paused, learner, locked, UUID.randomUUID()));

        assertEquals(java.util.Set.of(accepting, paused), found.stream().map(e -> e.id()).collect(java.util.stream.Collectors.toSet()));
        assertFalse(found.stream().filter(e -> e.id().equals(paused)).findFirst().orElseThrow().acceptingSubmissions());
        assertTrue(reader.findActive(java.util.List.<UUID>of()).isEmpty());
    }

    @Test
    void anExaminerWhoStoppedAcceptingIsKnownButNotListed() {
        UUID id = user("ACTIVE", "EXAMINER");
        reader.saveProfile(id, new BigDecimal("8.5"), "[{\"name\":\"CELTA\",\"issuer\":\"Cambridge\",\"year\":2018,\"score\":\"A\"}]", false);

        var found = reader.findActive(id).orElseThrow();
        assertFalse(found.acceptingSubmissions());
        assertEquals(new BigDecimal("8.5"), found.ieltsBand());
        assertTrue(found.certificatesJson().contains("CELTA"));
        assertTrue(reader.findAccepting().stream().noneMatch(e -> e.id().equals(id)));

        reader.saveProfile(id, null, "[]", true);
        assertTrue(reader.findAccepting().stream().anyMatch(e -> e.id().equals(id)));
        assertNull(reader.findActive(id).orElseThrow().ieltsBand());
    }

    @Test
    void theCapacityIsKeptApartFromTheRestOfTheProfile() {
        UUID id = user("ACTIVE", "EXAMINER");
        reader.saveProfile(id, new BigDecimal("7.0"), "[]", true);

        assertTrue(reader.saveCapacity(id, 4));
        var found = reader.findActive(id).orElseThrow();
        assertEquals(4, found.dailyLimit());
        assertEquals(new BigDecimal("7.0"), found.ieltsBand());
        assertThrows(DataIntegrityViolationException.class, () -> reader.saveCapacity(id, 51));
        assertThrows(DataIntegrityViolationException.class, () -> reader.saveCapacity(id, 0));
    }

    @Test
    void theDatabaseRefusesABandThatIsNotAHalfBand() {
        UUID id = user("ACTIVE", "EXAMINER");
        assertThrows(DataIntegrityViolationException.class,
                () -> reader.saveProfile(id, new BigDecimal("7.3"), "[]", true));
        assertThrows(DataIntegrityViolationException.class,
                () -> reader.saveProfile(id, new BigDecimal("9.5"), "[]", true));
    }

    private UUID user(String status, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,'Account name','test-only',?)",
                id, id + "@example.test", status);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?, id FROM roles WHERE name=?", id, role);
        return id;
    }
}
