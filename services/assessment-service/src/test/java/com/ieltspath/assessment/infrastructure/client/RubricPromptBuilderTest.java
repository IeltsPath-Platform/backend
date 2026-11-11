package com.ieltspath.assessment.infrastructure.client;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RubricPromptBuilderTest {
    @Test void promptVersionIncludesRubricAndTemplateVersions() {
        var rubric = new com.ieltspath.assessment.application.port.ActiveRubricProvider.Rubric(
                "IELTS_ESSAY", "TASK_2", 2, "source", java.util.List.of(), java.util.List.of());
        assertEquals("IELTS_ESSAY@2+tplv1", RubricPromptBuilder.promptVersion(rubric));
    }

    @Test void classpathPromptTemplateMatchesTheSingleSourceContract() throws Exception {
        String shared = Files.readString(Path.of("..", "..", "docs", "contracts", "essay-prompt-template-v1.txt"));
        assertEquals(shared, RubricPromptBuilder.template());
        try (var input = getClass().getResourceAsStream("/essay-prompt-template-v1.txt")) {
            org.junit.jupiter.api.Assertions.assertNotNull(input);
            org.junit.jupiter.api.Assertions.assertArrayEquals(shared.getBytes(java.nio.charset.StandardCharsets.UTF_8), input.readAllBytes());
        }
    }
}
