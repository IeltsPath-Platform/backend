package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.vo.WritingGrade.DataCheck;
import com.ieltspath.learning.domain.vo.WritingGrade.Task1Checks;
import com.ieltspath.learning.domain.vo.WritingGrade.Task2Checks;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TaskChecksParserTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ESSAY = "The chart shows sales. Sales rose to 40 in 2020. Sales fell to 10 in 2021. "
            + "I think this is sad. Overall, sales rose then fell.";

    private static JsonNode node(String text) throws Exception {
        return JSON.readTree(text);
    }

    @Test
    void keepsValidDataChecksAndCountsCorrectOnes() throws Exception {
        Task1Checks checks = TaskChecksParser.task1(node("""
                {"dataChecks":[
                 {"excerpt":"Sales rose to 40 in 2020.","verdict":"CORRECT"},
                 {"excerpt":"Sales fell to 10 in 2021.","verdict":"WRONG","expected":"Năm 2021 doanh số là 15."},
                 {"excerpt":"The chart shows sales.","verdict":"UNVERIFIABLE"}]}"""), ESSAY);

        assertEquals(3, checks.dataChecks().size());
        assertEquals(1, checks.correctCount());
        assertEquals(new DataCheck("Sales fell to 10 in 2021.", "WRONG", "Năm 2021 doanh số là 15."),
                checks.dataChecks().get(1));
    }

    @Test
    void dropsEntriesWhoseExcerptIsNotInTheEssayOrWhoseVerdictIsUnknown() throws Exception {
        Task1Checks checks = TaskChecksParser.task1(node("""
                {"dataChecks":[
                 {"excerpt":"Not in the essay.","verdict":"WRONG","expected":"x"},
                 {"excerpt":"Sales rose to 40 in 2020.","verdict":"MAYBE"},
                 {"excerpt":"Sales rose to 40 in 2020.","verdict":"CORRECT"}]}"""), ESSAY);

        assertEquals(1, checks.dataChecks().size());
        assertEquals("CORRECT", checks.dataChecks().getFirst().verdict());
    }

    @Test
    void keepsExpectedOnlyForWrongEntriesAndCutsItAt200Characters() throws Exception {
        String longText = "a".repeat(250);
        Task1Checks checks = TaskChecksParser.task1(node("""
                {"dataChecks":[
                 {"excerpt":"Sales rose to 40 in 2020.","verdict":"CORRECT","expected":"ignored"},
                 {"excerpt":"Sales fell to 10 in 2021.","verdict":"WRONG","expected":"%s"}]}""".formatted(longText)), ESSAY);

        assertNull(checks.dataChecks().get(0).expected());
        assertEquals(200, checks.dataChecks().get(1).expected().length());
    }

    @Test
    void keepsAtMostTwentyDataChecks() throws Exception {
        StringBuilder items = new StringBuilder();
        IntStream.range(0, 25).forEach(i -> items.append(i == 0 ? "" : ",")
                .append("{\"excerpt\":\"Sales rose to 40 in 2020.\",\"verdict\":\"CORRECT\"}"));

        Task1Checks checks = TaskChecksParser.task1(node("{\"dataChecks\":[" + items + "]}"), ESSAY);

        assertEquals(20, checks.dataChecks().size());
    }

    @Test
    void missingOrMalformedChecksReadAsEmpty() throws Exception {
        for (String text : new String[]{"{}", "\"text\"", "[1]", "{\"dataChecks\":\"no\"}", "null"}) {
            Task1Checks checks = TaskChecksParser.task1(node(text), ESSAY);
            assertTrue(checks.dataChecks().isEmpty(), text);
            assertNull(checks.hasOverview(), text);
            assertNull(checks.hasPersonalOpinion(), text);
        }
        assertTrue(TaskChecksParser.task1(null, ESSAY).dataChecks().isEmpty());
    }

    @Test
    void readsTheOverviewAndOpinionFlags() throws Exception {
        Task1Checks checks = TaskChecksParser.task1(node("""
                {"hasOverview":true,"overviewExcerpt":"Overall, sales rose then fell.","overviewAccurate":true,
                 "hasPersonalOpinion":true,"personalOpinionExcerpt":"I think this is sad."}"""), ESSAY);

        assertEquals(Boolean.TRUE, checks.hasOverview());
        assertEquals(Boolean.TRUE, checks.overviewAccurate());
        assertEquals(Boolean.TRUE, checks.hasPersonalOpinion());
        assertEquals("I think this is sad.", checks.personalOpinionExcerpt());
    }

    @Test
    void anOpinionFlagWithoutAQuotationFromTheEssayIsDiscarded() throws Exception {
        Task1Checks checks = TaskChecksParser.task1(node("""
                {"hasPersonalOpinion":true,"personalOpinionExcerpt":"Never written."}"""), ESSAY);

        assertNull(checks.hasPersonalOpinion());
        assertNull(checks.personalOpinionExcerpt());
    }

    @Test
    void noOverviewIsKeptBecauseAMissingParagraphCannotBeQuoted() throws Exception {
        Task1Checks checks = TaskChecksParser.task1(node("{\"hasOverview\":false}"), ESSAY);

        assertEquals(Boolean.FALSE, checks.hasOverview());
    }

    @Test
    void aNonBooleanFlagReadsAsUnknown() throws Exception {
        Task1Checks checks = TaskChecksParser.task1(node("{\"hasOverview\":\"yes\",\"overviewAccurate\":1}"), ESSAY);

        assertNull(checks.hasOverview());
        assertNull(checks.overviewAccurate());
    }

    private static final String ESSAY_2 = "Some say schools matter. In my view, schools matter a lot. "
            + "First, they teach skills. For example, my school taught me coding. In conclusion, schools matter.";

    @Test
    void keepsQuestionPartsAndDropsAnsweredOnesWithoutAQuotation() throws Exception {
        Task2Checks checks = TaskChecksParser.task2(node("""
                {"questionParts":[
                 {"part":"Nêu ý kiến","addressed":"FULL","excerpt":"In my view, schools matter a lot."},
                 {"part":"Bàn mặt trái","addressed":"PARTIAL","excerpt":"not in the essay"},
                 {"part":"Đưa giải pháp","addressed":"NONE"},
                 {"part":"Sai nhãn","addressed":"HALF","excerpt":"Some say schools matter."}]}"""), ESSAY_2);

        assertEquals(2, checks.questionParts().size());
        assertEquals("FULL", checks.questionParts().get(0).addressed());
        assertEquals("NONE", checks.questionParts().get(1).addressed());
        assertNull(checks.questionParts().get(1).excerpt());
    }

    @Test
    void keepsAtMostFourQuestionPartsAndEightOutlineItems() throws Exception {
        StringBuilder parts = new StringBuilder();
        StringBuilder outline = new StringBuilder();
        for (int i = 0; i < 6; i++) parts.append(i == 0 ? "" : ",").append("{\"part\":\"p\",\"addressed\":\"NONE\"}");
        for (int i = 0; i < 10; i++) outline.append(i == 0 ? "" : ",")
                .append("{\"role\":\"BODY\",\"mainIdea\":\"ý\",\"developed\":true}");

        Task2Checks checks = TaskChecksParser.task2(node("{\"questionParts\":[" + parts + "],\"outline\":["
                + outline + "]}"), ESSAY_2);

        assertEquals(4, checks.questionParts().size());
        assertEquals(8, checks.outline().size());
    }

    @Test
    void readsThePositionAndDropsQuotationsThatAreNotInTheEssay() throws Exception {
        Task2Checks checks = TaskChecksParser.task2(node("""
                {"position":{"required":true,"stated":true,"excerpt":"In my view, schools matter a lot.",
                 "consistent":false,"conflictExcerpt":"Never written."}}"""), ESSAY_2);

        assertTrue(checks.position().required());
        assertTrue(checks.position().stated());
        assertEquals("In my view, schools matter a lot.", checks.position().excerpt());
        assertEquals(Boolean.FALSE, checks.position().consistent());
        assertNull(checks.position().conflictExcerpt());
    }

    @Test
    void aConflictQuotationIsKeptOnlyWhenThePositionIsInconsistent() throws Exception {
        Task2Checks consistent = TaskChecksParser.task2(node("""
                {"position":{"required":true,"stated":true,"consistent":true,
                 "conflictExcerpt":"In my view, schools matter a lot."}}"""), ESSAY_2);
        Task2Checks conflicting = TaskChecksParser.task2(node("""
                {"position":{"required":true,"stated":true,"consistent":false,
                 "conflictExcerpt":"First, they teach skills."}}"""), ESSAY_2);

        assertNull(consistent.position().conflictExcerpt());
        assertEquals("First, they teach skills.", conflicting.position().conflictExcerpt());
    }

    @Test
    void aPositionWithNonBooleanRequiredOrStatedIsDiscarded() throws Exception {
        Task2Checks checks = TaskChecksParser.task2(node("{\"position\":{\"required\":\"yes\",\"stated\":true}}"), ESSAY_2);

        assertNull(checks.position());
    }

    @Test
    void outlineKeepsItemsWithoutAQuotationAndDropsUnknownRoles() throws Exception {
        Task2Checks checks = TaskChecksParser.task2(node("""
                {"outline":[
                 {"role":"INTRODUCTION","mainIdea":"Giới thiệu","developed":false,"excerpt":"Some say schools matter."},
                 {"role":"BODY","mainIdea":"Kỹ năng","developed":true,"excerpt":"not in the essay"},
                 {"role":"SIDEBAR","mainIdea":"x","developed":true},
                 {"role":"BODY","mainIdea":"","developed":true}]}"""), ESSAY_2);

        assertEquals(2, checks.outline().size());
        assertEquals("Some say schools matter.", checks.outline().get(0).excerpt());
        assertNull(checks.outline().get(1).excerpt());
    }

    @Test
    void derivesTheFlagsTheCapsAndTheViewUse() throws Exception {
        Task2Checks checks = TaskChecksParser.task2(node("""
                {"questionParts":[{"part":"a","addressed":"FULL","excerpt":"In my view, schools matter a lot."},
                                  {"part":"b","addressed":"NONE"}],
                 "position":{"required":true,"stated":false,"consistent":false},
                 "outline":[{"role":"INTRODUCTION","mainIdea":"i","developed":false},
                            {"role":"BODY","mainIdea":"b","developed":true}]}"""), ESSAY_2);

        assertTrue(checks.partialAnswer());
        assertTrue(checks.noPosition());
        assertTrue(checks.inconsistentPosition());
        assertTrue(checks.noConclusion());
    }

    @Test
    void flagsStayFalseWhenTheModelGaveNothingToJudgeBy() throws Exception {
        Task2Checks empty = TaskChecksParser.task2(node("{}"), ESSAY_2);
        Task2Checks notRequired = TaskChecksParser.task2(node("""
                {"questionParts":[{"part":"a","addressed":"FULL","excerpt":"Some say schools matter."}],
                 "position":{"required":false,"stated":false},
                 "outline":[{"role":"CONCLUSION","mainIdea":"c","developed":true}]}"""), ESSAY_2);

        assertFalse(empty.partialAnswer());
        assertFalse(empty.noPosition());
        assertFalse(empty.inconsistentPosition());
        assertFalse(empty.noConclusion());
        assertFalse(notRequired.partialAnswer());
        assertFalse(notRequired.noPosition());
        assertFalse(notRequired.noConclusion());
    }
}
