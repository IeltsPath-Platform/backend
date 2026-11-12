package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.PracticeTestKeys;
import com.ieltspath.learning.application.port.LearningContentClient.PracticeTestQuestionKey;
import com.ieltspath.learning.application.result.PracticeProgressResult;
import com.ieltspath.learning.domain.repository.PracticeProgressRepository;
import com.ieltspath.learning.domain.repository.PracticeProgressRepository.GradedSpeaking;
import com.ieltspath.learning.domain.repository.PracticeProgressRepository.GradedWriting;
import com.ieltspath.learning.domain.vo.PracticeProgressSkill;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetPracticeProgressUseCaseTest {
    private static final UUID USER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

    private final PracticeProgressRepository progress = mock(PracticeProgressRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final GetPracticeProgressUseCase useCase = new GetPracticeProgressUseCase(progress, content);

    @Test
    void speakingPartsAreKeyedByThePackageOfTheirVersionAndTheirPart() {
        UUID version = UUID.randomUUID(), pkg = UUID.randomUUID(), submission = UUID.randomUUID();
        when(progress.gradedSpeaking(USER, SpeakingScope.PART)).thenReturn(List.of(new GradedSpeaking(submission, version, 2, NOW)));
        when(content.practiceTestKeys(List.of(version), List.of())).thenReturn(new PracticeTestKeys(Map.of(version, pkg), Map.of()));

        PracticeProgressResult result = useCase.execute(USER, PracticeProgressSkill.SPEAKING, SpeakingScope.PART);

        assertThat(result.completed()).containsExactly(new PracticeProgressResult.Key(pkg, 2, submission));
    }

    @Test
    void aFullSpeakingTestHasNoPart() {
        UUID version = UUID.randomUUID(), pkg = UUID.randomUUID(), submission = UUID.randomUUID();
        when(progress.gradedSpeaking(USER, SpeakingScope.FULL)).thenReturn(List.of(new GradedSpeaking(submission, version, null, NOW)));
        when(content.practiceTestKeys(List.of(version), List.of())).thenReturn(new PracticeTestKeys(Map.of(version, pkg), Map.of()));

        assertThat(useCase.execute(USER, PracticeProgressSkill.SPEAKING, SpeakingScope.FULL).completed())
                .containsExactly(new PracticeProgressResult.Key(pkg, null, submission));
    }

    @Test
    void anOlderAndANewerVersionOfOnePackageAreOneTestAndTheLatestAnswerIsKept() {
        UUID older = UUID.randomUUID(), newer = UUID.randomUUID(), pkg = UUID.randomUUID();
        UUID oldAnswer = UUID.randomUUID(), newAnswer = UUID.randomUUID();
        when(progress.gradedSpeaking(USER, SpeakingScope.PART)).thenReturn(List.of(
                new GradedSpeaking(newAnswer, newer, 1, NOW), new GradedSpeaking(oldAnswer, older, 1, NOW.minusSeconds(3600))));
        when(content.practiceTestKeys(anyList(), anyList())).thenReturn(new PracticeTestKeys(Map.of(older, pkg, newer, pkg), Map.of()));

        assertThat(useCase.execute(USER, PracticeProgressSkill.SPEAKING, SpeakingScope.PART).completed())
                .containsExactly(new PracticeProgressResult.Key(pkg, 1, newAnswer));
    }

    @Test
    void aVersionThatIsNoLongerAPracticeTestIsLeftOut() {
        UUID version = UUID.randomUUID();
        when(progress.gradedSpeaking(USER, SpeakingScope.PART)).thenReturn(List.of(new GradedSpeaking(UUID.randomUUID(), version, 1, NOW)));
        when(content.practiceTestKeys(anyList(), anyList())).thenReturn(new PracticeTestKeys(Map.of(), Map.of()));

        assertThat(useCase.execute(USER, PracticeProgressSkill.SPEAKING, SpeakingScope.PART).completed()).isEmpty();
    }

    @Test
    void nothingIsAskedOfContentWhenThereIsNothingDone() {
        when(progress.gradedSpeaking(USER, SpeakingScope.PART)).thenReturn(List.of());

        assertThat(useCase.execute(USER, PracticeProgressSkill.SPEAKING, SpeakingScope.PART).completed()).isEmpty();
        verify(content, never()).practiceTestKeys(anyList(), anyList());
    }

    @Test
    void writingTasksAreKeyedByThePackageAndPartOfTheirQuestion() {
        UUID question = UUID.randomUUID(), pkg = UUID.randomUUID(), submission = UUID.randomUUID();
        when(progress.gradedFreeWriting(USER)).thenReturn(List.of(new GradedWriting(submission, question, NOW)));
        when(content.practiceTestKeys(List.of(), List.of(question)))
                .thenReturn(new PracticeTestKeys(Map.of(), Map.of(question, new PracticeTestQuestionKey(pkg, 2))));

        assertThat(useCase.execute(USER, PracticeProgressSkill.WRITING, SpeakingScope.PART).completed())
                .containsExactly(new PracticeProgressResult.Key(pkg, 2, submission));
    }

    @Test
    void thereIsNoFullWritingTest() {
        assertThat(useCase.execute(USER, PracticeProgressSkill.WRITING, SpeakingScope.FULL).completed()).isEmpty();
        verify(progress, never()).gradedFreeWriting(USER);
        verify(content, never()).practiceTestKeys(anyList(), anyList());
    }
}
