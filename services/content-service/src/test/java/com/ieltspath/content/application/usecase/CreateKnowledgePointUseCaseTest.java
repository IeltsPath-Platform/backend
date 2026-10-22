package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateKnowledgePointCommand;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.exception.InvalidKnowledgePointCodeException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateKnowledgePointUseCaseTest {
    @Mock private KnowledgePointRepository knowledgePointRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private CourseRepository courseRepository;
    @InjectMocks private CreateKnowledgePointUseCase useCase;

    @Test
    void defaultsCefrFromTheTopicCourseAndPersistsMetadata() {
        UUID topicId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Topic topic = new Topic(topicId, null, "TOPIC", "Topic", 1, ContentStatus.ACTIVE,
                null, Skill.WRITING, Instant.now(), Instant.now(), courseId);
        Course course = new Course(courseId, "IELTS_6_5", "IELTS 6.5", new BigDecimal("6.5"),
                ContentStatus.ACTIVE, Instant.now(), Instant.now());
        when(knowledgePointRepository.existsByCode("G60_COMPLEX_SENTENCES")).thenReturn(false);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(knowledgePointRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new CreateKnowledgePointCommand(topicId, "G60_COMPLEX_SENTENCES",
                "Complex sentences", KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, Skill.ALL,
                "Old summary", null, "Can form a complex sentence.", "Independent clauses", "Advanced clause structures"));

        assertThat(result.cefrLevel()).isEqualTo(CefrLevel.C1);
        assertThat(result.canDo()).isEqualTo("Can form a complex sentence.");
        assertThat(result.scopeIn()).isEqualTo("Independent clauses");
        assertThat(result.scopeOut()).isEqualTo("Advanced clause structures");
        verify(knowledgePointRepository).save(any(KnowledgePoint.class));
    }

    @Test
    void speakingPrefixRequiresSpeakingSkill() {
        var command = new CreateKnowledgePointCommand(UUID.randomUUID(), "S60_PART2_LONG_TURN",
                "Long turn", KnowledgePointKind.STRATEGY, LearningType.PROCEDURE, Skill.READING, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidKnowledgePointCodeException.class);
    }

    @Test
    void rejectsAnActivePointWhenNoCefrLevelCanBeDerivedFromACourse() {
        UUID topicId = UUID.randomUUID();
        Topic topic = new Topic(topicId, null, "FOUNDATION", "Foundation", 1, ContentStatus.ACTIVE,
                null, Skill.SPEAKING, Instant.now(), Instant.now(), null);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

        assertThatThrownBy(() -> useCase.execute(new CreateKnowledgePointCommand(topicId, "S60_PART2_LONG_TURN",
                "Long turn", KnowledgePointKind.STRATEGY, LearningType.PROCEDURE, Skill.SPEAKING, null,
                null, "Deliver a long turn.", null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cefrLevel");
    }

    @Test
    void grammarAndVocabularyPrefixesRequireMatchingKindAndAllSkill() {
        var command = new CreateKnowledgePointCommand(UUID.randomUUID(), "G55_COMPLEX_SENTENCES",
                "Complex sentences", KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, Skill.WRITING, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidKnowledgePointCodeException.class);
    }
}
