package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.DuplicateCourseException;
import com.ieltspath.content.domain.repository.CourseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateCourseUseCaseTest {
    private final CourseRepository courses = mock(CourseRepository.class);
    private final CreateCourseUseCase useCase = new CreateCourseUseCase(courses);

    @Test
    void aCodeOrABandLevelAlreadyUsedIsRefused() {
        Course existing = Course.create("IELTS_5_5", "IELTS 5.5", new BigDecimal("5.5"));
        when(courses.existsByCode("IELTS_5_5")).thenReturn(true);
        when(courses.findByBandLevel(new BigDecimal("5.5"))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(new CreateCourseCommand("IELTS_5_5", "Again", new BigDecimal("6.0"))))
                .isInstanceOf(DuplicateCourseException.class).hasMessageContaining("code");
        assertThatThrownBy(() -> useCase.execute(new CreateCourseCommand("NEW", "New", new BigDecimal("5.5"))))
                .isInstanceOf(DuplicateCourseException.class).hasMessageContaining("bandLevel");
        verify(courses, never()).save(any());
    }
}
