package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingSubmissionResponseTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void correctionSnapshotsAreReturnedButTheModelNameStaysInternal() throws Exception {
        var correction = new WritingGrade.Correction("Peoples", "People", "GRAMMAR", "PLURAL_FORM",
                "Plural form", "Use the plural form only for countable nouns.", 2, UUID.randomUUID());
        var grade = new WritingGrade(List.of(), List.of(correction), "Summary", new BigDecimal("6.0"), List.of(),
                null, "configured-model");
        var result = new WritingSubmissionResult(UUID.randomUUID(), "GRADED", "TASK_2", 250,
                new BigDecimal("6.0"), true, grade, 3, null, null, null, "essay text");

        String response = json.writeValueAsString(WritingSubmissionResponse.from(result));

        assertTrue(response.contains("PLURAL_FORM"));
        assertTrue(response.contains("Plural form"));
        assertTrue(response.contains("practiceKnowledgePointId"));
        assertFalse(response.contains("configured-model"));
        assertFalse(response.contains("\"model\""));
    }

    @Test
    void legacyCorrectionJsonStillDeserializes() throws Exception {
        WritingGrade.Correction correction = json.readValue(
                "{\"excerpt\":\"wrong\",\"suggestion\":\"right\",\"category\":\"GRAMMAR\",\"errorCode\":\"OLD_CODE\"}",
                WritingGrade.Correction.class);

        assertNull(correction.errorName());
        assertNull(correction.fixHint());
        assertNull(correction.severity());
        assertNull(correction.practiceKnowledgePointId());
    }
}
