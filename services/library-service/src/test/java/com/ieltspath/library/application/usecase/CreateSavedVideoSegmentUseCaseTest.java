package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.CreateSavedVideoSegmentCommand;
import com.ieltspath.library.domain.exception.ConflictException;
import com.ieltspath.library.domain.repository.SavedVideoSegmentRepository;
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

    @Test
    void aFirstSaveIsStoredAndReturned() {
        UUID user = UUID.randomUUID();
        UUID segment = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var saved = useCase.execute(new CreateSavedVideoSegmentCommand(user, UUID.randomUUID(), segment, "transcript", null));
        org.junit.jupiter.api.Assertions.assertEquals(user, saved.userId());
        org.junit.jupiter.api.Assertions.assertEquals(segment, saved.segmentId());
        org.junit.jupiter.api.Assertions.assertEquals("transcript", saved.transcriptSnapshot());
        org.mockito.Mockito.verify(repository).save(any());
    }
}
