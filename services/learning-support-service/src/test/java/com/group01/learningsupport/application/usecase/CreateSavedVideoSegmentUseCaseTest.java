package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.command.CreateSavedVideoSegmentCommand;
import com.group01.learningsupport.domain.exception.ConflictException;
import com.group01.learningsupport.domain.repository.SavedVideoSegmentRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateSavedVideoSegmentUseCaseTest {
    private final SavedVideoSegmentRepository repository = mock(SavedVideoSegmentRepository.class);
    private final CreateSavedVideoSegmentUseCase useCase = new CreateSavedVideoSegmentUseCase(repository);

    @Test
    void duplicateSegmentConflictFromPortIsNotSwallowed() {
        when(repository.save(any())).thenThrow(new ConflictException());

        assertThrows(ConflictException.class, () -> useCase.execute(new CreateSavedVideoSegmentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "transcript",
                null
        )));
    }
}
