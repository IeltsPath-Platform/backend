package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.application.result.UpdateCodeSettings;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.service.CodePattern;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeSettingsQuestionTest {
    private final CodeSettingsStore store = mock(CodeSettingsStore.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final CodeSettingsUseCases useCases = new CodeSettingsUseCases(store, new CodePattern(), null, null,
            null, null, null, null, null,
            new CodeExistence(null, null, null, null, null, null, questions), null, null);

    @Test
    void questionCodesAreSuggestedFromTheSharedCounterAndAlwaysLocked() {
        when(store.find("QUESTION")).thenReturn(Optional.of(
                new CodeSetting("QUESTION", true, "Q-{SEQ:6}", 1, false)));
        when(store.peek("QUESTION", "", 1)).thenReturn(12L);
        when(questions.existsByCode("Q-000012")).thenReturn(false);

        var suggestion = useCases.suggest("QUESTION", null);

        assertThat(suggestion.code()).isEqualTo("Q-000012");
        assertThat(suggestion.applied()).isTrue();
        assertThat(suggestion.editable()).isFalse();
    }

    @Test
    void questionSettingsCannotBeTurnedOffOrMadeEditable() {
        when(store.find("QUESTION")).thenReturn(Optional.of(
                new CodeSetting("QUESTION", true, "Q-{SEQ:6}", 1, false)));

        CodeSetting saved = useCases.update("QUESTION",
                new UpdateCodeSettings(false, "Q-{SEQ:5}", 7, true), UUID.randomUUID());

        assertThat(saved.applied()).isTrue();
        assertThat(saved.editable()).isFalse();
        verify(store).save(org.mockito.ArgumentMatchers.eq(saved), org.mockito.ArgumentMatchers.any());
        verify(store).reset("QUESTION", "", 7);
    }
}
