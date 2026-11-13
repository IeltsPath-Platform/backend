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

/** Each criterion remembers the next band the rubric describes, so the result can say what it takes to reach it. */
class EssayGraderNextBandTest {
    private static final String ESSAY = "Some people say cities should plant more trees. Peoples can save energy.";

    private final EssayGraderTest.ScriptedLlm llm = new EssayGraderTest.ScriptedLlm();
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final EssayPromptActivationProvider activations = mock(EssayPromptActivationProvider.class);

    private static LearningContentClient.RubricBandDescriptor descriptor(String band) {
        return new LearningContentClient.RubricBandDescriptor(new BigDecimal(band), "Band " + band + " descriptor");
    }

    @BeforeEach
    void rubricIsAvailable() {
        List<LearningContentClient.RubricCriterion> criteria = List.of(
                new LearningContentClient.RubricCriterion("TR", "TR", "d", 1,
                        List.of(descriptor("8.0"), descriptor("5.0"), descriptor("7.0"), descriptor("6.0"))),
                new LearningContentClient.RubricCriterion("CC", "CC", "d", 2, List.of(descriptor("6.0"))),
                new LearningContentClient.RubricCriterion("LR", "LR", "d", 3, null),
                new LearningContentClient.RubricCriterion("GRA", "GRA", "d", 4, List.of(descriptor("9.0"))));
        var rubric = new LearningContentClient.Rubric("IELTS_ESSAY", "TASK_2", 1, "source", criteria, List.of());
        when(content.getActiveRubric(anyString())).thenReturn(rubric);
        when(content.getRubricVersion(anyString(), anyInt())).thenReturn(rubric);
        when(content.activeErrorTypes("WRITING")).thenReturn(List.of());
    }

    private static EssayPrompt task2() {
        return new EssayPrompt(UUID.randomUUID(), "Should cities plant more trees?", "TASK_2", 50, new BigDecimal("6.0"),
                null, "Sample", List.of(), List.of(UUID.randomUUID()));
    }

    private void replyWithBands(String tr, String cc, String lr, String gra) {
        llm.reply("{\"criteria\":[{\"code\":\"TR\",\"band\":" + tr + ",\"comment\":\"c\"},{\"code\":\"CC\",\"band\":" + cc
                + ",\"comment\":\"c\"},{\"code\":\"LR\",\"band\":" + lr + ",\"comment\":\"c\"},{\"code\":\"GRA\",\"band\":"
                + gra + ",\"comment\":\"c\"}],\"corrections\":[]}");
    }

    private static WritingGrade.Criterion criterion(WritingGrade grade, String code) {
        return grade.criteria().stream().filter(item -> item.code().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void theNextBandIsTheLowestDescribedBandAboveTheCurrentOne() {
        replyWithBands("6", "5.5", "6", "6");

        WritingGrade grade = new EssayGrader(llm, new ObjectMapper(), content, activations).grade(task2(), ESSAY).grade();

        assertEquals(0, new BigDecimal("7.0").compareTo(criterion(grade, "TR").nextBand().band()));
        assertEquals("Band 7.0 descriptor", criterion(grade, "TR").nextBand().descriptor());
    }

    @Test
    void aBandBetweenTwoDescribedBandsPointsAtTheNextDescribedOne() {
        replyWithBands("6.5", "6", "6", "6");

        WritingGrade grade = new EssayGrader(llm, new ObjectMapper(), content, activations).grade(task2(), ESSAY).grade();

        assertEquals(0, new BigDecimal("7.0").compareTo(criterion(grade, "TR").nextBand().band()));
    }

    @Test
    void thereIsNoNextBandAtTheTopOfTheRubricOrWhenNothingIsDescribed() {
        replyWithBands("8", "6", "6", "9");

        WritingGrade grade = new EssayGrader(llm, new ObjectMapper(), content, activations).grade(task2(), ESSAY).grade();

        assertNull(criterion(grade, "TR").nextBand(), "8.0 is the highest described band");
        assertNull(criterion(grade, "CC").nextBand(), "6.0 is the only described band");
        assertNull(criterion(grade, "LR").nextBand(), "no descriptors at all");
        assertNull(criterion(grade, "GRA").nextBand());
    }

    @Test
    void aCappedBandPointsAtTheNextBandAboveTheCap() {
        replyWithBands("7", "6", "6", "6");
        EssayPrompt tooShort = new EssayPrompt(UUID.randomUUID(), "Q", "TASK_2", 250, new BigDecimal("6.0"), null,
                "Sample", List.of(), List.of(UUID.randomUUID()));
        WritingSettings withCap = new WritingSettings(3, 50, 1000, 10000, 20, 120, 10, "Asia/Ho_Chi_Minh", 2, true,
                new BigDecimal("5.0"), null, null, null, null, 30, "v1");

        WritingGrade grade = new EssayGrader(llm, new ObjectMapper(), content, activations, new EssayKnowledgeJudge(), "m",
                withCap).grade(tooShort, ESSAY).grade();

        assertEquals(0, new BigDecimal("5.0").compareTo(criterion(grade, "TR").band()));
        assertEquals(0, new BigDecimal("6.0").compareTo(criterion(grade, "TR").nextBand().band()));
    }
}
