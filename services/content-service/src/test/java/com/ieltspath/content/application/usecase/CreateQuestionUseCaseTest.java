package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.exception.DuplicateImportRefException;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateQuestionUseCaseTest {
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionCodeGenerator codes = mock(QuestionCodeGenerator.class);
    private final CreateQuestionUseCase useCase = new CreateQuestionUseCase(questions, codes);

    @Test
    void aReusedOrMalformedImportReferenceIsRefused() {
        when(questions.findByImportRef("R-1")).thenReturn(Optional.of(
                Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null, QuestionPurpose.LEARNING, "R-1")));
        assertThatThrownBy(() -> useCase.execute(new CreateQuestionCommand(QuestionType.MULTIPLE_CHOICE, Skill.READING, null,
                QuestionPurpose.LEARNING, "R-1"))).isInstanceOf(DuplicateImportRefException.class);
        assertThatThrownBy(() -> useCase.execute(new CreateQuestionCommand(QuestionType.MULTIPLE_CHOICE, Skill.READING, null,
                QuestionPurpose.LEARNING, "bad ref!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mã tham chiếu (importRef) chỉ gồm chữ, số, dấu chấm, gạch dưới, gạch ngang và dài tối đa 100 ký tự");
        verify(questions, never()).save(any());
    }
}
