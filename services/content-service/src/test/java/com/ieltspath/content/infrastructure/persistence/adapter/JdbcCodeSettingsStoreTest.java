package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.application.result.UpdateCodeSettings;
import com.ieltspath.content.application.usecase.CodeSettingsUseCases;
import com.ieltspath.content.application.usecase.CreateCourseUseCase;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.service.CodePattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers(disabledWithoutDocker = true)
class JdbcCodeSettingsStoreTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcTemplate plainJdbc;
    private static CodeSettingsStore store;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        plainJdbc = new JdbcTemplate(dataSource);
        store = new JdbcCodeSettingsStore(jdbc);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void clearCounters() {
        plainJdbc.update("DELETE FROM code_counters");
        plainJdbc.update("UPDATE code_settings SET applied = TRUE, pattern = 'COURSE_{SEQ:3}', start_number = 1, editable = TRUE WHERE code_kind = 'COURSE'");
    }

    @Test
    void migrationSeedsCourseDefaultsAndRestrictsKinds() {
        CodeSetting setting = store.find("COURSE").orElseThrow();
        assertTrue(setting.applied());
        assertEquals("COURSE_{SEQ:3}", setting.pattern());
        assertEquals(1, setting.startNumber());
        assertTrue(setting.editable());
        assertThrows(DataIntegrityViolationException.class,
                () -> plainJdbc.update("INSERT INTO code_settings(code_kind, applied, pattern, start_number, editable) VALUES ('BOGUS', false, 'X', 1, true)"));
        assertThrows(DataIntegrityViolationException.class,
                () -> plainJdbc.update("INSERT INTO code_counters(code_kind, scope_key, next_number) VALUES ('BOGUS', '', 1)"));
    }

    @Test
    void concurrentAllocationsNeverReturnTheSameNumber() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(1, 2).stream().map(ignored -> executor.submit(() -> {
                ready.countDown();
                start.await();
                return transactions.execute(status -> store.allocate("COURSE", "", 1));
            })).toList();
            ready.await();
            start.countDown();
            Set<Long> allocated = Set.of(tasks.get(0).get(), tasks.get(1).get());
            assertEquals(Set.of(1L, 2L), allocated);
        }
    }

    @Test
    void allocationRollsBackWithItsTransactionAndSeparatesScopes() {
        assertThrows(IllegalStateException.class, () -> transactions.execute(status -> {
            assertEquals(1, store.allocate("COURSE", "", 1));
            throw new IllegalStateException("rollback");
        }));
        Long afterRollback = transactions.execute(status -> store.allocate("COURSE", "", 1));
        assertEquals(1L, afterRollback.longValue());
        assertEquals(8, store.allocate("TOPIC", "course-a", 8));
        assertEquals(8, store.allocate("TOPIC", "course-b", 8));
        assertEquals(9, store.allocate("TOPIC", "course-a", 8));
    }

    @Test
    void changedStartNumberBecomesNextCourseCodeAfterTwoCreations() {
        CourseRepository courses = mock(CourseRepository.class);
        CreateCourseUseCase createCourse = mock(CreateCourseUseCase.class);
        List<CreateCourseCommand> created = new ArrayList<>();
        when(createCourse.execute(any())).thenAnswer(call -> { created.add(call.getArgument(0)); return null; });
        when(courses.existsByCode(anyString())).thenReturn(false);
        var useCases = new CodeSettingsUseCases(store, new CodePattern(), courses, createCourse);
        var command = new CreateCourseCommand("", "Course", new BigDecimal("5.5"), null, null);

        useCases.createCourse(command);
        useCases.createCourse(command);
        assertEquals(3, useCases.listConnected().get(0).startNumber());
        useCases.update("COURSE", new UpdateCodeSettings(true, "COURSE_{SEQ:3}", 10, true), UUID.randomUUID());
        useCases.createCourse(command);

        assertEquals(List.of("COURSE_001", "COURSE_002", "COURSE_010"), created.stream().map(CreateCourseCommand::code).toList());
        assertEquals(11, store.peek("COURSE", "", 1));
    }

    @Test
    void failedCourseCreationRollsBackTheAllocatedNumber() {
        CourseRepository courses = mock(CourseRepository.class);
        CreateCourseUseCase createCourse = mock(CreateCourseUseCase.class);
        when(courses.existsByCode(anyString())).thenReturn(false);
        when(createCourse.execute(any())).thenThrow(new IllegalStateException("failed"));
        var useCases = new CodeSettingsUseCases(store, new CodePattern(), courses, createCourse);
        var command = new CreateCourseCommand("", "Course", new BigDecimal("5.5"), null, null);
        assertThrows(IllegalStateException.class, () -> transactions.execute(status -> useCases.createCourse(command)));
        doReturn(null).when(createCourse).execute(any());
        useCases.createCourse(command);
        verify(createCourse, times(2)).execute(argThat(value -> value.code().equals("COURSE_001")));
    }
}
