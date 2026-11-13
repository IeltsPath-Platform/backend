package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.WritingGradingException;
import com.ieltspath.learning.application.port.EssayPromptActivationProvider;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The second prompt template: chosen by the activated version, and its richer reply read defensively. */
class EssayGraderSecondTemplateTest {
    private static final String ESSAY = "Some people say cities should plant more trees. Peoples can save energy. "
            + "Parks is good for children.";

    private final EssayGraderTest.ScriptedLlm llm = new EssayGraderTest.ScriptedLlm();
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final EssayPromptActivationProvider activations = mock(EssayPromptActivationProvider.class);
    private final EssayGrader grader = new EssayGrader(llm, new ObjectMapper(), content, activations);

    @BeforeEach
    void rubricAndCatalogAreAvailable() {
        List<LearningContentClient.RubricCriterion> criteria = List.of("TR", "CC", "LR", "GRA").stream()
                .map(code -> new LearningContentClient.RubricCriterion(code, code, "Criterion description", 1,
                        List.of(new LearningContentClient.RubricBandDescriptor(new BigDecimal("6.0"), "Descriptor"))))
                .toList();
        LearningContentClient.Rubric rubric = new LearningContentClient.Rubric("IELTS_ESSAY", "TASK_2", 2, "source",
                criteria, List.of());
        when(content.getActiveRubric(anyString())).thenReturn(rubric);
        when(content.getRubricVersion(anyString(), anyInt())).thenReturn(rubric);
        when(content.activeErrorTypes("WRITING")).thenReturn(List.of(
                new LearningContentClient.ErrorType("GRAMMAR", "Grammar", "GRAMMAR", 1, "Fix grammar"),
                new LearningContentClient.ErrorType("PLURAL", "Plural", "GRAMMAR", 1, "Fix plural")));
    }

    private static EssayPrompt task2() {
        return new EssayPrompt(UUID.randomUUID(), "Should cities plant more trees?", "TASK_2", 50,
                new BigDecimal("6.0"), null, "Sample", List.of(), List.of(UUID.randomUUID()));
    }

    private static String criterion(String code, String extra) {
        return "{\"code\":\"" + code + "\"," + extra + "\"band\":6,\"comment\":\"ok\"}";
    }

    private static String reply(String criteriaExtra, String rest) {
        return "{\"criteria\":[" + String.join(",", criterion("TR", criteriaExtra), criterion("CC", criteriaExtra),
                criterion("LR", criteriaExtra), criterion("GRA", criteriaExtra)) + "]," + rest + "}";
    }

    @Test
    void anActivatedSecondTemplateUsesItsPromptAndKeepsItsVersion() {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv2");
        llm.reply(reply("", "\"corrections\":[]"));

        var graded = grader.grade(task2(), ESSAY);

        assertEquals("IELTS_ESSAY@2+tplv2", graded.promptVersion());
        assertTrue(llm.prompts.getFirst()[0].contains("\"priorities\""), "the second template asks for priorities");
    }

    @Test
    void withNothingActivatedTheFirstTemplateStaysInUse() {
        llm.reply(reply("", "\"corrections\":[]"));

        var graded = grader.grade(task2(), ESSAY);

        assertEquals("IELTS_ESSAY@2+tplv1", graded.promptVersion());
        assertFalse(llm.prompts.getFirst()[0].contains("\"priorities\""));
    }

    @Test
    void withNothingActivatedTheConfiguredDefaultTemplateIsUsed() {
        llm.reply(reply("", "\"corrections\":[]"));

        var graded = graderDefaultingTo("v2").grade(task2(), ESSAY);

        assertEquals("IELTS_ESSAY@2+tplv2", graded.promptVersion());
        assertTrue(llm.prompts.getFirst()[0].contains("\"priorities\""), "the second template asks for priorities");
    }

    @Test
    void anActivationStillWinsOverTheConfiguredDefaultTemplate() {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv1");
        llm.reply(reply("", "\"corrections\":[]"));

        var graded = graderDefaultingTo("v2").grade(task2(), ESSAY);

        assertEquals("IELTS_ESSAY@2+tplv1", graded.promptVersion());
        assertFalse(llm.prompts.getFirst()[0].contains("\"priorities\""));
    }

    private EssayGrader graderDefaultingTo(String template) {
        WritingSettings settings = new WritingSettings(3, 50, 1000, 10000, 20, 120, 10, "Asia/Ho_Chi_Minh", 2, false,
                null, null, null, null, null, 30, template);
        return new EssayGrader(llm, new ObjectMapper(), content, activations, new EssayKnowledgeJudge(), "m", settings);
    }

    @Test
    void anUnknownTemplateVersionIsUnavailable() {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv3");

        assertEquals("PROMPT_VERSION_UNAVAILABLE", assertThrows(WritingGradingException.class,
                () -> grader.grade(task2(), ESSAY)).getCode());
        assertTrue(llm.prompts.isEmpty());
    }

    @Test
    void theFirstTemplateIgnoresTheSecondTemplatesFields() {
        llm.reply(reply("\"evidence\":[\"Peoples can save\"],\"descriptor\":\"x\",",
                "\"corrections\":[{\"excerpt\":\"Peoples can save\",\"suggestion\":\"People can save\","
                        + "\"errorCode\":\"PLURAL\",\"reason\":\"r\",\"certainty\":\"SUGGESTION\"}],"
                        + "\"priorities\":[{\"title\":\"t\",\"excerpts\":[\"Peoples can save\"]}]"));

        WritingGrade grade = grader.grade(task2(), ESSAY).grade();

        assertTrue(grade.priorities().isEmpty());
        assertTrue(grade.errorExamples().isEmpty());
        assertTrue(grade.criteria().getFirst().evidence().isEmpty());
        assertNull(grade.corrections().getFirst().reason());
        assertFalse(grade.corrections().getFirst().suggestionOnly());
    }

