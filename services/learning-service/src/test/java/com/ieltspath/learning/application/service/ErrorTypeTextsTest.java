package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorTypeTextsTest {
    @Test
    void mapsNameAndFixHintAndDegradesToAnEmptyMapWhenContentFails() {
        LearningContentClient content = mock(LearningContentClient.class);
        when(content.activeErrorTypes()).thenReturn(List.of(
                new LearningContentClient.ErrorType("SPELLING", "Spelling", "LANGUAGE", 1, "Check spelling")));
        var subject = new ErrorTypeTexts(content);
        assertThat(subject.byCode()).containsEntry("SPELLING", new ErrorTypeTexts.Text("Spelling", "Check spelling"));

        when(content.activeErrorTypes()).thenThrow(new IllegalStateException("unavailable"));
        assertThat(subject.byCode()).isEqualTo(Map.of());
    }
}
