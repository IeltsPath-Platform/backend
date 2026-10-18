package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.*;
import com.ieltspath.learning.application.port.LearningProgressReader;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LearningStatisticsTest {
    final UUID user = UUID.randomUUID(), course = UUID.randomUUID(), topic = UUID.randomUUID();
    final LearningProgressReader reader = mock(LearningProgressReader.class);
    final LearningContentClient content = mock(LearningContentClient.class);
    final GetLearningProgressStatisticsUseCase useCase = new GetLearningProgressStatisticsUseCase(reader, content);

    @Test void countsDistinctMultiSkillLessonsAndKeepsMissingSkillNull() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        when(reader.startedCourses(user)).thenReturn(List.of(new LearningProgressReader.CourseActivity(course, null, null)));
        var mixed = lesson(first, Set.of(LearningSkill.READING, LearningSkill.LISTENING));
        when(content.progressMetadata(List.of(course))).thenReturn(new ProgressMetadata("revision", true,
                List.of(mixed, mixed, lesson(second, Set.of(LearningSkill.READING)))));
        when(reader.completedLessons(user, List.of(first, second))).thenReturn(Set.of(first));
        var result = useCase.execute(user, null, 0, 4);
        assertThat(result.totalCourses()).isEqualTo(1);
        assertThat(result.courses().getFirst().completedLessons()).isEqualTo(1);
        assertThat(result.courses().getFirst().totalLessons()).isEqualTo(2);
        assertThat(result.courses().getFirst().percent()).isEqualByComparingTo("50.0");
        assertThat(result.courses().getFirst().startedAt()).isNull();
        assertThat(result.courses().getFirst().activityProvenance()).isEqualTo("LEGACY_UNKNOWN");
        var bySkill = new EnumMap<LearningSkill, com.ieltspath.learning.application.result.LearningProgressStatistics.SkillProgress>(LearningSkill.class);
        result.skills().forEach(s -> bySkill.put(s.skill(), s));
        assertThat(bySkill.get(LearningSkill.READING).percent()).isEqualByComparingTo("50.0");
        assertThat(bySkill.get(LearningSkill.LISTENING).percent()).isEqualByComparingTo("100.0");
        assertThat(bySkill.get(LearningSkill.SPEAKING).percent()).isNull();
        verify(content, times(1)).progressMetadata(List.of(course));
    }

    @Test void paginationDoesNotChangeSkillDenominatorsAndCourseFilterRestrictsScope() {
        UUID other = UUID.randomUUID(), lesson = UUID.randomUUID(), otherLesson = UUID.randomUUID();
        when(reader.startedCourses(user)).thenReturn(List.of(new LearningProgressReader.CourseActivity(course, null, null),
                new LearningProgressReader.CourseActivity(other, null, null)));
        when(content.progressMetadata(List.of(course, other))).thenReturn(new ProgressMetadata("all", true,
                List.of(lesson(lesson, Set.of(LearningSkill.READING)), new ProgressLesson(other,"Other",BigDecimal.TEN,topic,otherLesson,Set.of(LearningSkill.READING)))));
        when(reader.completedLessons(any(), anyList())).thenReturn(Set.of(lesson));
        var result = useCase.execute(user, null, 0, 1);
        assertThat(result.courses()).hasSize(1);
        assertThat(result.totalCourses()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.skills()).filteredOn(s -> s.skill() == LearningSkill.READING).first()
                .satisfies(s -> assertThat(s.totalLessons()).isEqualTo(2));
        when(content.progressMetadata(List.of(course))).thenReturn(new ProgressMetadata("one", true,List.of(lesson(lesson, Set.of(LearningSkill.READING)))));
        assertThat(useCase.execute(user, course, 0, 4).scope()).isEqualTo("COURSE");
    }

    @Test void rejectsTruncatedMetadataAndUnknownCoursesInsteadOfReturningFalsePercentages() {
        when(reader.startedCourses(user)).thenReturn(List.of(new LearningProgressReader.CourseActivity(course, Instant.now(), Instant.now())));
        when(content.progressMetadata(List.of(course))).thenReturn(new ProgressMetadata("too-big", false,List.of()));
        assertThatThrownBy(() -> useCase.execute(user,null,0,4)).isInstanceOfSatisfying(LearningRequestException.class,
                e -> assertThat(e.getStatus()).isEqualTo(503));
        assertThatThrownBy(() -> useCase.execute(user,UUID.randomUUID(),0,4)).isInstanceOfSatisfying(LearningRequestException.class,
                e -> assertThat(e.getStatus()).isEqualTo(404));
        verify(reader,never()).completedLessons(any(),anyList());
    }

    @Test void emptyScopeDoesNotCallContentAndPageBoundsAreCheckedBeforeReads() {
        when(reader.startedCourses(user)).thenReturn(List.of());
        var result=useCase.execute(user,null,0,4);
        assertThat(result.availability()).isEqualTo("EMPTY");
        assertThat(result.skills()).allSatisfy(s -> assertThat(s.percent()).isNull());
        verifyNoInteractions(content);
        clearInvocations(reader);
        assertThatThrownBy(() -> useCase.execute(user,null,0,21)).isInstanceOf(LearningRequestException.class);
        verifyNoInteractions(reader);
    }

    private ProgressLesson lesson(UUID id,Set<LearningSkill> skills) {
        return new ProgressLesson(course,"Course", new BigDecimal("5.5"),topic,id,skills);
    }

    @Test void oneHundredStartedCoursesUseOneCompleteMetadataBatch() {
        var activities = java.util.stream.IntStream.range(0,100)
                .mapToObj(i -> new LearningProgressReader.CourseActivity(UUID.randomUUID(), null, null)).toList();
        var courseIds = activities.stream().map(LearningProgressReader.CourseActivity::courseId).toList();
        var lessons = courseIds.stream().map(id -> new ProgressLesson(id,"Course",BigDecimal.TEN,
                topic,UUID.randomUUID(),Set.of(LearningSkill.READING))).toList();
        when(reader.startedCourses(user)).thenReturn(activities);
        when(content.progressMetadata(courseIds)).thenReturn(new ProgressMetadata("all",true,lessons));

        var result = useCase.execute(user,null,0,4);

        assertThat(result.totalCourses()).isEqualTo(100);
        assertThat(result.skills()).filteredOn(s -> s.skill() == LearningSkill.READING).first()
                .satisfies(s -> assertThat(s.totalLessons()).isEqualTo(100));
        verify(content).progressMetadata(courseIds);
    }

    @Test void oneHundredAndOneStartedCoursesRejectsWholeScopeEvenWithCourseFilter() {
        var activities = java.util.stream.IntStream.range(0,101)
                .mapToObj(i -> new LearningProgressReader.CourseActivity(UUID.randomUUID(), null, null)).toList();
        when(reader.startedCourses(user)).thenReturn(activities);

        for (UUID selectedCourse : Arrays.asList(null, activities.getFirst().courseId())) {
            assertThatThrownBy(() -> useCase.execute(user,selectedCourse,0,4))
                    .isInstanceOfSatisfying(LearningRequestException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(422);
                        assertThat(e.getCode()).isEqualTo("PROGRESS_SCOPE_TOO_LARGE");
                        assertThat(e.getMessage()).contains("100 started courses");
                    });
        }
        verifyNoInteractions(content);
        verify(reader,never()).completedLessons(any(),anyList());
    }
}
