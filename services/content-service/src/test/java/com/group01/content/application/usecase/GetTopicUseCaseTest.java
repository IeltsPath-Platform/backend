package com.group01.content.application.usecase;

import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.exception.TopicNotFoundException;
import com.group01.content.domain.repository.TopicRepository;
import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetTopicUseCaseTest {
    @Test
    void returnsTopicByIdAndRejectsUnknownId() {
        TopicRepository repository = mock(TopicRepository.class);
        UUID id = UUID.randomUUID();
        Topic topic = new Topic(id, null, "READING", "Reading", 1, ContentStatus.ACTIVE,
                BandRange.UNBOUNDED, Skill.READING, Instant.now(), Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(topic));
        var useCase = new GetTopicUseCase(repository);
        assertEquals(id, useCase.execute(id).id());
        assertEquals(Skill.READING, useCase.execute(id).skill());
        UUID missing = UUID.randomUUID();
        assertThrows(TopicNotFoundException.class, () -> useCase.execute(missing));
    }
}
