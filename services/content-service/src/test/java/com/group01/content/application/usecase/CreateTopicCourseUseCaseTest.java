package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateTopicCommand;
import com.group01.content.domain.aggregate.Course;
import com.group01.content.domain.exception.CourseNotFoundException;
import com.group01.content.domain.repository.CourseRepository;
import com.group01.content.domain.repository.TopicRepository;
import com.group01.content.domain.vo.BandRange;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateTopicCourseUseCaseTest {
    private final TopicRepository topics = mock(TopicRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final CreateTopicUseCase useCase = new CreateTopicUseCase(topics, courses);

    @Test
    void createsTopicsWithAnExistingCourseOrNoCourse() {
        Course course = Course.create("IELTS", "IELTS", new BigDecimal("5.5"));
        when(courses.findById(course.getId())).thenReturn(Optional.of(course));
        when(topics.save(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(useCase.execute(command(course.getId())).courseId()).isEqualTo(course.getId());
        assertThat(useCase.execute(command(null)).courseId()).isNull();
    }

    @Test
    void missingCourseIsRefusedWithoutSaving() {
        assertThatThrownBy(() -> useCase.execute(command(UUID.randomUUID())))
                .isInstanceOf(CourseNotFoundException.class);
        verify(topics, never()).save(any());
    }

    private CreateTopicCommand command(UUID courseId) {
        return new CreateTopicCommand(null, "TOPIC", "Topic", 1, BandRange.UNBOUNDED, null, courseId);
    }
}
