package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.ErrorType;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The codes and rubric instructions the grader shows the model, per Writing task. */
class EssayGraderWritingDefinitionsTest {
    private static final String RUBRIC_INSTRUCTION = "How to choose a band for each criterion:\n1. Apply the ceilings first.";

    private static final ErrorType BOTH_TASKS = new ErrorType("MISSING_VERB", "Thiếu động từ", "GRAMMAR", 2,
            "Hint for learners", null, "Use when a clause has no finite verb.", List.of());
    private static final ErrorType TASK_ONE_ONLY = new ErrorType("T1_IRRELEVANT_CONTENT", "Ý kiến", "TASK", 2,
            "Learner hint", null, "Use when the report gives an opinion the chart does not show.", List.of("TASK_1"));
    private static final ErrorType TASK_TWO_ONLY = new ErrorType("OFF_TOPIC", "Lạc đề", "TASK", 3,
            "Đọc lại đề", null, null, List.of("TASK_2"));

    @Test
    void eachTaskSeesOnlyItsOwnCodesAndTheCodesForBoth() {
        List<ErrorType> errors = List.of(BOTH_TASKS, TASK_ONE_ONLY, TASK_TWO_ONLY);
        String taskOne = EssayGrader.systemPrompt(WritingTask.TASK_1, rubric("TASK_1"), EssayGrader.appliesTo(errors,
                WritingTask.TASK_1), "v2");
        String taskTwo = EssayGrader.systemPrompt(WritingTask.TASK_2, rubric("TASK_2"), EssayGrader.appliesTo(errors,
                WritingTask.TASK_2), "v2");

        assertTrue(taskOne.contains("MISSING_VERB —"));
        assertTrue(taskOne.contains("T1_IRRELEVANT_CONTENT —"));
        assertFalse(taskOne.contains("OFF_TOPIC —"));
        assertTrue(taskTwo.contains("MISSING_VERB —"));
        assertTrue(taskTwo.contains("OFF_TOPIC —"));
        assertFalse(taskTwo.contains("T1_IRRELEVANT_CONTENT —"));
    }

    @Test
    void theModelReadsTheDefinitionAndFallsBackToTheLearnerHint() {
        List<ErrorType> errors = List.of(BOTH_TASKS, TASK_TWO_ONLY);
        String prompt = EssayGrader.systemPrompt(WritingTask.TASK_2, rubric("TASK_2"),
                EssayGrader.appliesTo(errors, WritingTask.TASK_2), "v2");

        assertTrue(prompt.contains("MISSING_VERB — Thiếu động từ: Use when a clause has no finite verb."));
        assertFalse(prompt.contains("Hint for learners"));
        assertTrue(prompt.contains("OFF_TOPIC — Lạc đề: Đọc lại đề"));
    }

    @Test
    void theRubricInstructionComesBeforeItsCriteriaWhenPresent() {
        String prompt = EssayGrader.systemPrompt(WritingTask.TASK_2, rubric("TASK_2"), List.of(BOTH_TASKS), "v2");

        int instruction = prompt.indexOf(RUBRIC_INSTRUCTION);
        assertTrue(instruction >= 0, "rubric instruction missing");
        assertTrue(instruction < prompt.indexOf("TR (TR): Description"));
    }

    @Test
    void aRubricWithoutInstructionKeepsThePromptWithoutThatBlock() {
        var plain = new LearningContentClient.Rubric("IELTS_ESSAY", "TASK_2", 1, "source", null,
                criteria("TASK_2"), List.of());
        String prompt = EssayGrader.systemPrompt(WritingTask.TASK_2, plain, List.of(BOTH_TASKS), "v2");

        assertFalse(prompt.contains("How to choose a band"));
        assertTrue(prompt.contains("TR (TR): Description"));
    }

    @Test
    void theGraderAcceptsOnlyTheCodesOfItsTask() {
        Set<String> taskOneCodes = EssayGrader.appliesTo(List.of(BOTH_TASKS, TASK_ONE_ONLY, TASK_TWO_ONLY),
                WritingTask.TASK_1).stream().map(ErrorType::code).collect(Collectors.toSet());

        assertEquals(Set.of("MISSING_VERB", "T1_IRRELEVANT_CONTENT"), taskOneCodes);
    }

