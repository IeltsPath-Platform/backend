package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.service.EssayKnowledgeJudge.TaskPoint;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class EssayKnowledgeJudgeTest {
    private static final String ESSAY = "I used however to connect the ideas.";
    private static final UUID LINKING = UUID.randomUUID();
    private static final UUID OPINION = UUID.randomUUID();
    private final ObjectMapper json = new ObjectMapper();
    private final EssayKnowledgeJudge judge = new EssayKnowledgeJudge();

    private static KnowledgePoint point(UUID id, String code, String name, String description, boolean practice) {
        return new KnowledgePoint(id, code, name, "PROCEDURE", "WRITING", description, practice);
    }

    private static EssayPrompt prompt(UUID... points) {
        return new EssayPrompt(UUID.randomUUID(), "Question", "TASK_2", 250, new BigDecimal("6.0"), null, "Sample",
                List.of(), List.of(points));
    }

    private static WritingGrade grade(List<WritingGrade.Correction> corrections) {
        return new WritingGrade(List.of(new WritingGrade.Criterion("CC", new BigDecimal("6.0"), List.of(), List.of())),
                corrections, "Summary", new BigDecimal("6.0"));
    }

    private static List<TaskPoint> linking(boolean practice) {
        return List.of(new TaskPoint(LINKING, "LINKING", "Linking", "Use linking words", practice));
    }

    private JsonNode entries(String text) throws Exception {
        return json.readTree(text);
    }

    private static String entry(String code, String value, String excerpt) {
        return "{\"code\":\"" + code + "\",\"judgment\":\"" + value + "\",\"excerpt\":\"" + excerpt
                + "\",\"reason\":\"Giải thích\"}";
    }

    @Test
    void aQuestionWithTwoPointsPutsExactlyTwoLinesInThePrompt() {
        Map<UUID, KnowledgePoint> catalog = Map.of(
                LINKING, point(LINKING, "LINKING", "Linking", "Use linking words", true),
                OPINION, point(OPINION, "TR_W_OPINION", "Opinion", null, false));

        List<TaskPoint> points = EssayKnowledgeJudge.taskPoints(prompt(LINKING, OPINION), () -> catalog);
        String extension = EssayKnowledgeJudge.prompt(points);

        assertEquals(2, points.size());
        assertTrue(extension.contains("SAME JSON object"));
        assertTrue(extension.contains("placed right after \"criteria\""));
        assertTrue(extension.contains("Return every field requested above"));
        assertTrue(extension.contains("- LINKING — Linking: Use linking words\n"));
        assertTrue(extension.contains("- TR_W_OPINION — Opinion\n"), "a blank description leaves out the colon");
        assertFalse(extension.contains("criterion\":"), "an entry names no criterion");
        assertFalse(extension.contains(LINKING.toString()));
        assertEquals("kpj3", EssayKnowledgeJudge.VERSION);
    }

    @Test
    void aQuestionWithoutPointsHasNoJudgmentsBlockAndTheCatalogIsNotRead() {
        AtomicInteger reads = new AtomicInteger();

        List<TaskPoint> points = EssayKnowledgeJudge.taskPoints(prompt(), () -> { reads.incrementAndGet(); return Map.of(); });

        assertTrue(points.isEmpty());
        assertEquals("", EssayKnowledgeJudge.prompt(points));
        assertEquals(0, reads.get());
        var base = grade(List.of());
        assertSame(base, judge.judge(ESSAY, base, points, Map.of(), null));
        assertNull(base.judgmentVersion());
    }

    @Test
    void aQuestionWithFivePointsKeepsTheFirstFour() {
        List<UUID> ids = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).toList();
        Map<UUID, KnowledgePoint> catalog = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) catalog.put(ids.get(i), point(ids.get(i), "KP" + i, "Name " + i, null, false));

        List<TaskPoint> points = EssayKnowledgeJudge.taskPoints(prompt(ids.toArray(UUID[]::new)), () -> catalog);

        assertEquals(List.of("KP0", "KP1", "KP2", "KP3"), points.stream().map(TaskPoint::code).toList());
    }

    @Test
    void aRepeatedPointCountsOnce() {
        Map<UUID, KnowledgePoint> catalog = Map.of(LINKING, point(LINKING, "LINKING", "Linking", null, false));

        assertEquals(1, EssayKnowledgeJudge.taskPoints(prompt(LINKING, LINKING), () -> catalog).size());
    }

    @Test
    void aPointTheCatalogDoesNotKnowIsLeftOutOfThePrompt() {
        Map<UUID, KnowledgePoint> catalog = Map.of(LINKING, point(LINKING, "LINKING", "Linking", null, false));

        List<TaskPoint> points = EssayKnowledgeJudge.taskPoints(prompt(OPINION, LINKING), () -> catalog);

        assertEquals(List.of("LINKING"), points.stream().map(TaskPoint::code).toList());
    }

    @Test
    void aFailWithAnEssayExcerptBecomesAJudgmentAndAStudyPoint() throws Exception {
        var result = judge.judge(ESSAY, grade(List.of()), linking(true), Map.of(),
                entries("[" + entry("LINKING", "FAIL", "however") + "]"));

        var decision = result.taskKnowledgePointJudgments().getFirst();
        assertEquals(WritingGrade.Judgment.FAIL, decision.judgment());
        assertEquals("however", decision.excerpt());
        assertEquals("kpj3", result.judgmentVersion());
        assertTrue(result.criteria().getFirst().knowledgePointJudgments().isEmpty());
        var studyPoint = result.studyPoints().getFirst();
        assertEquals(WritingGrade.StudyPoint.KNOWLEDGE_POINT, studyPoint.kind());
        assertNull(studyPoint.criterionCode());
        assertNull(studyPoint.band());
        assertEquals("however", studyPoint.excerpt());
        assertEquals(LINKING, studyPoint.practiceKnowledgePointId());
    }

    @Test
    void aPointWithoutAPracticeSetSendsTheLearnerNowhere() throws Exception {
        var result = judge.judge(ESSAY, grade(List.of()), linking(false), Map.of(),
                entries("[" + entry("LINKING", "FAIL", "however") + "]"));

        assertNull(result.studyPoints().getFirst().practiceKnowledgePointId());
    }

    @Test
    void failedPointsComeAfterTheCriterionPointsAndBeforeTheModelsFocus() throws Exception {
        var criterionPoint = new WritingGrade.StudyPoint(WritingGrade.StudyPoint.CRITERION, "CC", new BigDecimal("5.0"),
                UUID.randomUUID(), "CRIT", "Criterion", "Criterion", null);
        var focus = new WritingGrade.StudyPoint(WritingGrade.StudyPoint.AI, null, null, null, null, null, "Focus", null);
        var base = new WritingGrade(grade(List.of()).criteria(), List.of(), "Summary", new BigDecimal("6.0"),
                List.of(criterionPoint, focus));

        var result = judge.judge(ESSAY, base, linking(true), Map.of(), entries("[" + entry("LINKING", "FAIL", "however") + "]"));

        assertEquals(List.of("CRITERION", "KNOWLEDGE_POINT", "AI"),
                result.studyPoints().stream().map(WritingGrade.StudyPoint::kind).toList());
    }

    @Test
    void aPassWithAnExcerptThatIsNotInTheEssayIsNotAssessed() throws Exception {
        var result = judge.judge(ESSAY, grade(List.of()), linking(true), Map.of(),
                entries("[" + entry("LINKING", "PASS", "words the learner never wrote") + "]"));

        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, result.taskKnowledgePointJudgments().getFirst().judgment());
    }

    @Test
    void aPassWithARealExcerptIsKept() throws Exception {
        var result = judge.judge(ESSAY, grade(List.of()), linking(true), Map.of(),
                entries("[" + entry("LINKING", "PASS", "however") + "]"));

        assertEquals(WritingGrade.Judgment.PASS, result.taskKnowledgePointJudgments().getFirst().judgment());
        assertTrue(result.studyPoints().isEmpty());
    }

    @Test
    void aCorrectionTiedToThePointTurnsAPassIntoNotAssessed() throws Exception {
        var correction = new WritingGrade.Correction("however", "Use a different linker", "COHERENCE", "LINKER");
        var errors = Map.of("LINKER", new LearningContentClient.ErrorType("LINKER", "Linker error", "COHERENCE", 2,
                "Use a clear linker", LINKING));

        var result = judge.judge(ESSAY, grade(List.of(correction)), linking(true), errors,
                entries("[" + entry("LINKING", "PASS", "however") + "]"));

        var decision = result.taskKnowledgePointJudgments().getFirst();
        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, decision.judgment());
        assertNull(decision.excerpt());
    }

    @Test
    void aCorrectionTiedToThePointRaisesNotAssessedToFail() throws Exception {
        var correction = new WritingGrade.Correction("however", "Use a different linker", "COHERENCE", "LINKER");
        var errors = Map.of("LINKER", new LearningContentClient.ErrorType("LINKER", "Linker error", "COHERENCE", 2,
                "Use a clear linker", LINKING));

        var result = judge.judge(ESSAY, grade(List.of(correction)), linking(true), errors,
                entries("[" + entry("LINKING", "NOT_ASSESSED", "") + "]"));

        var decision = result.taskKnowledgePointJudgments().getFirst();
        assertEquals(WritingGrade.Judgment.FAIL, decision.judgment());
        assertEquals("however", decision.excerpt());
        assertEquals("Use a clear linker", decision.reason());
    }

    @Test
    void aSuggestionNeverTurnsAPointIntoAFail() throws Exception {
        var suggestion = new WritingGrade.Correction("however", "Another linker", "COHERENCE", "LINKER", "Linker",
                "Hint", 1, null, null, "Better wording", WritingGrade.Correction.SUGGESTION);
        var errors = Map.of("LINKER", new LearningContentClient.ErrorType("LINKER", "Linker error", "COHERENCE", 2,
                "Use a clear linker", LINKING));

        var result = judge.judge(ESSAY, grade(List.of(suggestion)), linking(true), errors,
                entries("[" + entry("LINKING", "NOT_ASSESSED", "") + "]"));

        assertEquals(WritingGrade.Judgment.NOT_ASSESSED, result.taskKnowledgePointJudgments().getFirst().judgment());
    }

    @Test
    void anEntryThatNamesACriterionStillMatchesByCode() throws Exception {
        var result = judge.judge(ESSAY, grade(List.of()), linking(true), Map.of(),
                entries("[{\"criterion\":\"CC\",\"code\":\"LINKING\",\"judgment\":\"PASS\",\"excerpt\":\"however\"}]"));

        assertEquals(WritingGrade.Judgment.PASS, result.taskKnowledgePointJudgments().getFirst().judgment());
    }

    @Test
    void missingOrMalformedJudgmentsPreserveTheBandsAndLeavePointsUnassessed() throws Exception {
        var base = grade(List.of());
        for (String payload : List.of("null", "{}", "42", "\"invalid\"")) {
            var result = judge.judge(ESSAY, base, linking(true), Map.of(), entries(payload));

            assertEquals(base.overallBand(), result.overallBand());
            assertEquals("kpj3", result.judgmentVersion(), "the prompt asked, so the version is recorded");
            assertEquals(WritingGrade.Judgment.NOT_ASSESSED, result.taskKnowledgePointJudgments().getFirst().judgment());
            assertTrue(result.studyPoints().isEmpty());
        }
        assertEquals("kpj3", judge.judge(ESSAY, base, linking(true), Map.of(), null).judgmentVersion());
    }

    @Test
    void malformedEntriesCannotDiscardAValidJudgment() throws Exception {
        var result = judge.judge(ESSAY, grade(List.of()), linking(true), Map.of(),
                entries("[null,42,\"bad\",{\"code\":{}}," + entry("LINKING", "PASS", "however") + "]"));

        assertEquals(WritingGrade.Judgment.PASS, result.taskKnowledgePointJudgments().getFirst().judgment());
    }
}
