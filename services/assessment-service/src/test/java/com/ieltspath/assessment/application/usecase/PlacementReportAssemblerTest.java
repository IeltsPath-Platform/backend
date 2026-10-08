package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.result.PlacementResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementReportAssemblerTest {
    @Mock AttemptSectionRepository sections;
    @Mock AttemptItemRepository attemptItems;
    @Mock AttemptResponseRepository responses;
    @Mock ItemResultRepository itemResults;
    @Mock GateEssayJobStore jobs;
    @Mock PlacementGradingService grading;

    private final ObjectMapper json = new ObjectMapper();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID resultId = UUID.randomUUID();
    private final UUID reading = UUID.randomUUID();
    private final UUID writing = UUID.randomUUID();
    private final UUID speaking = UUID.randomUUID();

    private final AttemptItem choice = item(reading, 1,
            "{\"stem\":\"How long can a member keep a tool?\",\"options\":[{\"optionKey\":\"A\",\"content\":\"One day\"},"
                    + "{\"optionKey\":\"B\",\"content\":\"One week\"}]}",
            "{\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"B\"},\"explanation\":\"Paragraph A.\",\"maxScore\":1}");
    private final AttemptItem gap = item(reading, 2, "{\"stem\":\"Volunteers check each tool within ______.\"}",
            "{\"answerSpec\":{\"type\":\"FILL\",\"accepted\":[\"a day\",\"one day\"]},\"maxScore\":1}");
    private final AttemptItem tfng = item(reading, 3,
            "{\"stem\":\"Members pay a fine.\",\"options\":[{\"optionKey\":\"TRUE\",\"content\":\"TRUE\"},"
                    + "{\"optionKey\":\"NOT_GIVEN\",\"content\":\"NOT GIVEN\"}]}",
            "{\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"NOT_GIVEN\"},\"maxScore\":1}");
    private final AttemptItem graded = item(writing, 1, "{\"stem\":\"Describe the table.\"}",
            "{\"answerSpec\":{\"type\":\"ESSAY\",\"task\":\"TASK_1\",\"passBand\":5.5},\"maxScore\":1}");
    private final AttemptItem blank = item(writing, 2, "{\"stem\":\"Discuss.\"}",
            "{\"answerSpec\":{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":5.5},\"maxScore\":1}");

    @Test
    void listsEveryQuestionWithBothAnswersAndEveryEssayWithItsComments() {
        when(sections.findByAttemptId(attemptId)).thenReturn(List.of(
                section(speaking, 3, "{\"title\":\"Speaking\",\"skill\":\"SPEAKING\"}"),
                section(reading, 1, "{\"title\":\"Reading: a tool library\",\"skill\":\"READING\"}"),
                section(writing, 2, "{\"title\":\"Writing\",\"skill\":\"WRITING\"}")));
        when(attemptItems.findByAttemptId(attemptId)).thenReturn(List.of(gap, choice, tfng, graded, blank));
        when(responses.findByAttemptItemIds(anyList())).thenReturn(List.of(
                response(choice, "{\"answer\":\"B\"}"), response(gap, "{\"answer\":\"two days\"}"),
                response(tfng, "{\"answer\":\"\"}")));
        when(itemResults.findByResultId(resultId)).thenReturn(List.of(
                result(choice, true), result(gap, false), result(tfng, false)));
        when(jobs.aiJobs(attemptId)).thenReturn(List.of(new GateEssayJobStore.JobState(graded.id(), "COMPLETED",
                new BigDecimal("6.0"), "{\"summary\":\"Thiếu overview.\",\"criteria\":[{\"code\":\"TA\",\"band\":5.5,"
                + "\"comment\":\"Chưa có overview.\"}],\"focus\":[\"Overview Writing\"]}")));

        List<PlacementResult.Section> report = assembler().sections(attemptId, resultId);

        assertThat(report).extracting(PlacementResult.Section::skill)
                .containsExactly(Skill.READING, Skill.WRITING, Skill.SPEAKING);
        assertThat(report.get(0).questions()).containsExactly(
                new PlacementResult.Question(1, "How long can a member keep a tool?", "B. One week", "B. One week",
                        true, "Paragraph A."),
                new PlacementResult.Question(2, "Volunteers check each tool within ______.", "two days",
                        "a day / one day", false, null),
                new PlacementResult.Question(3, "Members pay a fine.", null, "NOT GIVEN", false, null));

        PlacementResult.Essay task1 = report.get(1).essays().get(0);
        assertThat(task1.task()).isEqualTo("TASK_1");
        assertThat(task1.band()).isEqualTo(6.0);
        assertThat(task1.feedback().summary()).isEqualTo("Thiếu overview.");
        assertThat(task1.feedback().criteria()).containsExactly(new PlacementResult.Criterion("TA", 5.5, "Chưa có overview."));
        assertThat(task1.feedback().focus()).containsExactly("Overview Writing");
        assertThat(report.get(1).essays().get(1)).isEqualTo(new PlacementResult.Essay("TASK_2", 0.0, false, null));

        assertThat(report.get(2).questions()).isEmpty();
        assertThat(report.get(2).essays()).isEmpty();
    }

    @Test
    void anEssayGradedWithoutCommentsKeepsItsBandAndNoFeedback() {
        when(sections.findByAttemptId(attemptId)).thenReturn(List.of(
                section(writing, 1, "{\"title\":\"Writing\",\"skill\":\"WRITING\"}")));
        when(attemptItems.findByAttemptId(attemptId)).thenReturn(List.of(graded));
        when(responses.findByAttemptItemIds(anyList())).thenReturn(List.of());
        when(itemResults.findByResultId(resultId)).thenReturn(List.of());
        when(jobs.aiJobs(attemptId)).thenReturn(List.of(new GateEssayJobStore.JobState(graded.id(), "COMPLETED", null)));
        when(grading.defaultWritingBand()).thenReturn(new BigDecimal("5.5"));

        PlacementResult.Essay essay = assembler().sections(attemptId, resultId).get(0).essays().get(0);

        assertThat(essay).isEqualTo(new PlacementResult.Essay("TASK_1", 5.5, true, null));
    }

    private PlacementReportAssembler assembler() {
        return new PlacementReportAssembler(sections, attemptItems, responses, itemResults, jobs, grading, json);
    }

    private AttemptSection section(UUID id, int order, String snapshot) {
        return new AttemptSection(id, attemptId, UUID.randomUUID(), order, snapshot);
    }

    private static AttemptItem item(UUID sectionId, int order, String question, String answer) {
        return new AttemptItem(UUID.randomUUID(), sectionId, UUID.randomUUID(), order, question, answer, null);
    }

    private static AttemptResponse response(AttemptItem item, String payload) {
        return new AttemptResponse(UUID.randomUUID(), item.id(), payload, 1, 1, Instant.now(), null);
    }

    private ItemResult result(AttemptItem item, boolean correct) {
        return new ItemResult(UUID.randomUUID(), resultId, item.id(), correct ? 1.0 : 0.0, 1.0, correct, null, "{}");
    }
}