    @Test
    void theThirdTemplateAsksForTheNewOrderAndTheCodeOfTaskOneOpinions() {
        String third = EssayGrader.systemPrompt(WritingTask.TASK_1, rubric("TASK_1"), List.of(BOTH_TASKS, TASK_ONE_ONLY),
                "v3");
        String second = EssayGrader.systemPrompt(WritingTask.TASK_1, rubric("TASK_1"), List.of(BOTH_TASKS, TASK_ONE_ONLY),
                "v2");

        assertTrue(third.contains("Write the fields in exactly this order"));
        assertTrue(third.contains("T1_IRRELEVANT_CONTENT"));
        assertTrue(third.contains("Every sentence marked \"WRONG\""));
        assertFalse(third.contains("[["), "task markers are removed");
        assertFalse(second.contains("Write the fields in exactly this order"));
    }

    @Test
    void aLongTextIsCutAtASentenceEndWhenOneFallsInTheSecondHalfOfTheLimit() {
        assertEquals("A sentence that comes first and is long enough.", EssayGrader.truncateAtSentence(
                "A sentence that comes first and is long enough. Then another sentence that runs well past the limit.", 60));
    }

    @Test
    void aLongTextWithoutASentenceEndIsCutAtASpace() {
        assertEquals("alpha beta", EssayGrader.truncateAtSentence("alpha beta gamma delta epsilon", 12));
    }

    @Test
    void aTextWithinTheLimitIsUnchanged() {
        assertEquals("short text", EssayGrader.truncateAtSentence("short text", 60));
    }

    private static LearningContentClient.Rubric rubric(String task) {
        return new LearningContentClient.Rubric("IELTS_ESSAY", task, 1, "source", RUBRIC_INSTRUCTION,
                criteria(task), List.of());
    }

    private static List<LearningContentClient.RubricCriterion> criteria(String task) {
        List<String> codes = "TASK_1".equals(task) ? List.of("TA", "CC", "LR", "GRA") : List.of("TR", "CC", "LR", "GRA");
        return codes.stream().map(code -> new LearningContentClient.RubricCriterion(code, code, "Description", 1,
                List.of(new LearningContentClient.RubricBandDescriptor(new BigDecimal("6.0"), "Descriptor")), null,
                List.of())).toList();
    }

    private static final ErrorType WHOLE_ESSAY = new ErrorType("T1_NO_OVERVIEW", "Thiếu overview", "TASK", 3,
            "Viết overview", null, "The essay has no overview.", List.of("TASK_1"), false);

    @Test
    void aCodeThatDescribesTheWholeEssayIsNotOfferedToTheModel() {
        List<ErrorType> errors = List.of(BOTH_TASKS, TASK_ONE_ONLY, WHOLE_ESSAY);

        var offered = EssayGrader.appliesTo(errors, WritingTask.TASK_1);
        String prompt = EssayGrader.systemPrompt(WritingTask.TASK_1, rubric("TASK_1"), offered, "v3");

        assertEquals(List.of("MISSING_VERB", "T1_IRRELEVANT_CONTENT"), offered.stream().map(ErrorType::code).toList());
        assertFalse(prompt.contains("T1_NO_OVERVIEW"));
        assertTrue(prompt.contains("T1_IRRELEVANT_CONTENT —"));
    }

    @Test
    void aContentThatDoesNotSendTheFlagLeavesEveryCodeOffered() {
        var withoutFlag = new ErrorType("T1_NO_OVERVIEW", "Thiếu overview", "TASK", 3, "Viết overview", null,
                "The essay has no overview.", List.of("TASK_1"));
        var explicitlyTrue = new ErrorType("TENSE", "Thì", "GRAMMAR", 1, "Hint", null, null, List.of(), true);

        assertTrue(withoutFlag.selectableByModel());
        assertTrue(explicitlyTrue.selectableByModel());
        assertFalse(WHOLE_ESSAY.selectableByModel());
        assertEquals(2, EssayGrader.appliesTo(List.of(withoutFlag, explicitlyTrue), WritingTask.TASK_1).size());
    }
}
