package com.group01.content.application.usecase;

import com.group01.content.application.command.GetGameContentSnapshotCommand;
import com.group01.content.domain.repository.QuestionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GetGameContentSnapshotUseCaseTest {
    @Test
    void refusesVocabularyDomainWithoutReadingContentDatabase() {
        QuestionRepository repository = mock(QuestionRepository.class);
        var useCase = new GetGameContentSnapshotUseCase(repository);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GetGameContentSnapshotCommand("SPELLING", "VOCABULARY", List.of(UUID.randomUUID()))));
        verifyNoInteractions(repository);
    }
}
