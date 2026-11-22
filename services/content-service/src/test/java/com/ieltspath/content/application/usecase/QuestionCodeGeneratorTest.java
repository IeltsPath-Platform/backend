package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.service.CodePattern;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionCodeGeneratorTest {
    private final CodeSettingsStore settings = mock(CodeSettingsStore.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionCodeGenerator generator = new QuestionCodeGenerator(settings, new CodePattern(), questions);

    @Test
    void allocatesAUniqueCodeAndSkipsCodesAlreadyInUse() {
        when(settings.find("QUESTION")).thenReturn(Optional.of(new CodeSetting("QUESTION", true, "Q-{SEQ:6}", 1, false)));
        when(settings.allocate("QUESTION", "", 1)).thenReturn(1L, 2L, 3L);
        when(questions.existsByCode("Q-000001")).thenReturn(true);
        when(questions.existsByCode("Q-000002")).thenReturn(true);
        when(questions.existsByCode("Q-000003")).thenReturn(false);

        assertThat(generator.allocate()).isEqualTo("Q-000003");
    }
}
