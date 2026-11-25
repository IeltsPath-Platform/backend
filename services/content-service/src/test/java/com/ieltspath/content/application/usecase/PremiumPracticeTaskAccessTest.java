package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.exception.PremiumRequiredException;
import com.ieltspath.content.application.port.LearnerEntitlement;
import com.ieltspath.content.application.port.SpeakingTaskReader;
import com.ieltspath.content.application.port.WritingTaskReader;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PremiumPracticeTaskAccessTest {
    @Test
    void taggedWritingTaskIsNotReturnedToANonPremiumLearner() {
        var reader = mock(WritingTaskReader.class);
        var mediaPolicy = mock(MediaReferencePolicy.class);
        LearnerEntitlement entitlement = () -> false;
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        var task = new WritingTaskReader.WritingTask(UUID.randomUUID(), versionId, sectionId, "Test", "Task 2",
                2, 2400, "Write an essay", UUID.randomUUID(), "Prompt", "TASK_2", 250, null, null, null,
                List.of(), "PREMIUM_PRACTICE");
        when(reader.find(versionId, sectionId)).thenReturn(Optional.of(task));

        var useCase = new GetWritingTaskUseCase(reader, mediaPolicy, entitlement);
        assertThrows(PremiumRequiredException.class, () -> useCase.execute(versionId, sectionId, true));
        verifyNoInteractions(mediaPolicy);
    }

    @Test
    void taggedSpeakingTaskIsNotReturnedToANonPremiumLearner() {
        var reader = mock(SpeakingTaskReader.class);
        LearnerEntitlement entitlement = () -> false;
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        var task = new SpeakingTaskReader.SpeakingTask(UUID.randomUUID(), versionId, sectionId, "Test", "Part 1",
                1, 600, "Answer the questions", List.of(), "PREMIUM_PRACTICE");
        when(reader.find(versionId, sectionId)).thenReturn(Optional.of(task));

        var useCase = new GetSpeakingTaskUseCase(reader, entitlement);
        assertThrows(PremiumRequiredException.class, () -> useCase.execute(versionId, sectionId, true));
    }
}
