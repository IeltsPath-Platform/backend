package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateTopicCommand;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.Skill;
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
        assertThat(useCase.execute(command(course.getId(), Skill.READING)).courseId()).isEqualTo(course.getId());
        assertThat(useCase.execute(command(null)).courseId()).isNull();
    }

    @Test
    void courseTopicsRequireOneSpecificSkill() {
        Course course = Course.create("IELTS", "IELTS", new BigDecimal("5.5"));
        when(courses.findById(course.getId())).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> useCase.execute(command(course.getId(), null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one skill");
        assertThatThrownBy(() -> useCase.execute(command(course.getId(), Skill.ALL)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one skill");
        verify(topics, never()).save(any());
    }

    @Test
    void missingCourseIsRefusedWithoutSaving() {
        assertThatThrownBy(() -> useCase.execute(command(UUID.randomUUID())))
                .isInstanceOf(CourseNotFoundException.class);
        verify(topics, never()).save(any());
    }

    private CreateTopicCommand command(UUID courseId) {
        return command(courseId, null);
    }

    private CreateTopicCommand command(UUID courseId, Skill skill) {
        return new CreateTopicCommand(null, "TOPIC", "Topic", 1, BandRange.UNBOUNDED, skill, courseId);
    }
}
