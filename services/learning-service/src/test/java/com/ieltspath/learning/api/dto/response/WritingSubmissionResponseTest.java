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

    private static WritingGrade detailedGrade() {
        var practice = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var first = new WritingGrade.Correction("He go", "He goes", "GRAMMAR", "SVA", "Subject-verb", "Match the verb",
                2, practice, "KP_SVA", "Chủ ngữ số ít", "ERROR");
        var second = new WritingGrade.Correction("He go", "He goes", "GRAMMAR", "SVA", "Subject-verb", "Match the verb",
                2, practice, "KP_SVA", null, "ERROR");
        var tip = new WritingGrade.Correction("big", "huge", "VOCABULARY", "WORD", "Word choice", "Pick a precise word",
                1, null, null, "Từ chính xác hơn", "SUGGESTION");
        var criterion = new WritingGrade.Criterion("TR", new BigDecimal("6.0"), List.of(), List.of(), null, List.of(),
                List.of("He go"), "Band 6 descriptor");
        return new WritingGrade(List.of(criterion), List.of(first, second, tip), "Summary", new BigDecimal("6.0"),
                List.of(), null, null, List.of(), List.of(),
                List.of(new WritingGrade.ErrorExample("SVA", "She go", "She goes")));
    }

    private static final String DETAILED_ESSAY = "He go. It is big. He go.";

    private com.fasterxml.jackson.databind.JsonNode detailedResponse() throws Exception {
        var result = new WritingSubmissionResult(UUID.randomUUID(), "GRADED", "TASK_2", 250, new BigDecimal("6.0"), true,
                detailedGrade(), 3, null, null, null, DETAILED_ESSAY);
        return json.readTree(json.writeValueAsString(WritingSubmissionResponse.from(result)));
    }

    @Test
    void correctionsCarryTheirPositionReasonAndCertainty() throws Exception {
        var corrections = detailedResponse().get("corrections");

        assertTrue(corrections.get(0).get("start").asInt() == 0 && corrections.get(0).get("end").asInt() == 5);
        assertTrue(corrections.get(1).get("start").asInt() == 18, "the second mention takes the second occurrence");
        assertTrue(corrections.get(0).get("reason").asText().equals("Chủ ngữ số ít"));
        assertFalse(corrections.get(1).has("reason"));
        assertTrue(corrections.get(2).get("certainty").asText().equals("SUGGESTION"));
    }

    @Test
    void criteriaCarryTheirEvidenceAndDescriptorWhenThereAreAny() throws Exception {
        var criterion = detailedResponse().get("criteria").get(0);

        assertTrue(criterion.get("evidence").get(0).asText().equals("He go"));
        assertTrue(criterion.get("descriptor").asText().equals("Band 6 descriptor"));
    }

    @Test
    void errorGroupsAndPrioritiesComeFromTheStoredCorrections() throws Exception {
        var tree = detailedResponse();

        var groups = tree.get("errorGroups");
        assertTrue(groups.size() == 1, "a suggestion does not make a group");
        assertTrue(groups.get(0).get("count").asInt() == 2);
        assertTrue(groups.get(0).get("example").get("right").asText().equals("She goes"));
        assertTrue(groups.get(0).get("practiceKnowledgePointId").asText().endsWith("1"));
        var priority = tree.get("priorities").get(0);
        assertTrue(priority.get("source").asText().equals("RULE"));
        assertTrue(priority.get("title").asText().equals("Subject-verb"));
    }

    @Test
    void aCriterionCarriesTheNextBandWhenOneIsKept() throws Exception {
        var criterion = new WritingGrade.Criterion("TR", new BigDecimal("6.0"), List.of(), List.of(), null, List.of(),
                List.of(), null, new WritingGrade.NextBand(new BigDecimal("7.0"), "Band 7 descriptor"));
        var grade = new WritingGrade(List.of(criterion), List.of(), "Summary", new BigDecimal("6.0"));

        var tree = json.readTree(json.writeValueAsString(
                WritingSubmissionResponse.from(graded("TASK_2", grade)))).get("criteria").get(0);

        assertTrue(tree.get("nextBand").get("band").decimalValue().compareTo(new BigDecimal("7.0")) == 0);
        assertTrue(tree.get("nextBand").get("descriptor").asText().equals("Band 7 descriptor"));
    }

    @Test
    void aRevisionCarriesTheComparisonWithTheEarlierVersion() throws Exception {
        UUID previous = UUID.randomUUID();
        var result = new WritingSubmissionResult(UUID.randomUUID(), "GRADED", "TASK_2", 250, new BigDecimal("6.0"), true,
                gradeWith(null, null), 3, null, null, null, "essay text",
                new WritingSubmissionResult.Revision(previous, List.of("PLURAL"), List.of("TENSE"), List.of("WORD")));

        var tree = json.readTree(json.writeValueAsString(WritingSubmissionResponse.from(result)));

        assertTrue(tree.get("revision").get("previousSubmissionId").asText().equals(previous.toString()));
        assertTrue(tree.get("revision").get("fixedCodes").get(0).asText().equals("PLURAL"));
        assertTrue(tree.get("revision").get("repeatedCodes").get(0).asText().equals("TENSE"));
        assertTrue(tree.get("revision").get("newCodes").get(0).asText().equals("WORD"));
        assertFalse(json.readTree(json.writeValueAsString(WritingSubmissionResponse.from(
                graded("TASK_2", gradeWith(null, null))))).has("revision"));
    }

    @Test
    void gradesWithoutErrorsReturnNoGroupsOrPriorities() throws Exception {
        var tree = json.readTree(json.writeValueAsString(WritingSubmissionResponse.from(
                graded("TASK_2", gradeWith(null, null)))));

        assertFalse(tree.has("errorGroups"));
        assertFalse(tree.has("priorities"));
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
