package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.EssayPromptActivationProvider;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The whole-essay checks of the second prompt template: read, kept with the grade, and wired to the band caps. */
class EssayGraderTaskChecksTest {
    private static final String ESSAY_1 = "The chart shows sales. Sales rose to 40 in 2020. Sales fell to 10 in 2021. "
            + "I think this is sad.";
    private static final String ESSAY_2 = "Some say schools matter. In my view, schools matter a lot. "
            + "First, they teach skills.";
    private static final String CHART = "2020: 40. 2021: 15.";

    private final EssayGraderTest.ScriptedLlm llm = new EssayGraderTest.ScriptedLlm();
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final EssayPromptActivationProvider activations = mock(EssayPromptActivationProvider.class);
    private final EssayGrader grader = new EssayGrader(llm, new ObjectMapper(), content, activations);

    @BeforeEach
    void rubricAndCatalogAreAvailable() {
        when(content.activeErrorTypes("WRITING")).thenReturn(List.of(
                new LearningContentClient.ErrorType("GRAMMAR", "Grammar", "GRAMMAR", 1, "Fix grammar"),
                new LearningContentClient.ErrorType("T1_DATA_MISREPORTED", "Số liệu sai", "TASK", 3, "Đối chiếu bảng")));
        when(activations.activatedVersion("TASK_1")).thenReturn("IELTS_ESSAY@1+tplv2");
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@1+tplv2");
    }

    private static LearningContentClient.Rubric rubric(String task) {
        List<String> codes = "TASK_1".equals(task) ? List.of("TA", "CC", "LR", "GRA") : List.of("TR", "CC", "LR", "GRA");
        return new LearningContentClient.Rubric("IELTS_ESSAY", task, 1, "source", codes.stream()
                .map(code -> new LearningContentClient.RubricCriterion(code, code, "Description", 1,
                        List.of(new LearningContentClient.RubricBandDescriptor(new BigDecimal("6.0"), "Descriptor"))))
                .toList(), List.of());
    }

    /** The rubric the grader asks for depends on the task of the prompt, so the prompt sets it up. */
    private void rubricIs(String task) {
        LearningContentClient.Rubric rubric = rubric(task);
        when(content.getActiveRubric(anyString())).thenReturn(rubric);
        when(content.getRubricVersion(anyString(), anyInt())).thenReturn(rubric);
    }

    private EssayPrompt task1() {
        rubricIs("TASK_1");
        return new EssayPrompt(UUID.randomUUID(), "Summarise the chart.", "TASK_1", 150, new BigDecimal("6.0"), CHART,
                "Sample", List.of(), List.of(UUID.randomUUID()));
    }

    private EssayPrompt task2() {
        rubricIs("TASK_2");
        return new EssayPrompt(UUID.randomUUID(), "Do schools matter?", "TASK_2", 250, new BigDecimal("6.0"), null,
                "Sample", List.of(), List.of(UUID.randomUUID()));
    }

    private static String reply(String first, String rest) {
        String evidence = "\"evidence\":[],\"descriptor\":\"d\",";
        String criteria = String.join(",", List.of(first, "CC", "LR", "GRA").stream()
                .map(code -> "{\"code\":\"" + code + "\"," + evidence + "\"band\":7,\"comment\":\"ok\"}").toList());
        return "{\"criteria\":[" + criteria + "]," + rest + "}";
    }

    private static final String GRAMMAR_CORRECTION = "{\"excerpt\":\"Sales fell to 10 in 2021.\","
            + "\"suggestion\":\"Sales fell in 2021.\",\"errorCode\":\"GRAMMAR\",\"reason\":\"r\",\"certainty\":\"ERROR\"}";

    @Test
    void aTaskOneGradeCarriesTheDataChecksAndNoTaskTwoChecks() {
        llm.reply(reply("TA", "\"corrections\":[],\"taskChecks\":{\"dataChecks\":["
                + "{\"excerpt\":\"Sales rose to 40 in 2020.\",\"verdict\":\"CORRECT\"}],\"hasOverview\":false}"));

        WritingGrade grade = grader.grade(task1(), ESSAY_1).grade();

        assertEquals(1, grade.task1Checks().correctCount());
        assertEquals(Boolean.FALSE, grade.task1Checks().hasOverview());
        assertNull(grade.task2Checks());
    }

    @Test
    void aTaskTwoGradeCarriesTheRequirementChecksAndNoTaskOneChecks() {
        llm.reply(reply("TR", "\"corrections\":[],\"taskChecks\":{\"questionParts\":["
                + "{\"part\":\"Nêu ý kiến\",\"addressed\":\"FULL\",\"excerpt\":\"In my view, schools matter a lot.\"}]}"));

        WritingGrade grade = grader.grade(task2(), ESSAY_2).grade();

        assertEquals(1, grade.task2Checks().questionParts().size());
        assertNull(grade.task1Checks());
    }

