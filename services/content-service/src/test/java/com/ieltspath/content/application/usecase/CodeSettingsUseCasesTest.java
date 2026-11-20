package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.application.result.UpdateCodeSettings;
import com.ieltspath.content.application.command.CreateTopicCommand;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.service.CodePattern;
import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CodeSettingsUseCasesTest {
    private final CodeSettingsStore store = mock(CodeSettingsStore.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final CreateCourseUseCase createCourse = mock(CreateCourseUseCase.class);
    private final CodeSettingsUseCases useCases = new CodeSettingsUseCases(store, new CodePattern(), courses, createCourse);
    private CodeSetting setting;

    @BeforeEach
    void setup() {
        setting = new CodeSetting("COURSE", true, "COURSE_{SEQ:3}", 1, true);
        when(store.find("COURSE")).thenReturn(Optional.of(setting));
        when(store.peek(eq("COURSE"), eq(""), anyLong())).thenAnswer(call -> call.getArgument(2));
        AtomicLong next = new AtomicLong(1);
        when(store.allocate(eq("COURSE"), eq(""), anyLong())).thenAnswer(call -> next.getAndIncrement());
        when(courses.existsByCode(anyString())).thenAnswer(call -> false);
    }

    @Test
    void skipsExistingSequenceCodesIncludingPaddedPatterns() {
        when(courses.existsByCode("COURSE_001")).thenReturn(true);
        useCases.createCourse(command("", "6.0"));
        verify(createCourse).execute(argThat(command -> command.code().equals("COURSE_002")));
    }

    @Test
    void acceptsManualCodeWhenEditableAndGeneratesWhenCodeIsBlank() {
        useCases.createCourse(command("MANUAL", "6.0"));
        verify(createCourse).execute(argThat(command -> command.code().equals("MANUAL")));
        reset(createCourse);
        useCases.createCourse(command("", "6.0"));
        verify(createCourse).execute(argThat(command -> command.code().equals("COURSE_001")));
    }

    @Test
    void lockedModeRejectsDifferentClientCode() {
        setting = new CodeSetting("COURSE", true, "COURSE_{SEQ:3}", 1, false);
        when(store.find("COURSE")).thenReturn(Optional.of(setting));
        InvalidContentStateException error = assertThrows(InvalidContentStateException.class,
                () -> useCases.createCourse(command("MANUAL", "6.0")));
        assertEquals(InvalidContentStateException.CODE_LOCKED, error.getCode());
        verifyNoInteractions(createCourse);
    }

    @Test
    void unappliedModeKeepsManualCodeRequired() {
        setting = new CodeSetting("COURSE", false, "COURSE_{SEQ:3}", 1, true);
        when(store.find("COURSE")).thenReturn(Optional.of(setting));
        assertThrows(IllegalArgumentException.class, () -> useCases.createCourse(command("", "6.0")));
        useCases.createCourse(command("MANUAL", "6.0"));
        verify(createCourse).execute(argThat(command -> command.code().equals("MANUAL")));
    }

    @Test
    void rejectsBandOutsideHalfStepsAndSuggestsNormalizedIntegerBand() {
        assertThrows(IllegalArgumentException.class, () -> useCases.suggest("COURSE", "6.2"));
        setting = new CodeSetting("COURSE", true, "IELTS_{BAND}", 1, true);
        when(store.find("COURSE")).thenReturn(Optional.of(setting));
        assertEquals("IELTS_6_0", useCases.suggest("COURSE", "6").code());
        assertEquals("IELTS_6_5", useCases.suggest("COURSE", "6.5").code());
    }

    @Test
    void reportsCurrentNextNumberAndResetsCounterWhenSettingChanges() {
        when(store.peek("COURSE", "", 1)).thenReturn(3L);
        useCases.update("COURSE", new UpdateCodeSettings(true, "COURSE_{SEQ:3}", 10, true), UUID.randomUUID());
        verify(store).reset("COURSE", "", 10);
    }

    @Test
    void suggestsAndCreatesTopicCodesWithCourseAndSkillContext() {
        UUID courseId = UUID.randomUUID();
        TopicRepository topics = mock(TopicRepository.class);
        CreateTopicUseCase createTopic = mock(CreateTopicUseCase.class);
        Course course = mock(Course.class);
        when(course.getCode()).thenReturn("COURSE_001");
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        when(store.find("TOPIC")).thenReturn(Optional.of(new CodeSetting("TOPIC", true,
                "{COURSE}_{SKILL}_T{SEQ:2}", 1, true)));
        when(store.peek("TOPIC", courseId.toString(), 1)).thenReturn(1L);
        when(store.allocate("TOPIC", courseId.toString(), 1)).thenReturn(1L);
        when(topics.existsByCode(any())).thenReturn(false);
        var topicCodes = new CodeSettingsUseCases(store, new CodePattern(), courses, topics, createCourse, createTopic);
        assertEquals("COURSE_001_R_T01", topicCodes.suggest("TOPIC", null, courseId, Skill.READING).code());
        topicCodes.createTopic(new CreateTopicCommand(null, "", "Topic", 1, BandRange.UNBOUNDED,
                Skill.READING, courseId, null));
        verify(createTopic).execute(argThat(command -> command.code().equals("COURSE_001_R_T01")));
    }

    @Test
    void lockedTopicCodeRejectsManualCodeDifferentFromSuggestion() {
        UUID courseId = UUID.randomUUID();
        TopicRepository topics = mock(TopicRepository.class);
        CreateTopicUseCase createTopic = mock(CreateTopicUseCase.class);
        Course course = mock(Course.class);
        when(course.getCode()).thenReturn("COURSE_001");
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        when(store.find("TOPIC")).thenReturn(Optional.of(new CodeSetting("TOPIC", true,
                "{COURSE}_T{SEQ:2}", 1, false)));
        when(store.allocate("TOPIC", courseId.toString(), 1)).thenReturn(1L);
        when(topics.existsByCode(any())).thenReturn(false);
        var topicCodes = new CodeSettingsUseCases(store, new CodePattern(), courses, topics, createCourse, createTopic);
        InvalidContentStateException error = assertThrows(InvalidContentStateException.class,
                () -> topicCodes.createTopic(new CreateTopicCommand(null, "MANUAL", "Topic", 1,
                        BandRange.UNBOUNDED, Skill.READING, courseId, null)));
        assertEquals(InvalidContentStateException.CODE_LOCKED, error.getCode());
        verifyNoInteractions(createTopic);
    }

    private static CreateCourseCommand command(String code, String band) {
        return new CreateCourseCommand(code, "Course", new BigDecimal(band), null, null);
    }
}
