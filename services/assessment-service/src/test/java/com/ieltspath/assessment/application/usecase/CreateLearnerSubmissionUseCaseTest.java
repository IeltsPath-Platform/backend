package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand;
import com.ieltspath.assessment.config.SpeakingRecordingProperties;
import com.ieltspath.assessment.domain.entity.LearnerSubmission;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.LearnerSubmissionRepository;
import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.commonstorage.ObjectStorage;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateLearnerSubmissionUseCaseTest {
    private final LearnerSubmissionRepository submissions = mock(LearnerSubmissionRepository.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final UUID userId = UUID.randomUUID();
    private final CreateLearnerSubmissionUseCase useCase = new CreateLearnerSubmissionUseCase(
            submissions, mock(AttemptItemRepository.class), mock(AttemptSectionRepository.class),
            mock(AssessmentAttemptRepository.class), storage,
            new SpeakingRecordingProperties("speaking-recordings", 90, 900));

    @Test
    void rejectsLocalFakeAndAnotherUsersObjectReference() {
        assertThrows(InvalidAssessmentStateException.class,
                () -> useCase.execute(command("local-recording:attempt:item")));
        String otherUserKey = "speaking/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".webm";
        assertThrows(InvalidAssessmentStateException.class, () -> useCase.execute(command(otherUserKey)));
        verifyNoInteractions(storage);
        verify(submissions, never()).save(any());
    }

    @Test
    void speakingSubmissionWithTextAndNoRecordingIsRejectedAsInvalidState() {
        var command = new CreateLearnerSubmissionCommand(
                userId, null, "{}", Skill.SPEAKING, "transcript", null, "submission-1");

        assertThrows(InvalidAssessmentStateException.class, () -> useCase.execute(command));
        verifyNoInteractions(storage);
        verify(submissions, never()).save(any());
    }

    @Test
    void speakingSubmissionWithBothPayloadsIsRejectedAsInvalidState() {
        var command = new CreateLearnerSubmissionCommand(
                userId, null, "{}", Skill.SPEAKING, "transcript", keyFor(userId), "submission-1");

        assertThrows(InvalidAssessmentStateException.class, () -> useCase.execute(command));
        verifyNoInteractions(storage);
        verify(submissions, never()).save(any());
    }

    @Test
    void rejectsAWellFormedRecordingKeyWhenTheObjectDoesNotExist() {
        String key = keyFor(userId);
        when(storage.exists("speaking-recordings", key)).thenReturn(false);

        assertThrows(InvalidAssessmentStateException.class, () -> useCase.execute(command(key)));
        verify(submissions, never()).save(any());
    }

    @Test
    void acceptsAnExistingRecordingOwnedByTheSubmitter() {
        String key = keyFor(userId);
        when(storage.exists("speaking-recordings", key)).thenReturn(true);
        when(submissions.findByUserIdAndSubmissionKey(userId, "submission-1")).thenReturn(Optional.empty());
        when(submissions.save(any())).thenAnswer(call -> call.getArgument(0));

        var result = useCase.execute(command(key));

        assertEquals(userId, result.userId());
        verify(submissions).save(any(LearnerSubmission.class));
    }

    private CreateLearnerSubmissionCommand command(String reference) {
        return new CreateLearnerSubmissionCommand(userId, null, "{}", Skill.SPEAKING, null, reference, "submission-1");
    }

    private static String keyFor(UUID owner) {
        return "speaking/" + owner + "/" + UUID.randomUUID() + ".webm";
    }
}