    @Test
    void theFirstTemplateIgnoresTheChecks() {
        when(activations.activatedVersion("TASK_1")).thenReturn(null);
        llm.reply(reply("TA", "\"corrections\":[],\"taskChecks\":{\"hasOverview\":false}"));

        WritingGrade grade = grader.grade(task1(), ESSAY_1).grade();

        assertNull(grade.task1Checks());
        assertNull(grade.task2Checks());
    }

    @Test
    void aWrongFigureWithoutACorrectionGetsOneCarryingTheCorrectStatement() {
        llm.reply(reply("TA", "\"corrections\":[],\"taskChecks\":{\"dataChecks\":["
                + "{\"excerpt\":\"Sales fell to 10 in 2021.\",\"verdict\":\"WRONG\",\"expected\":\"Năm 2021 là 15.\"}]}"));

        WritingGrade grade = grader.grade(task1(), ESSAY_1).grade();

        assertEquals(1, grade.corrections().size());
        var added = grade.corrections().getFirst();
        assertEquals("T1_DATA_MISREPORTED", added.errorCode());
        assertEquals("Sales fell to 10 in 2021.", added.excerpt());
        assertEquals("Năm 2021 là 15.", added.suggestion());
        assertEquals(3, added.severity());
        assertFalse(added.isSuggestion());
    }

    @Test
    void aGrammarCorrectionOnTheSameWordsDoesNotCoverAWrongFigure() {
        llm.reply(reply("TA", "\"corrections\":[" + GRAMMAR_CORRECTION + "],\"taskChecks\":{\"dataChecks\":["
                + "{\"excerpt\":\"Sales fell to 10 in 2021.\",\"verdict\":\"WRONG\",\"expected\":\"Năm 2021 là 15.\"}]}"));

        WritingGrade grade = grader.grade(task1(), ESSAY_1).grade();

        assertEquals(List.of("GRAMMAR", "T1_DATA_MISREPORTED"),
                grade.corrections().stream().map(WritingGrade.Correction::errorCode).toList());
    }

    @Test
    void aWrongFigureThatAlreadyHasADataCorrectionIsNotCorrectedTwice() {
        String data = "{\"excerpt\":\"Sales fell to 10 in 2021.\",\"suggestion\":\"Sales fell to 15 in 2021.\","
                + "\"errorCode\":\"T1_DATA_MISREPORTED\",\"reason\":\"r\",\"certainty\":\"ERROR\"}";
        llm.reply(reply("TA", "\"corrections\":[" + data + "],\"taskChecks\":{\"dataChecks\":["
                + "{\"excerpt\":\"Sales fell to 10 in 2021.\",\"verdict\":\"WRONG\",\"expected\":\"Năm 2021 là 15.\"}]}"));

        WritingGrade grade = grader.grade(task1(), ESSAY_1).grade();

        assertEquals(1, grade.corrections().size());
        assertEquals("Sales fell to 15 in 2021.", grade.corrections().getFirst().suggestion());
    }

    @Test
    void aWrongFigureOverlappingADataCorrectionOnlyPartlyIsStillCovered() {
        String inner = "{\"excerpt\":\"fell to 10\",\"suggestion\":\"fell to 15\","
                + "\"errorCode\":\"T1_DATA_MISREPORTED\",\"reason\":\"r\",\"certainty\":\"ERROR\"}";
        llm.reply(reply("TA", "\"corrections\":[" + inner + "],\"taskChecks\":{\"dataChecks\":["
                + "{\"excerpt\":\"Sales fell to 10 in 2021.\",\"verdict\":\"WRONG\",\"expected\":\"Năm 2021 là 15.\"}]}"));

        assertEquals(1, grader.grade(task1(), ESSAY_1).grade().corrections().size());
    }

    @Test
    void noCorrectionIsAddedWhenTheCatalogHasNoDataCode() {
        when(content.activeErrorTypes("WRITING")).thenReturn(List.of(
                new LearningContentClient.ErrorType("GRAMMAR", "Grammar", "GRAMMAR", 1, "Fix grammar")));
        llm.reply(reply("TA", "\"corrections\":[],\"taskChecks\":{\"dataChecks\":["
                + "{\"excerpt\":\"Sales fell to 10 in 2021.\",\"verdict\":\"WRONG\",\"expected\":\"Năm 2021 là 15.\"}]}"));

        assertTrue(grader.grade(task1(), ESSAY_1).grade().corrections().isEmpty());
    }

