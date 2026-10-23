package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.exception.DuplicateImportRefException;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateQuestionImportRefUseCaseTest {

    private final QuestionRepository repository = mock(QuestionRepository.class);
    private final CreateQuestionUseCase useCase = new CreateQuestionUseCase(repository);

    @Test
    void newReferenceIsStoredAndReturned() {
        when(repository.findByImportRef("row-1")).thenReturn(Optional.empty());
        when(repository.save(any(Question.class))).thenAnswer(call -> call.getArgument(0));

        QuestionResult result = useCase.execute(command("  row-1 "));

        assertEquals("row-1", result.importRef());
    }

    @Test
    void referenceAlreadyUsedIsRefusedBeforeSaving() {
        when(repository.findByImportRef("row-1")).thenReturn(Optional.of(
                Question.create(QuestionType.MULTIPLE_CHOICE, null, null, QuestionPurpose.LEARNING, "row-1")));

        assertThrows(DuplicateImportRefException.class, () -> useCase.execute(command("row-1")));
        verify(repository, never()).save(any());
    }

    private static CreateQuestionCommand command(String importRef) {
        return new CreateQuestionCommand(QuestionType.MULTIPLE_CHOICE, null, null, QuestionPurpose.LEARNING, importRef);
    }
}
