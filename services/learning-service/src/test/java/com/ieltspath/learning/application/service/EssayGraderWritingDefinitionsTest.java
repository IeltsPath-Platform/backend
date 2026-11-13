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
}
