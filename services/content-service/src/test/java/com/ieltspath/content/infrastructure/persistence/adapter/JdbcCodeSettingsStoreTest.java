package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.application.command.CreateTopicCommand;
import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.application.result.UpdateCodeSettings;
import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.usecase.CodeContextResolver;
import com.ieltspath.content.application.usecase.CodeExistence;
import com.ieltspath.content.application.usecase.CreateContentPackageUseCase;
import com.ieltspath.content.application.usecase.LessonAuthoringUseCase;
import com.ieltspath.content.application.usecase.CodeSettingsUseCases;
import com.ieltspath.content.application.usecase.CreateCourseUseCase;
import com.ieltspath.content.application.usecase.CreateTopicUseCase;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.service.CodePattern;
import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.PackageType;
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
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
        plainJdbc.update("UPDATE code_settings SET applied = TRUE, pattern = '{COURSE}_T{SEQ:2}', start_number = 1, editable = TRUE WHERE code_kind = 'TOPIC'");
        plainJdbc.update("UPDATE code_settings SET applied = TRUE, pattern = '{TOPIC}_L{SEQ:2}', start_number = 1, editable = TRUE WHERE code_kind = 'LESSON'");
        plainJdbc.update("UPDATE code_settings SET applied = TRUE, pattern = '{COURSE}_{TYPE}{SEQ:2}', start_number = 1, editable = TRUE WHERE code_kind = 'PACKAGE'");
    }

    @Test
    void migrationSeedsCourseDefaultsAndRestrictsKinds() {
        CodeSetting setting = store.find("COURSE").orElseThrow();
        assertTrue(setting.applied());
        assertEquals("COURSE_{SEQ:3}", setting.pattern());
        assertEquals(1, setting.startNumber());
        assertTrue(setting.editable());
        CodeSetting topic = store.find("TOPIC").orElseThrow();
        assertTrue(topic.applied());
        assertEquals("{COURSE}_T{SEQ:2}", topic.pattern());
        CodeSetting lesson = store.find("LESSON").orElseThrow();
        assertTrue(lesson.applied());
        assertEquals("{TOPIC}_L{SEQ:2}", lesson.pattern());
        assertEquals(1L, lesson.startNumber());
        assertTrue(lesson.editable());
        CodeSetting pkg = store.find("PACKAGE").orElseThrow();
        assertTrue(pkg.applied());
        assertEquals("{COURSE}_{TYPE}{SEQ:2}", pkg.pattern());
        assertEquals(1L, pkg.startNumber());
        assertTrue(pkg.editable());
        CodeSetting errorType = store.find("ERROR_TYPE").orElseThrow();
        assertFalse(errorType.applied());
        assertEquals("{NAME}", errorType.pattern());
        assertEquals(1L, errorType.startNumber());
        assertTrue(errorType.editable());
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
        assertEquals(8L, store.allocate("TOPIC", "course-a", 8));
        assertEquals(8L, store.allocate("TOPIC", "course-b", 8));
        assertEquals(9L, store.allocate("TOPIC", "course-a", 8));
    }

    @Test
    void topicCountersStartIndependentlyForEachCourse() {
        String firstCourse = UUID.randomUUID().toString();
        String secondCourse = UUID.randomUUID().toString();
        assertEquals(1, store.allocate("TOPIC", firstCourse, 1));
        assertEquals(1, store.allocate("TOPIC", secondCourse, 1));
        assertEquals(2, store.allocate("TOPIC", firstCourse, 1));
    }

    @Test
    void topicCreationUsesCourseScopedCountersAndRollsBackWhenCreationFails() {
        plainJdbc.update("UPDATE code_settings SET applied = TRUE, pattern = '{COURSE}_T{SEQ:2}', start_number = 1, editable = TRUE WHERE code_kind = 'TOPIC'");
        CourseRepository courses = mock(CourseRepository.class);
        TopicRepository topics = mock(TopicRepository.class);
        CreateTopicUseCase createTopic = mock(CreateTopicUseCase.class);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        Course firstCourse = mock(Course.class);
        Course secondCourse = mock(Course.class);
        when(firstCourse.getCode()).thenReturn("COURSE_A");
        when(secondCourse.getCode()).thenReturn("COURSE_B");
        when(courses.findById(firstId)).thenReturn(java.util.Optional.of(firstCourse));
        when(courses.findById(secondId)).thenReturn(java.util.Optional.of(secondCourse));
        when(topics.existsByCode(anyString())).thenReturn(false);
        var useCases = new CodeSettingsUseCases(store, new CodePattern(), courses, topics,
                mock(CreateCourseUseCase.class), createTopic);
        when(createTopic.execute(any())).thenReturn(null);

        useCases.createTopic(topicCommand(firstId));
        useCases.createTopic(topicCommand(secondId));
        var first = org.mockito.ArgumentCaptor.forClass(CreateTopicCommand.class);
        verify(createTopic, times(2)).execute(first.capture());
        assertEquals(List.of("COURSE_A_T01", "COURSE_B_T01"), first.getAllValues().stream().map(CreateTopicCommand::code).toList());

        doThrow(new IllegalStateException("failed")).when(createTopic).execute(any());
        assertThrows(IllegalStateException.class,
                () -> transactions.execute(status -> useCases.createTopic(topicCommand(firstId))));
        doReturn(null).when(createTopic).execute(any());
        useCases.createTopic(topicCommand(firstId));
        var retried = org.mockito.ArgumentCaptor.forClass(CreateTopicCommand.class);
        verify(createTopic, times(4)).execute(retried.capture());
        assertEquals("COURSE_A_T02", retried.getAllValues().get(3).code());
    }

    @Test
    void lessonCountersAreScopedByTopicAndSkipCodesAlreadyUsedManually() {
        CourseRepository courses = mock(CourseRepository.class);
        TopicRepository topics = mock(TopicRepository.class);
        LessonAuthoringRepository lessons = mock(LessonAuthoringRepository.class);
        ContentPackageRepository packages = mock(ContentPackageRepository.class);
        LessonAuthoringUseCase authoring = mock(LessonAuthoringUseCase.class);
        UUID firstTopicId = UUID.randomUUID();
        UUID secondTopicId = UUID.randomUUID();
        var firstTopic = mock(com.ieltspath.content.domain.aggregate.Topic.class);
        var secondTopic = mock(com.ieltspath.content.domain.aggregate.Topic.class);
        when(firstTopic.getCode()).thenReturn("TOPIC_A");
        when(secondTopic.getCode()).thenReturn("TOPIC_B");
        when(topics.findById(firstTopicId)).thenReturn(java.util.Optional.of(firstTopic));
        when(topics.findById(secondTopicId)).thenReturn(java.util.Optional.of(secondTopic));
        when(lessons.existsByCode("TOPIC_A_L01")).thenReturn(true);
        when(lessons.existsByCode("TOPIC_A_L02")).thenReturn(false);
        when(lessons.existsByCode("TOPIC_B_L01")).thenReturn(false);
        var useCases = codeUseCases(courses, topics, lessons, packages, authoring, mock(CreateContentPackageUseCase.class));

        transactions.execute(status -> useCases.createLesson(firstTopicId, "", "Lesson A", null, null, null, null));
        transactions.execute(status -> useCases.createLesson(secondTopicId, "", "Lesson B", null, null, null, null));

        var created = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(authoring, times(2)).create(any(), created.capture(), any(), any(), any(), any(), any());
        assertEquals(List.of("TOPIC_A_L02", "TOPIC_B_L01"), created.getAllValues());
        assertEquals(3L, store.peek("LESSON", firstTopicId.toString(), 1));
        assertEquals(2L, store.peek("LESSON", secondTopicId.toString(), 1));
    }

    @Test
    void packageCountersAreScopedByCourseAndTypeAndNoCourseUsesFallbackScope() {
        CourseRepository courses = mock(CourseRepository.class);
        TopicRepository topics = mock(TopicRepository.class);
        LessonAuthoringRepository lessons = mock(LessonAuthoringRepository.class);
        ContentPackageRepository packages = mock(ContentPackageRepository.class);
        CreateContentPackageUseCase packageCreator = mock(CreateContentPackageUseCase.class);
        UUID courseId = UUID.randomUUID();
        Course course = mock(Course.class);
        when(course.getCode()).thenReturn("COURSE_A");
        when(courses.findById(courseId)).thenReturn(java.util.Optional.of(course));
        when(packages.existsByCode(anyString())).thenReturn(false);
        List<String> codes = new ArrayList<>();
        when(packageCreator.executeAsAuthor(any())).thenAnswer(call -> {
            codes.add(((CreateContentPackageCommand) call.getArgument(0)).code());
            return null;
        });
        var useCases = codeUseCases(courses, topics, lessons, packages, mock(LessonAuthoringUseCase.class), packageCreator);

        transactions.execute(status -> useCases.createPackage(packageCommand(courseId, PackageType.PRACTICE_SET)));
        transactions.execute(status -> useCases.createPackage(packageCommand(courseId, PackageType.MOCK_TEST)));
        transactions.execute(status -> useCases.createPackage(packageCommand(null, PackageType.PLACEMENT_TEST)));

        assertEquals(List.of("COURSE_A_PS01", "COURSE_A_MT01", "PKG_PL001"), codes);
        assertEquals(2L, store.peek("PACKAGE", courseId + ":PS", 1));
        assertEquals(2L, store.peek("PACKAGE", courseId + ":MT", 1));
        assertEquals(2L, store.peek("PACKAGE", "NOCOURSE:PL", 1));
    }

    @Test
    void concurrentPackageCreationsInOneScopeReceiveDifferentCodes() throws Exception {
        CourseRepository courses = mock(CourseRepository.class);
        TopicRepository topics = mock(TopicRepository.class);
        LessonAuthoringRepository lessons = mock(LessonAuthoringRepository.class);
        ContentPackageRepository packages = mock(ContentPackageRepository.class);
        CreateContentPackageUseCase packageCreator = mock(CreateContentPackageUseCase.class);
        UUID courseId = UUID.randomUUID();
        Course course = mock(Course.class);
        when(course.getCode()).thenReturn("COURSE_A");
        when(courses.findById(courseId)).thenReturn(java.util.Optional.of(course));
        when(packages.existsByCode(anyString())).thenReturn(false);
        List<String> codes = new CopyOnWriteArrayList<>();
        when(packageCreator.executeAsAuthor(any())).thenAnswer(call -> {
            codes.add(((CreateContentPackageCommand) call.getArgument(0)).code());
            return null;
        });
        var useCases = codeUseCases(courses, topics, lessons, packages, mock(LessonAuthoringUseCase.class), packageCreator);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(1, 2).stream().map(ignored -> executor.submit(() -> {
                ready.countDown();
                start.await();
                return transactions.execute(status -> useCases.createPackage(
                        packageCommand(courseId, PackageType.PRACTICE_SET)));
            })).toList();
            ready.await();
            start.countDown();
            for (var task : tasks) task.get();
        }
        assertEquals(Set.of("COURSE_A_PS01", "COURSE_A_PS02"), new HashSet<>(codes));
    }

    private CodeSettingsUseCases codeUseCases(CourseRepository courses, TopicRepository topics,
                                              LessonAuthoringRepository lessons, ContentPackageRepository packages,
                                              LessonAuthoringUseCase lessonAuthoring,
                                              CreateContentPackageUseCase packageCreator) {
        return new CodeSettingsUseCases(store, new CodePattern(), courses, topics,
                mock(CreateCourseUseCase.class), mock(CreateTopicUseCase.class), lessonAuthoring, packageCreator,
                new CodeContextResolver(courses, topics, lessons), new CodeExistence(courses, topics, lessons, packages));
    }

    private static CreateContentPackageCommand packageCommand(UUID courseId, PackageType type) {
        return new CreateContentPackageCommand("", "Package", type, null, null, null, null, courseId,
                type == PackageType.PLACEMENT_TEST ? com.ieltspath.content.domain.vo.PlacementPool.BAND_3_5 : null);
    }

    private static CreateTopicCommand topicCommand(UUID courseId) {
        return new CreateTopicCommand(null, "", "Topic", 1, BandRange.UNBOUNDED, Skill.READING, courseId, null);
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
