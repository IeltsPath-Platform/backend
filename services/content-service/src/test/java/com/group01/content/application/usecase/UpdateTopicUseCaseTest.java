package com.group01.content.application.usecase;

import com.group01.content.application.command.UpdateTopicCommand;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.repository.TopicRepository;
import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateTopicUseCaseTest {
    @Test
    void assigningACourseValidatesItAndReturnsItsId() {
        var courses = mock(com.group01.content.domain.repository.CourseRepository.class);
        var topic = Topic.create(null, "TOPIC", "Topic", 1);
        UUID courseId = UUID.randomUUID();
        when(repository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(courses.findById(courseId)).thenReturn(Optional.of(new com.group01.content.domain.aggregate.Course(
                courseId, "IELTS", "IELTS", new java.math.BigDecimal("5.5"), ContentStatus.ACTIVE, null, null)));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = new UpdateTopicUseCase(repository, courses).execute(new UpdateTopicCommand(
                topic.getId(), null, "Topic", 1, null, BandRange.UNBOUNDED, null, courseId));
        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(topic.getCourseId()).isEqualTo(courseId);
    }

    @Test
    void missingCourseIsRefusedBeforeTheTopicIsChangedOrSaved() {
        var courses = mock(com.group01.content.domain.repository.CourseRepository.class);
        var topic = Topic.create(null, "TOPIC", "Original", 1);
        when(repository.findById(topic.getId())).thenReturn(Optional.of(topic));
        assertThatThrownBy(() -> new UpdateTopicUseCase(repository, courses).execute(new UpdateTopicCommand(
                topic.getId(), null, "Changed", 1, null, BandRange.UNBOUNDED, null, UUID.randomUUID())))
                .isInstanceOf(com.group01.content.domain.exception.CourseNotFoundException.class);
        assertThat(topic.getName()).isEqualTo("Original");
        verify(repository, never()).save(any());
    }

    @Test
    void nullCourseKeepsExistingMembershipWithoutReadingCourses() {
        var courses = mock(com.group01.content.domain.repository.CourseRepository.class);
        var topic = Topic.create(null, "TOPIC", "Topic", 1);
        UUID courseId = UUID.randomUUID();
        topic.assignCourse(courseId);
        when(repository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = new UpdateTopicUseCase(repository, courses).execute(new UpdateTopicCommand(
                topic.getId(), null, "Topic", 1, null, BandRange.UNBOUNDED, null, null));
        assertThat(result.courseId()).isEqualTo(courseId);
        org.mockito.Mockito.verifyNoInteractions(courses);
    }

    private final TopicRepository repository = mock(TopicRepository.class);
    private final UpdateTopicUseCase useCase = new UpdateTopicUseCase(repository);
    private final UUID id = UUID.randomUUID();

    private void givenReadingTopic(boolean hasPublishedLessons) {
        Topic topic = new Topic(id, null, "DEMO_READING", "Reading", 900, ContentStatus.ACTIVE, BandRange.UNBOUNDED,
                Skill.READING, Instant.now(), Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(topic));
        when(repository.hasPublishedLessons(id)).thenReturn(hasPublishedLessons);
        when(repository.save(any(Topic.class))).thenAnswer(call -> call.getArgument(0));
    }

    private UpdateTopicCommand command(Skill skill) {
        return new UpdateTopicCommand(id, null, "Reading", 900, null, BandRange.UNBOUNDED, skill);
    }

    @Test
    void changingTheSkillOfATopicWithPublishedLessonsIsAllowed() {
        givenReadingTopic(true);

        assertThat(useCase.execute(command(Skill.LISTENING)).skill()).isEqualTo(Skill.LISTENING);
        verify(repository).save(any());
    }

    @Test
    void aNullSkillKeepsTheCurrentOneAndTheSameSkillIsAccepted() {
        givenReadingTopic(true);

        assertThat(useCase.execute(command(null)).skill()).isEqualTo(Skill.READING);
        assertThat(useCase.execute(command(Skill.READING)).skill()).isEqualTo(Skill.READING);
    }

    @Test
    void aTopicWithoutPublishedLessonsCanChangeItsSkill() {
        givenReadingTopic(false);

        assertThat(useCase.execute(command(Skill.LISTENING)).skill()).isEqualTo(Skill.LISTENING);
    }
}
