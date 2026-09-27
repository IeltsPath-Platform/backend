package com.group01.content.application.usecase;

import com.group01.content.application.command.AddQuestionVersionCommand;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.entity.QuestionKnowledgePoint;
import com.group01.content.domain.entity.QuestionVersion;
import com.group01.content.domain.repository.QuestionRepository;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddQuestionVersionUseCaseTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private AddQuestionVersionUseCase addQuestionVersionUseCase;

    @Test
    @DisplayName("Knowledge points are mapped to the version being created, whose id only the server knows")
    void mapsKnowledgePointsToTheNewVersion() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.WRITING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID knowledgePointId = UUID.randomUUID();

        addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(question.getId(), 1, "Stem", null, null,
                null, null, List.of(new AddQuestionVersionCommand.KnowledgePointInput(
                        null, knowledgePointId, new BigDecimal("0.50")))));

        QuestionVersion version = question.getVersions().get(0);
        assertThat(version.getKnowledgePoints())
                .extracting(QuestionKnowledgePoint::getQuestionVersionId, QuestionKnowledgePoint::getKnowledgePointId,
                        QuestionKnowledgePoint::getWeight)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(version.getId(), knowledgePointId,
                        new BigDecimal("0.50")));
    }

    @Test
    @DisplayName("A client-supplied version id cannot attach a mapping to another version")
    void ignoresAClientSuppliedVersionId() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.WRITING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(question.getId(), 1, "Stem", null, null,
                null, null, List.of(new AddQuestionVersionCommand.KnowledgePointInput(
                        UUID.randomUUID(), UUID.randomUUID(), null))));

        QuestionVersion version = question.getVersions().get(0);
        assertThat(version.getKnowledgePoints()).singleElement()
                .extracting(QuestionKnowledgePoint::getQuestionVersionId).isEqualTo(version.getId());
    }
}
