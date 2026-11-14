package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdateCourseCommand;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.exception.DuplicateCourseException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateCourseUseCaseTest {
    private final CourseRepository courses = mock(CourseRepository.class);
    private final UpdateCourseUseCase useCase = new UpdateCourseUseCase(courses);
    private final Course five = Course.create("IELTS_5_5", "IELTS 5.5", new BigDecimal("5.5"));
    private final Course six = Course.create("IELTS_6_0", "IELTS 6.0", new BigDecimal("6.0"));

    UpdateCourseUseCaseTest() {
        when(courses.findById(five.getId())).thenReturn(Optional.of(five));
        when(courses.findById(six.getId())).thenReturn(Optional.of(six));
        when(courses.findByBandLevel(new BigDecimal("5.5"))).thenReturn(Optional.of(five));
        when(courses.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aCourseCannotMoveOntoABandAnotherCourseUses() {
        assertThatThrownBy(() -> useCase.execute(new UpdateCourseCommand(six.getId(), "IELTS 6.0", new BigDecimal("5.5"), ContentStatus.ACTIVE)))
                .isInstanceOf(DuplicateCourseException.class).hasMessageContaining("bandLevel");
        assertThat(six.getBandLevel()).isEqualByComparingTo("6.0");
        verify(courses, never()).save(any());
    }

    @Test
    void aCourseMayKeepItsOwnBand() {
        var result = useCase.execute(new UpdateCourseCommand(five.getId(), "IELTS 5.5 renamed", new BigDecimal("5.5"), ContentStatus.ACTIVE));
        assertThat(result.name()).isEqualTo("IELTS 5.5 renamed");
        verify(courses).save(five);
    }

    @Test
    void anUnknownCourseOrAMissingBandIsRefused() {
        UUID unknown = UUID.randomUUID();
        when(courses.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(new UpdateCourseCommand(unknown, "X", new BigDecimal("7.0"), null)))
                .isInstanceOf(CourseNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(new UpdateCourseCommand(five.getId(), "X", null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("bandLevel is required");
        verify(courses, never()).save(any());
    }
}
