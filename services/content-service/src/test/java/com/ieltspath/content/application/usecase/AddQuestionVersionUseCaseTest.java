package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionKnowledgePoint;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.DifficultyRequiredException;
import com.ieltspath.content.domain.exception.InvalidErrorTypeException;
import com.ieltspath.content.domain.exception.UnknownErrorCodeException;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddQuestionVersionUseCaseTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ErrorTypeRepository errorTypeRepository;

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
                null, null, QuestionDifficulty.MEDIUM, List.of(new AddQuestionVersionCommand.KnowledgePointInput(
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
                null, null, QuestionDifficulty.MEDIUM, List.of(new AddQuestionVersionCommand.KnowledgePointInput(
                        UUID.randomUUID(), UUID.randomUUID(), null))));

        QuestionVersion version = question.getVersions().get(0);
        assertThat(version.getKnowledgePoints()).singleElement()
                .extracting(QuestionKnowledgePoint::getQuestionVersionId).isEqualTo(version.getId());
    }

    @Test
    @DisplayName("A hint is stored trimmed, and a blank hint is stored as none")
    void storesTrimmedHintAndBlankAsNone() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(question.getId(), 1, "Stem", null, null,
                null, "  Read paragraph C again.  ", QuestionDifficulty.MEDIUM, null));
        addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(question.getId(), 2, "Stem", null, null,
                null, "   ", QuestionDifficulty.MEDIUM, null));

        assertThat(question.getVersions()).extracting(QuestionVersion::getHint)
                .containsExactly("Read paragraph C again.", null);
    }

    @Test
    @DisplayName("Question difficulty is required before a version can be created")
    void requiresDifficulty() {
        assertThatThrownBy(() -> addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(
                UUID.randomUUID(), 1, "Stem", null, null, null, null, null, null)))
                .isInstanceOf(DifficultyRequiredException.class);
        verifyNoInteractions(questionRepository);
    }

    @Test
    @DisplayName("A distractor naming an unknown or inactive error code is refused and nothing is saved")
    void refusesUnknownDistractorCode() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> addQuestionVersionUseCase.execute(choiceCommand(question.getId(),
                "{\"type\":\"CHOICE\",\"correct\":\"A\",\"distractors\":{\"B\":\"NOT_A_CODE\"}}")))
                .isInstanceOf(UnknownErrorCodeException.class)
                .hasMessageContaining("NOT_A_CODE");
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("A distractor naming an active error code is stored with the version")
    void acceptsActiveDistractorCode() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(errorTypeRepository.findActiveCodes(any())).thenReturn(Set.of("PARAPHRASE_TRAP"));

        addQuestionVersionUseCase.execute(choiceCommand(question.getId(),
                "{\"type\":\"CHOICE\",\"correct\":\"A\",\"distractors\":{\"B\":\"PARAPHRASE_TRAP\"}}"));

        assertThat(question.getVersions().get(0).getAnswerSpecJson()).contains("PARAPHRASE_TRAP");
    }

    @Test
    @DisplayName("A distractor key must be an option of the question")
    void distractorKeyMustBeAnOption() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> addQuestionVersionUseCase.execute(choiceCommand(question.getId(),
                "{\"type\":\"CHOICE\",\"correct\":\"A\",\"distractors\":{\"Z\":\"PARAPHRASE_TRAP\"}}")))
                .isInstanceOf(InvalidErrorTypeException.class)
                .hasMessageContaining("Z");
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("A fill question has no options, so it cannot carry distractors")
    void fillQuestionCannotCarryDistractors() {
        Question question = Question.create(QuestionType.FILL_IN_BLANK, Skill.LISTENING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(question.getId(), 1,
                "Stem", null, "{\"type\":\"FILL\",\"accepted\":[\"x\"],\"distractors\":{\"B\":\"SPELLING\"}}", null, null,
                QuestionDifficulty.MEDIUM, List.of())))
                .isInstanceOf(InvalidErrorTypeException.class);
    }

    @Test
    void addsTfngTrapForNotGivenWhenBothCodesAreActive() {
        Question question = Question.create(QuestionType.TRUE_FALSE_NOT_GIVEN, Skill.READING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(errorTypeRepository.findActiveCodes(Set.of("TFNG_NG_AS_FALSE", "TFNG_FALSE_AS_NG")))
                .thenReturn(Set.of("TFNG_NG_AS_FALSE", "TFNG_FALSE_AS_NG"));
        when(errorTypeRepository.findActiveCodes(Set.of("TFNG_NG_AS_FALSE")))
                .thenReturn(Set.of("TFNG_NG_AS_FALSE"));

        addQuestionVersionUseCase.execute(tfngCommand(question.getId(), "NOT_GIVEN",
                List.of("TRUE", "FALSE", "NOT_GIVEN"), null));

        assertThat(question.getVersions().getFirst().getAnswerSpecJson())
                .contains("\"correct\":\"NOT_GIVEN\"")
                .contains("\"FALSE\":\"TFNG_NG_AS_FALSE\"");
    }

    @Test
    void preservesDistractorsProvidedByTheAuthor() {
        Question question = Question.create(QuestionType.TRUE_FALSE_NOT_GIVEN, Skill.READING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(errorTypeRepository.findActiveCodes(Set.of("OFF_TOPIC"))).thenReturn(Set.of("OFF_TOPIC"));

        addQuestionVersionUseCase.execute(tfngCommand(question.getId(), "NOT_GIVEN",
                List.of("TRUE", "FALSE", "NOT_GIVEN"), "{\"FALSE\":\"OFF_TOPIC\"}"));

        assertThat(question.getVersions().getFirst().getAnswerSpecJson()).contains("OFF_TOPIC")
                .doesNotContain("TFNG_NG_AS_FALSE");
    }

    @Test
    void doesNotAutofillTfngWhenEitherTrapCodeIsUnavailable() {
        Question question = Question.create(QuestionType.TRUE_FALSE_NOT_GIVEN, Skill.READING, null);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(errorTypeRepository.findActiveCodes(Set.of("TFNG_NG_AS_FALSE", "TFNG_FALSE_AS_NG")))
                .thenReturn(Set.of("TFNG_NG_AS_FALSE"));

        addQuestionVersionUseCase.execute(tfngCommand(question.getId(), "NOT_GIVEN",
                List.of("TRUE", "FALSE", "NOT_GIVEN"), null));

        assertThat(question.getVersions().getFirst().getAnswerSpecJson()).doesNotContain("distractors");
    }

    @Test
    void refusesToMapAQuestionToATrapKnowledgePoint() {
        Question question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        UUID trapPointId = UUID.randomUUID();
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(errorTypeRepository.containsTrapKnowledgePoints(Set.of(trapPointId))).thenReturn(true);

        assertThatThrownBy(() -> addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(question.getId(), 1,
                "Stem", null, null, null, null, QuestionDifficulty.MEDIUM,
                List.of(new AddQuestionVersionCommand.KnowledgePointInput(null, trapPointId, BigDecimal.ONE)))))
                .isInstanceOf(InvalidErrorTypeException.class)
                .hasMessage("TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE");
        verify(questionRepository, never()).save(any(Question.class));
    }

    private static AddQuestionVersionCommand choiceCommand(UUID questionId, String answerSpecJson) {
        return new AddQuestionVersionCommand(questionId, 1, "Stem",
                List.of(new QuestionOptionPayload("A", "Right", 1), new QuestionOptionPayload("B", "Wrong", 2)),
                answerSpecJson, null, null, QuestionDifficulty.MEDIUM, List.of());
    }

    private static AddQuestionVersionCommand tfngCommand(UUID questionId, String correct, List<String> keys,
                                                           String distractors) {
        String suffix = distractors == null ? "" : ",\"distractors\":" + distractors;
        String answerSpec = "{\"type\":\"CHOICE\",\"correct\":\"" + correct + "\"" + suffix + "}";
        List<QuestionOptionPayload> options = keys.stream().map(key -> new QuestionOptionPayload(key, key, keys.indexOf(key) + 1)).toList();
        return new AddQuestionVersionCommand(questionId, 1, "TFNG stem", options, answerSpec, null, null,
                QuestionDifficulty.MEDIUM, List.of());
    }
}