    @Test
    void aWrongFigureWithoutAnExpectedStatementAddsNoCorrection() {
        llm.reply(reply("TA", "\"corrections\":[],\"taskChecks\":{\"dataChecks\":["
                + "{\"excerpt\":\"Sales fell to 10 in 2021.\",\"verdict\":\"WRONG\"}]}"));

        WritingGrade grade = grader.grade(task1(), ESSAY_1).grade();

        assertTrue(grade.corrections().isEmpty());
        assertEquals(1, grade.task1Checks().dataChecks().size());
    }

    private EssayGrader capped(BigDecimal noOverview, BigDecimal partial, BigDecimal noPosition) {
        WritingSettings settings = new WritingSettings(3, 50, 1000, 10000, 20, 120, 10, "Asia/Ho_Chi_Minh", 2, true,
                null, null, noOverview, partial, noPosition, 30);
        return new EssayGrader(llm, new ObjectMapper(), content, activations, new EssayKnowledgeJudge(), "m", settings);
    }

    private static BigDecimal band(WritingGrade grade, String code) {
        return grade.criteria().stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow().band();
    }

    @Test
    void aMissingOverviewCapsTaskAchievementWhenTheCapIsOn() {
        llm.reply(reply("TA", "\"corrections\":[],\"taskChecks\":{\"hasOverview\":false}"));

        WritingGrade grade = capped(new BigDecimal("5.0"), null, null).grade(task1(), ESSAY_1).grade();

        assertEquals(0, new BigDecimal("5.0").compareTo(band(grade, "TA")));
        assertEquals("T1_NO_OVERVIEW", grade.bandLimits().getFirst().reason());
        assertNotNull(grade.task1Checks(), "the checks survive the cap and the knowledge judgment");
    }

    @Test
    void anOverviewThatIsThereLeavesTheBandAlone() {
        llm.reply(reply("TA", "\"corrections\":[],\"taskChecks\":{\"hasOverview\":true}"));

        WritingGrade grade = capped(new BigDecimal("5.0"), null, null).grade(task1(), ESSAY_1).grade();

        assertEquals(0, new BigDecimal("7").compareTo(band(grade, "TA")));
        assertTrue(grade.bandLimits().isEmpty());
    }

    @Test
    void aPartialAnswerAndNoPositionCapTaskResponse() {
        llm.reply(reply("TR", "\"corrections\":[],\"taskChecks\":{\"questionParts\":["
                + "{\"part\":\"a\",\"addressed\":\"NONE\"}],\"position\":{\"required\":true,\"stated\":false}}"));

        WritingGrade grade = capped(null, new BigDecimal("5.0"), new BigDecimal("4.5")).grade(task2(), ESSAY_2).grade();

        assertEquals(0, new BigDecimal("4.5").compareTo(band(grade, "TR")));
        assertEquals(List.of("PARTIAL_ANSWER", "NO_POSITION"),
                grade.bandLimits().stream().map(WritingGrade.AppliedBandLimit::reason).toList());
        assertNotNull(grade.task2Checks());
    }

    @Test
    void theTaskOnePromptAsksForTheDataChecksOnly() {
        llm.reply(reply("TA", "\"corrections\":[]"));

        grader.grade(task1(), ESSAY_1);

        String system = llm.prompts.getFirst()[0];
        assertTrue(system.contains("\"dataChecks\""));
        assertFalse(system.contains("questionParts"));
        assertFalse(system.contains("[["), "section markers are not part of the prompt");
    }

    @Test
    void theTaskTwoPromptAsksForTheRequirementChecksOnly() {
        llm.reply(reply("TR", "\"corrections\":[]"));

        grader.grade(task2(), ESSAY_2);

        String system = llm.prompts.getFirst()[0];
        assertTrue(system.contains("\"questionParts\""));
        assertFalse(system.contains("dataChecks"));
        assertFalse(system.contains("[["));
    }

    @Test
    void theNoticeForPeopleAtTheTopOfTheTemplateIsNotSentToTheModel() {
        llm.reply(reply("TR", "\"corrections\":[]"));

        grader.grade(task2(), ESSAY_2);

        String system = llm.prompts.getFirst()[0];
        assertFalse(system.contains("NOT ACTIVATED"));
        assertTrue(system.startsWith("You are an experienced IELTS examiner"));
    }

    @Test
    void theChecksNeverLowerABandWhileTheCapsAreOff() {
        llm.reply(reply("TR", "\"corrections\":[],\"taskChecks\":{\"questionParts\":["
                + "{\"part\":\"a\",\"addressed\":\"NONE\"}],\"position\":{\"required\":true,\"stated\":false}}"));

        WritingGrade grade = grader.grade(task2(), ESSAY_2).grade();

        assertEquals(0, new BigDecimal("7").compareTo(band(grade, "TR")));
        assertTrue(grade.bandLimits().isEmpty());
        assertTrue(grade.task2Checks().partialAnswer());
    }
}
