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

    private static WritingSubmissionResult graded(String task, WritingGrade grade) {
        return new WritingSubmissionResult(UUID.randomUUID(), "GRADED", task, 250, new BigDecimal("6.0"), true, grade, 3,
                null, null, null, "essay text");
    }

    private static WritingGrade gradeWith(WritingGrade.Task1Checks task1, WritingGrade.Task2Checks task2) {
        return new WritingGrade(List.of(), List.of(), "Summary", new BigDecimal("6.0")).withChecks(task1, task2);
    }

    @Test
    void taskOneReturnsOnlyTheWrongFiguresTheCountOfRightOnesAndTheFlags() throws Exception {
        var checks = new WritingGrade.Task1Checks(List.of(
                new WritingGrade.DataCheck("Sales rose to 40.", "CORRECT", null),
                new WritingGrade.DataCheck("Sales fell to 10.", "WRONG", "Doanh số là 15."),
                new WritingGrade.DataCheck("It is a chart.", "UNVERIFIABLE", null)), 1, false, null, true,
                "I think this is sad.");

        var tree = json.readTree(json.writeValueAsString(
                WritingSubmissionResponse.from(graded("TASK_1", gradeWith(checks, null)))));

        var data = tree.get("dataChecks");
        assertTrue(data.get("wrong").size() == 1);
        assertTrue(data.get("wrong").get(0).get("excerpt").asText().equals("Sales fell to 10."));
        assertTrue(data.get("wrong").get(0).get("expected").asText().equals("Doanh số là 15."));
        assertTrue(data.get("correctCount").asInt() == 1);
        assertFalse(data.get("hasOverview").asBoolean());
        assertFalse(data.has("overviewAccurate"), "an unknown flag is left out");
        assertTrue(data.get("hasPersonalOpinion").asBoolean());
        assertFalse(tree.has("taskChecks"));
    }

    @Test
    void taskTwoReturnsTheRequirementChecksAndTheDerivedFlags() throws Exception {
        var checks = new WritingGrade.Task2Checks(
                List.of(new WritingGrade.QuestionPart("Nêu ý kiến", "FULL", "In my view"),
                        new WritingGrade.QuestionPart("Đưa giải pháp", "NONE", null)),
                new WritingGrade.Position(true, false, null, null, null),
                List.of(new WritingGrade.OutlineItem("BODY", "Kỹ năng", true, "First,")));

        var tree = json.readTree(json.writeValueAsString(
                WritingSubmissionResponse.from(graded("TASK_2", gradeWith(null, checks)))));

        var task = tree.get("taskChecks");
        assertTrue(task.get("questionParts").size() == 2);
        assertFalse(task.get("questionParts").get(1).has("excerpt"));
        assertTrue(task.get("position").get("required").asBoolean());
        assertTrue(task.get("outline").get(0).get("role").asText().equals("BODY"));
        assertTrue(task.get("partialAnswer").asBoolean());
        assertTrue(task.get("noPosition").asBoolean());
        assertFalse(task.get("inconsistentPosition").asBoolean());
        assertTrue(task.get("noConclusion").asBoolean());
        assertFalse(tree.has("dataChecks"));
    }

    @Test
    void gradesWithoutChecksAndUngradedEssaysReturnNeitherField() throws Exception {
        var old = json.readTree(json.writeValueAsString(
                WritingSubmissionResponse.from(graded("TASK_1", gradeWith(null, null)))));
        var failed = json.readTree(json.writeValueAsString(WritingSubmissionResponse.from(
                new WritingSubmissionResult(UUID.randomUUID(), "FAILED", "TASK_1", 250, null, null, null, 3, null,
                        "LLM_UNAVAILABLE", null, null))));

        for (var tree : List.of(old, failed)) {
            assertFalse(tree.has("dataChecks"));
            assertFalse(tree.has("taskChecks"));
        }
    }

    @Test
    void theChecksSurviveTheJsonStoredWithTheGrade() throws Exception {
        var task1 = new WritingGrade.Task1Checks(List.of(new WritingGrade.DataCheck("s", "WRONG", "e")), 0, true, null,
                false, null);
        var task2 = new WritingGrade.Task2Checks(List.of(new WritingGrade.QuestionPart("p", "NONE", null)),
                new WritingGrade.Position(true, true, "x", false, "y"),
                List.of(new WritingGrade.OutlineItem("BODY", "i", true, null)));

        String stored1 = json.writeValueAsString(task1);
        String stored2 = json.writeValueAsString(task2);

        org.junit.jupiter.api.Assertions.assertEquals(task1, json.readValue(stored1, WritingGrade.Task1Checks.class));
        org.junit.jupiter.api.Assertions.assertEquals(task2, json.readValue(stored2, WritingGrade.Task2Checks.class));
        assertFalse(stored2.contains("partialAnswer"), "derived flags are recomputed, never stored");
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
