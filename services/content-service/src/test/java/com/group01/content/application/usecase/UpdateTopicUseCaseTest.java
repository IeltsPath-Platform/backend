package com.group01.content.application.usecase;

import com.group01.content.application.command.UpdateTopicCommand;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.exception.TopicSkillLockedException;
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
    void changingTheSkillOfATopicWithPublishedLessonsIsRefusedAndNothingIsSaved() {
        givenReadingTopic(true);

        assertThatThrownBy(() -> useCase.execute(command(Skill.LISTENING)))
                .isInstanceOf(TopicSkillLockedException.class);
        verify(repository, never()).save(any());
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
