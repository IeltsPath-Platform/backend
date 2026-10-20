package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.RecordVideoDictationAttemptCommand;
import com.ieltspath.library.application.result.VideoDictationAttemptResult;
import com.ieltspath.library.domain.aggregate.VideoDictationAttempt;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.repository.DictationVideoRepository;
import com.ieltspath.library.domain.repository.VideoDictationAttemptRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordVideoDictationAttemptUseCaseTest {
    private final DictationVideoRepository videoRepository = mock(DictationVideoRepository.class);
    private final VideoDictationAttemptRepository attemptRepository = mock(VideoDictationAttemptRepository.class);
    private final RecordVideoDictationAttemptUseCase useCase =
            new RecordVideoDictationAttemptUseCase(videoRepository, attemptRepository);

    private final UUID userId = UUID.randomUUID();
    private final UUID segmentId = UUID.randomUUID();
    private final UUID videoId = UUID.randomUUID();

    @Test
    void numbersTheAttemptAfterTheLearnersPreviousOnesAndTakesTheVideoFromTheSegment() {
        when(videoRepository.findPublishedVideoIdBySegmentId(segmentId)).thenReturn(Optional.of(videoId));
        when(attemptRepository.maxAttemptNo(userId, segmentId)).thenReturn(2);
        when(attemptRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        VideoDictationAttemptResult result = useCase.execute(command("She go to school", false, "83.33"));

        ArgumentCaptor<VideoDictationAttempt> saved = ArgumentCaptor.forClass(VideoDictationAttempt.class);
        verify(attemptRepository).save(saved.capture());
        assertEquals(3, saved.getValue().getAttemptNo());
        assertEquals(videoId, saved.getValue().getVideoId());
        assertEquals(3, result.attemptNo());
        assertEquals(false, result.correct());
    }

    @Test
    void firstAttemptIsNumberOne() {
        when(videoRepository.findPublishedVideoIdBySegmentId(segmentId)).thenReturn(Optional.of(videoId));
        when(attemptRepository.maxAttemptNo(userId, segmentId)).thenReturn(0);
        when(attemptRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertEquals(1, useCase.execute(command("She goes to school", true, "100")).attemptNo());
    }

    @Test
    void unknownOrUnpublishedSegmentIsNotFound() {
        when(videoRepository.findPublishedVideoIdBySegmentId(segmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(command("text", true, "100")));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void rejectsAccuracyOutsideZeroToHundred() {
        when(videoRepository.findPublishedVideoIdBySegmentId(segmentId)).thenReturn(Optional.of(videoId));

        assertThrows(InvalidDataException.class, () -> useCase.execute(command("text", true, "100.5")));
        assertThrows(InvalidDataException.class, () -> useCase.execute(command("text", true, "-1")));
    }

    @Test
    void rejectsBlankTypedText() {
        when(videoRepository.findPublishedVideoIdBySegmentId(segmentId)).thenReturn(Optional.of(videoId));

        assertThrows(InvalidDataException.class, () -> useCase.execute(command("   ", false, "0")));
    }

    private RecordVideoDictationAttemptCommand command(String typed, boolean correct, String accuracy) {
        return new RecordVideoDictationAttemptCommand(userId, segmentId, typed, correct, new BigDecimal(accuracy));
    }
}
