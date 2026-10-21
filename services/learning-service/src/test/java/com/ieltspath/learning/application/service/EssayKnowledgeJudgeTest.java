package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EssayKnowledgeJudgeTest {
    private static final String ESSAY = "I used however to connect the ideas.";
    private static final UUID LINKING = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private final ObjectMapper json = new ObjectMapper();
    private final EssayKnowledgeJudge judge = new EssayKnowledgeJudge();
    private com.fasterxml.jackson.databind.JsonNode entries;

    private static LearningContentClient.RubricCriterion criterion(List<LearningContentClient.SubKnowledgePoint> points) {
        return new LearningContentClient.RubricCriterion("CC", "Coherence and Cohesion", "Join ideas", 1,
                List.of(), TOPIC, points);
    }

    private static WritingGrade grade(List<WritingGrade.Criterion> criteria, List<WritingGrade.Correction> corrections) {
        return new WritingGrade(criteria, corrections, "Summary", new BigDecimal("6.0"));
    }

    private static String judgments(String entries) {
        return "{\"judgments\":[" + entries + "]}";
    }

    private static String judgment(String code, String value, String excerpt) {
        return "{\"criterion\":\"CC\",\"code\":\"" + code + "\",\"judgment\":\"" + value
                + "\",\"excerpt\":\"" + excerpt + "\",\"reason\":\"Giải thích\"}";
    }

    private WritingGrade apply(List<LearningContentClient.RubricCriterion> rubric, WritingGrade base,
                               Map<String, LearningContentClient.ErrorType> errors,
                               java.util.function.Supplier<Map<UUID, LearningContentClient.KnowledgePoint>> catalog) {
        return judge.judge(WritingTask.TASK_2, ESSAY, base, rubric, errors, entries, catalog);
    }

    @Test
    void aRubricWithoutSubPointsPreservesTheGrade() throws Exception {
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("5.0"), List.of(), List.of(), TOPIC)),
                List.of());
        var result = apply(List.of(criterion(List.of())), base, Map.of(), Map::of);
        assertSame(base, result);
        
    }

    @Test
    void aFailWithAnEssayExcerptBecomesAJudgmentAndAStudyPoint() throws Exception {
        entries = json.readTree(judgments(judgment("LINKING", "FAIL", "however"))).get("judgments");
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("5.0"), List.of(), List.of(), TOPIC)),
                List.of());
        var points = Map.of(LINKING, new LearningContentClient.KnowledgePoint(LINKING, "LINKING", "Linking",
                "PROCEDURE", "WRITING", "Description", true));

        var result = apply(List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING,
                "LINKING", "Linking", "Description")))), base, Map.of(), () -> points);

        var decision = result.criteria().getFirst().knowledgePointJudgments().getFirst();
        assertEquals(WritingGrade.Judgment.FAIL, decision.judgment());
        assertEquals("kpj2", result.judgmentVersion());
        var studyPoint = result.studyPoints().getFirst();
        assertEquals(WritingGrade.StudyPoint.KNOWLEDGE_POINT, studyPoint.kind());
        assertEquals("however", studyPoint.excerpt());
        assertEquals(LINKING, studyPoint.practiceKnowledgePointId());
    }

    @Test
    void aKnowledgePointTheModelDoesNotMentionIsNotAssessed() throws Exception {
        entries = json.readTree(judgments("")).get("judgments");
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), TOPIC)),
                List.of());
        var result = apply(List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING,
                "LINKING", "Linking", null)))), base, Map.of(), Map::of);
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, result.criteria().getFirst().knowledgePointJudgments()
                .getFirst().judgment());
        assertEquals("kpj2", result.judgmentVersion());
    }

    @Test
    void aCorrectionTiedToAKnowledgePointCrossChecksTheJudgment() throws Exception {
        entries = json.readTree(judgments(judgment("LINKING", "NOT_ASSESSED", ""))).get("judgments");
        var correction = new WritingGrade.Correction("however", "Use a different linker", "COHERENCE", "LINKER");
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), TOPIC)),
                List.of(correction));
        var result = apply(List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING,
                "LINKING", "Linking", null)))), base,
                Map.of("LINKER", new LearningContentClient.ErrorType("LINKER", "Linker error", "COHERENCE", 2,
                        "Use a clear linker", LINKING)), Map::of);
        var decision = result.criteria().getFirst().knowledgePointJudgments().getFirst();
        assertEquals(WritingGrade.Judgment.FAIL, decision.judgment());
        assertEquals("however", decision.excerpt());
        assertEquals("Use a clear linker", decision.reason());
    }

    @Test
    void practiceTargetFallsBackToTheCriterionPointWhenTheSubPointHasNoPracticeSet() throws Exception {
        entries = json.readTree(judgments(judgment("LINKING", "FAIL", "however"))).get("judgments");
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("5.5"), List.of(), List.of(), TOPIC)),
                List.of());
        var result = apply(List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING,
                "LINKING", "Linking", null)))), base, Map.of(), () -> Map.of(LINKING,
                new LearningContentClient.KnowledgePoint(LINKING, "LINKING", "Linking", "PROCEDURE", "WRITING",
                        "Description", false)));
        assertEquals(TOPIC, result.studyPoints().getFirst().practiceKnowledgePointId());
    }

    @Test
    void topicSequenceCatalogIsOnlyAskedForWhenAJudgmentFails() throws Exception {
        entries = json.readTree(judgments(judgment("LINKING", "PASS", "however"))).get("judgments");
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), TOPIC)),
                List.of());
        AtomicInteger calls = new AtomicInteger();
        apply(List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING, "LINKING", "Linking", null)))),
                base, Map.of(), () -> { calls.incrementAndGet(); return Map.of(); });
        assertEquals(0, calls.get());
    }

    @Test
    void missingOrMalformedJudgmentsPreserveTheBandsAndLeaveSubPointsUnassessed() throws Exception {
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), TOPIC)), List.of());
        var rubric = List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING, "LINKING", "Linking", null))));
        for (String payload : List.of("null", "{}", "42", "\"invalid\"")) {
            entries = json.readTree(payload);
            var result = apply(rubric, base, Map.of(), Map::of);
            assertEquals(base.overallBand(), result.overallBand());
            assertNull(result.judgmentVersion());
            assertEquals(WritingGrade.Judgment.NOT_ASSESSED, result.criteria().getFirst().knowledgePointJudgments().getFirst().judgment());
            assertTrue(result.studyPoints().isEmpty());
        }
        entries = null;
        assertNull(apply(rubric, base, Map.of(), Map::of).judgmentVersion());
    }

    @Test
    void malformedEntriesCannotDiscardAValidJudgment() throws Exception {
        entries = json.readTree("[null,42,\"bad\",{\"criterion\":false,\"code\":{}}," + judgment("LINKING", "PASS", "however") + "]");
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), TOPIC)), List.of());
        var result = apply(List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING, "LINKING", "Linking", null)))), base, Map.of(), Map::of);
        assertEquals(WritingGrade.Judgment.PASS, result.criteria().getFirst().knowledgePointJudgments().getFirst().judgment());
    }

    @Test
    void aValidatedCorrectionCanAssessAPointWhenJudgmentsAreMissing() {
        var base = grade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of(), TOPIC)),
                List.of(new WritingGrade.Correction("however", "Use another linker", "COHERENCE", "LINKER")));
        var result = apply(List.of(criterion(List.of(new LearningContentClient.SubKnowledgePoint(LINKING, "LINKING", "Linking", null)))), base,
                Map.of("LINKER", new LearningContentClient.ErrorType("LINKER", "Linker error", "COHERENCE", 2, "Use a clear linker", LINKING)), Map::of);
        assertEquals(WritingGrade.Judgment.FAIL, result.criteria().getFirst().knowledgePointJudgments().getFirst().judgment());
        assertNull(result.judgmentVersion());
    }

    @Test
    void thePromptExtendsTheBandResponseWithNamedPointsAndNoIds() {
        String extension = EssayKnowledgeJudge.prompt(WritingTask.TASK_2, List.of(criterion(List.of(
                new LearningContentClient.SubKnowledgePoint(LINKING, "LINKING", "Linking", "Use linking words")))));
        assertTrue(extension.contains("SAME JSON object"));
        assertTrue(extension.contains("criteria, corrections, summary, focus"));
        assertTrue(extension.contains("CC (Coherence and Cohesion)"));
        assertTrue(extension.contains("LINKING — Linking: Use linking words"));
        assertFalse(extension.contains(LINKING.toString()));
        assertFalse(extension.contains(TOPIC.toString()));
        assertEquals("", EssayKnowledgeJudge.prompt(WritingTask.TASK_2, List.of(criterion(List.of()))));
    }
}
