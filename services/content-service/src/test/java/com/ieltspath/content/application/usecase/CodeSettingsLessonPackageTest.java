package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.service.CodePattern;
import com.ieltspath.content.domain.vo.PackageType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CodeSettingsLessonPackageTest {
    private final CodeSettingsStore store = mock(CodeSettingsStore.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final TopicRepository topics = mock(TopicRepository.class);
    private final LessonAuthoringRepository lessons = mock(LessonAuthoringRepository.class);
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final LessonAuthoringUseCase authoring = mock(LessonAuthoringUseCase.class);
    private final CreateContentPackageUseCase packageCreator = mock(CreateContentPackageUseCase.class);
    private final CodeContextResolver resolver = new CodeContextResolver(courses, topics, lessons);
    private final CodeSettingsUseCases useCases = new CodeSettingsUseCases(store, new CodePattern(), courses, topics,
            mock(CreateCourseUseCase.class), mock(CreateTopicUseCase.class), authoring, packageCreator, resolver,
            new CodeExistence(courses, topics, lessons, packages));

    @Test
    void createsLessonWithTopicScopedCodeAndRejectsDifferentLockedCode() {
        UUID topicId = UUID.randomUUID();
        Topic topic = mock(Topic.class);
        when(topic.getCode()).thenReturn("COURSE_T01");
        when(topics.findById(topicId)).thenReturn(Optional.of(topic));
        setting("LESSON", true, "{TOPIC}_L{SEQ:2}");
        when(store.allocate("LESSON", topicId.toString(), 1)).thenReturn(1L, 2L);
        when(store.peek("LESSON", topicId.toString(), 1)).thenReturn(1L);
        when(lessons.existsByCode(anyString())).thenReturn(false);

        useCases.createLesson(topicId, "", "Lesson", null, null, null, null);
        verify(authoring).create(topicId, "COURSE_T01_L01", "Lesson", null, null, null, null);

        setting("LESSON", true, "{TOPIC}_L{SEQ:2}", false);
        when(store.allocate("LESSON", topicId.toString(), 1)).thenReturn(2L);
        var error = assertThrows(com.ieltspath.content.domain.exception.InvalidContentStateException.class,
                () -> useCases.createLesson(topicId, "MANUAL", "Lesson", null, null, null, null));
        assertEquals(com.ieltspath.content.domain.exception.InvalidContentStateException.CODE_LOCKED, error.getCode());
    }

    @Test
    void createsPackagesWithCourseTypeScopeAndFixedNoCourseFallback() {
        UUID courseId = UUID.randomUUID();
        Course course = mock(Course.class);
        when(course.getCode()).thenReturn("COURSE_001");
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        setting("PACKAGE", true, "{COURSE}_{TYPE}{SEQ:2}");
        when(store.allocate("PACKAGE", courseId + ":PS", 1)).thenReturn(1L);
        when(store.allocate("PACKAGE", "NOCOURSE:MT", 1)).thenReturn(3L);
        when(packages.existsByCode(anyString())).thenReturn(false);

        useCases.createPackage(command("", PackageType.PRACTICE_SET, courseId));
        useCases.createPackage(command("", PackageType.MOCK_TEST, null));

        var captured = org.mockito.ArgumentCaptor.forClass(CreateContentPackageCommand.class);
        verify(packageCreator, times(2)).executeAsAuthor(captured.capture());
        assertEquals("COURSE_001_PS01", captured.getAllValues().get(0).code());
        assertEquals("PKG_MT003", captured.getAllValues().get(1).code());
    }

    @Test
    void lockedPackageRejectsDifferentClientCode() {
        setting("PACKAGE", true, "{COURSE}_{TYPE}{SEQ:2}", false);
        when(store.allocate("PACKAGE", "NOCOURSE:MT", 1)).thenReturn(1L);
        when(packages.existsByCode(anyString())).thenReturn(false);

        var error = assertThrows(com.ieltspath.content.domain.exception.InvalidContentStateException.class,
                () -> useCases.createPackage(command("MANUAL", PackageType.MOCK_TEST, null)));

        assertEquals(com.ieltspath.content.domain.exception.InvalidContentStateException.CODE_LOCKED, error.getCode());
        verifyNoInteractions(packageCreator);
    }

    @Test
    void suggestsPackageFallbackAndRequiresManualCodeWhenSettingIsOff() {
        setting("PACKAGE", true, "{COURSE}_{TYPE}{SEQ:2}");
        when(store.peek("PACKAGE", "NOCOURSE:PL", 1)).thenReturn(1L);
        assertEquals("PKG_PL001", useCases.suggest("PACKAGE", null, null, null, null, null,
                PackageType.PLACEMENT_TEST).code());

        setting("PACKAGE", false, "{COURSE}_{TYPE}{SEQ:2}");
        var error = assertThrows(IllegalArgumentException.class,
                () -> useCases.createPackage(command("", PackageType.PLACEMENT_TEST, null)));
        assertTrue(error.getMessage().contains("Mã gói đề không được để trống."));
        verify(packageCreator, never()).executeAsAuthor(any());
    }

    private void setting(String kind, boolean applied, String pattern) {
        setting(kind, applied, pattern, true);
    }

    private void setting(String kind, boolean applied, String pattern, boolean editable) {
        when(store.find(kind)).thenReturn(Optional.of(new CodeSetting(kind, applied, pattern, 1, editable)));
    }

    private static CreateContentPackageCommand command(String code, PackageType type, UUID courseId) {
        return new CreateContentPackageCommand(code, "Package", type, null, null, null, null, courseId, null);
    }
}