    @Test
    void theSecondTemplateKeepsReasonCertaintyEvidenceAndDescriptor() {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv2");
        llm.reply(reply("\"evidence\":[\"Peoples can save\",\"not in the essay\"],\"descriptor\":\"Band 6 text\",",
                "\"corrections\":[{\"excerpt\":\"Peoples can save\",\"suggestion\":\"People can save\","
                        + "\"errorCode\":\"PLURAL\",\"reason\":\"Dung so nhieu\",\"certainty\":\"SUGGESTION\"},"
                        + "{\"excerpt\":\"Parks is\",\"suggestion\":\"Parks are\",\"errorCode\":\"GRAMMAR\"}]"));

        WritingGrade grade = grader.grade(task2(), ESSAY).grade();

        WritingGrade.Criterion first = grade.criteria().getFirst();
        assertEquals(List.of("Peoples can save"), first.evidence(), "a quotation not in the essay is dropped");
        assertEquals("Band 6 text", first.descriptor());
        assertEquals(new BigDecimal("6.0"), first.band(), "the band stays whatever the evidence");
        WritingGrade.Correction suggestion = grade.corrections().get(0);
        assertEquals("Dung so nhieu", suggestion.reason());
        assertTrue(suggestion.suggestionOnly());
        WritingGrade.Correction missing = grade.corrections().get(1);
        assertNull(missing.reason(), "a missing reason does not spoil the correction");
        assertEquals("ERROR", missing.certainty(), "a missing certainty reads as an error");
    }

    @Test
    void anUnknownCertaintyReadsAsAnError() {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv2");
        llm.reply(reply("", "\"corrections\":[{\"excerpt\":\"Peoples can save\",\"suggestion\":\"People can save\","
                + "\"errorCode\":\"PLURAL\",\"certainty\":\"MAYBE\"}]"));

        assertFalse(grader.grade(task2(), ESSAY).grade().corrections().getFirst().suggestionOnly());
    }

    @Test
    void prioritiesDropFalseQuotationsUnknownCodesAndAreCutToThree() {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv2");
        String valid = "{\"title\":\"%s\",\"explanation\":\"e\",\"excerpts\":[\"Peoples can save\"],\"action\":\"a\","
                + "\"errorCodes\":[\"PLURAL\",\"GRAMMAR\",\"UNKNOWN\"]}";
        llm.reply(reply("", "\"corrections\":[{\"excerpt\":\"Peoples can save\",\"suggestion\":\"People can save\","
                + "\"errorCode\":\"PLURAL\"}],\"priorities\":["
                + "{\"title\":\"fake\",\"excerpts\":[\"made up text\"],\"errorCodes\":[\"PLURAL\"]},"
                + valid.formatted("one") + "," + valid.formatted("two") + "," + valid.formatted("three") + ","
                + valid.formatted("four") + "]"));

        List<WritingGrade.Priority> priorities = grader.grade(task2(), ESSAY).grade().priorities();

        assertEquals(List.of("one", "two", "three"), priorities.stream().map(WritingGrade.Priority::title).toList());
        assertEquals(List.of("PLURAL"), priorities.getFirst().errorCodes(),
                "a code that is not among the corrections is dropped");
    }

    @Test
    void errorExamplesKeepTheFirstPerCodeAndOnlyCodesThatAppear() {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv2");
        llm.reply(reply("", "\"corrections\":[{\"excerpt\":\"Peoples can save\",\"suggestion\":\"People can save\","
                + "\"errorCode\":\"PLURAL\"}],\"errorExamples\":["
                + "{\"errorCode\":\"PLURAL\",\"wrong\":\"two cat\",\"right\":\"two cats\"},"
                + "{\"errorCode\":\"PLURAL\",\"wrong\":\"three dog\",\"right\":\"three dogs\"},"
                + "{\"errorCode\":\"GRAMMAR\",\"wrong\":\"a\",\"right\":\"b\"},"
                + "{\"errorCode\":\"PLURAL\",\"wrong\":\"\",\"right\":\"x\"}]"));

        List<WritingGrade.ErrorExample> examples = grader.grade(task2(), ESSAY).grade().errorExamples();

        assertEquals(1, examples.size());
        assertEquals("two cat", examples.getFirst().wrong());
    }

    @Test
    void theSecondTemplateAllowsMoreCorrectionsThanTheFirstsTen() {
        String essay = IntStream.rangeClosed(1, 25).mapToObj(index -> "Mistake" + index + " here.")
                .reduce("", (left, right) -> left + right + " ");
        String corrections = IntStream.rangeClosed(1, 25).mapToObj(index -> "{\"excerpt\":\"Mistake" + index
                + " here\",\"suggestion\":\"fixed\",\"errorCode\":\"GRAMMAR\"}")
                .reduce((left, right) -> left + "," + right).orElseThrow();
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_ESSAY@2+tplv2");
        llm.reply(reply("", "\"corrections\":[" + corrections + "]"));

        assertEquals(25, grader.grade(task2(), essay).grade().corrections().size());

        llm.reply(reply("", "\"corrections\":[" + corrections + "]"));
        when(activations.activatedVersion("TASK_2")).thenReturn(null);
        assertEquals(10, grader.grade(task2(), essay).grade().corrections().size(), "the first template keeps its ceiling");
    }
}
