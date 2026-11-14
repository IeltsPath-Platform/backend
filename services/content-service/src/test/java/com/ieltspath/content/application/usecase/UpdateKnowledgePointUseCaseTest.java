package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdateKnowledgePointCommand;
import com.ieltspath.content.application.service.TrapKnowledgePointSync;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.KnowledgePointInUseException;
import com.ieltspath.content.domain.exception.PrerequisiteLevelException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateKnowledgePointUseCaseTest {
    private final KnowledgePointRepository points = mock(KnowledgePointRepository.class);
    private final TopicRepository topics = mock(TopicRepository.class);
    private final KnowledgePrerequisiteRepository prerequisites = mock(KnowledgePrerequisiteRepository.class);
    private final TrapKnowledgePointSync traps = mock(TrapKnowledgePointSync.class);
    private final UpdateKnowledgePointUseCase useCase = new UpdateKnowledgePointUseCase(points, topics, prerequisites, traps);
    private final Topic topic = Topic.create(null, "GRAMMAR", "Grammar", 1);
    private final Topic otherTopic = Topic.create(null, "GRAMMAR_2", "Grammar 2", 2);
    private final KnowledgePoint point = KnowledgePoint.create(topic.getId(), "G60_COMPLEX_SENTENCES", "Complex sentences",
            KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, Skill.ALL, null, CefrLevel.B1, "Can form one.", null, null);

    UpdateKnowledgePointUseCaseTest() {
        when(points.findById(point.getId())).thenReturn(Optional.of(point));
        when(topics.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(topics.findById(otherTopic.getId())).thenReturn(Optional.of(otherTopic));
    }

    private UpdateKnowledgePointCommand command(UUID topicId, String code, Skill skill, CefrLevel level) {
        return new UpdateKnowledgePointCommand(topicId, code, "Complex sentences", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, skill, null, level, "Can form one.", null, null);
    }

    @Test
    void aPointInUseCannotMoveToAnotherTopic() {
        when(points.isInUse(point.getId())).thenReturn(true);
        assertThatThrownBy(() -> useCase.execute(point.getId(), command(otherTopic.getId(), point.getCode(), Skill.ALL, CefrLevel.B1)))
                .isInstanceOf(KnowledgePointInUseException.class);
        assertThat(point.getTopicId()).isEqualTo(topic.getId());
        verify(points, never()).save(any());
    }

    @Test
    void aCodeAnotherPointUsesIsRefused() {
        KnowledgePoint other = KnowledgePoint.create(topic.getId(), "G60_OTHER", "Other", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, Skill.ALL, null, CefrLevel.B1, "Can do.", null, null);
        when(points.findByCode("G60_OTHER")).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> useCase.execute(point.getId(), command(topic.getId(), "G60_OTHER", Skill.ALL, CefrLevel.B1)))
                .isInstanceOf(DuplicateCodeException.class);
        assertThat(point.getCode()).isEqualTo("G60_COMPLEX_SENTENCES");
        verify(points, never()).save(any());
    }

    @Test
    void aLevelThatBreaksThePrerequisiteOrderIsRefusedUnderTheTreeLock() {
        KnowledgePoint dependent = KnowledgePoint.create(topic.getId(), "G60_DEPENDENT", "Dependent", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, Skill.ALL, null, CefrLevel.B1, "Can do.", null, null);
        when(prerequisites.findAll()).thenReturn(List.of(new KnowledgePrerequisiteRepository.Edge(point.getId(), dependent.getId())));
        when(points.findByIds(List.of(dependent.getId()))).thenReturn(List.of(dependent));

        assertThatThrownBy(() -> useCase.execute(point.getId(), command(topic.getId(), point.getCode(), Skill.ALL, CefrLevel.C1)))
                .isInstanceOf(PrerequisiteLevelException.class);
        var order = inOrder(prerequisites);
        order.verify(prerequisites).lockTree();
        order.verify(prerequisites).findAll();
        assertThat(point.getCefrLevel()).isEqualTo(CefrLevel.B1);
        verify(points, never()).save(any());
    }

    @Test
    void aPointInACourseTopicMustMatchTheTopicSkill() {
        Topic writing = new Topic(UUID.randomUUID(), null, "WRITING", "Writing", 3, ContentStatus.ACTIVE, BandRange.UNBOUNDED,
                Skill.WRITING, Instant.now(), Instant.now(), UUID.randomUUID());
        when(topics.findById(writing.getId())).thenReturn(Optional.of(writing));
        assertThatThrownBy(() -> useCase.execute(point.getId(), command(writing.getId(), "W60_TASK_RESPONSE", Skill.READING, CefrLevel.B1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A knowledge point in a course topic must match its skill");
        verify(points, never()).save(any());
    }
}
