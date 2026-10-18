package com.ieltspath.assessment.infrastructure.client;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RubricPromptBuilderTest {
    @Test void localPromptTemplateMatchesSharedContractVector() throws Exception {
        String shared = Files.readString(Path.of("..", "..", "docs", "contracts", "essay-prompt-template-v1.txt"));
        assertEquals(shared, RubricPromptBuilder.template());
    }
}
