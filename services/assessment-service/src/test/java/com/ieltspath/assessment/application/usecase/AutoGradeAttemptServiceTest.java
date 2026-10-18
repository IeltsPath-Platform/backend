package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.entity.ErrorAnalysisItem;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemKnowledgePointRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.ErrorAnalysisItemRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.repository.SkillScoreRepository;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoGradeAttemptServiceTest {
    private static final String CHOICE_SNAPSHOT = "{\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"A\","
            + "\"distractors\":{\"B\":\"PARAPHRASE_TRAP\",\"C\":\"KEYWORD_MATCH_ONLY\"}},\"maxScore\":1}";

    @Mock AttemptItemRepository attemptItems;
    @Mock AttemptResponseRepository responses;
    @Mock AssessmentResultRepository results;
    @Mock ItemResultRepository itemResults;
    @Mock ErrorAnalysisItemRepository errors;
    @Mock AttemptItemKnowledgePointRepository knowledgeSnapshot;

    private final UUID attemptId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID attemptItemId = UUID.randomUUID();
    private final UUID knowledgePointId = UUID.randomUUID();

    @Test
    void wrongDistractorAnswerIsRecordedAgainstEachKnowledgePointOfTheItem() {
        stubGradingOf("{\"answer\":\"B\"}");
        when(knowledgeSnapshot.findByAttemptItemIds(anyCollection())).thenReturn(List.of(
                new AttemptItemKnowledgePoint(attemptItemId, knowledgePointId, BigDecimal.ONE)));

        assertThat(service().gradeIfObjective(attempt())).isTrue();

        ArgumentCaptor<List<ItemResult>> graded = ArgumentCaptor.forClass(List.class);
        verify(itemResults).saveAll(graded.capture());
        ArgumentCaptor<List<ErrorAnalysisItem>> tags = ArgumentCaptor.forClass(List.class);
        verify(errors).saveAll(tags.capture());
        assertThat(tags.getValue()).hasSize(1);
        ErrorAnalysisItem row = tags.getValue().get(0);
        assertThat(row.errorType()).isEqualTo("PARAPHRASE_TRAP");
        assertThat(row.knowledgePointId()).isEqualTo(knowledgePointId);
        assertThat(row.itemResultId()).isEqualTo(graded.getValue().get(0).id());
    }

    @Test
    void correctAnswerWritesNoErrorRows() {
        stubGradingOf("{\"answer\":\"A\"}");

        assertThat(service().gradeIfObjective(attempt())).isTrue();

        verify(errors, never()).saveAll(any());
    }

    @Test
    void wrongAnswerWithoutDistractorWritesNoErrorRows() {
        stubGradingOf("{\"answer\":\"D\"}");

        assertThat(service().gradeIfObjective(attempt())).isTrue();

        verify(errors, never()).saveAll(any());
    }

    @Test
    void taggedItemWithoutKnowledgePointStillKeepsItsTagWithANullKnowledgePoint() {
        stubGradingOf("{\"answer\":\"C\"}");
        when(knowledgeSnapshot.findByAttemptItemIds(anyCollection())).thenReturn(List.of());

        service().gradeIfObjective(attempt());

        ArgumentCaptor<List<ErrorAnalysisItem>> tags = ArgumentCaptor.forClass(List.class);
        verify(errors).saveAll(tags.capture());
        assertThat(tags.getValue()).singleElement().satisfies(row -> {
            assertThat(row.errorType()).isEqualTo("KEYWORD_MATCH_ONLY");
            assertThat(row.knowledgePointId()).isNull();
        });
    }

    @Test
    void storedElapsedTimeIsCopiedIntoTheItemResult() {
        stubGradingOf("{\"answer\":\"A\"}");
        when(attemptItems.elapsedMsByItemId(anyCollection())).thenReturn(Map.of(attemptItemId, 4200L));

        service().gradeIfObjective(attempt());

        ArgumentCaptor<List<ItemResult>> graded = ArgumentCaptor.forClass(List.class);
        verify(itemResults).saveAll(graded.capture());
        assertThat(graded.getValue().get(0).durationMilliseconds()).isEqualTo(4200L);
    }

    /** One objective item with a stored response, graded by the real service; the result save returns its input. */
    private void stubGradingOf(String payload) {
        when(attemptItems.findByAttemptId(attemptId)).thenReturn(List.of(
                new AttemptItem(attemptItemId, UUID.randomUUID(), UUID.randomUUID(), 1, "{}", CHOICE_SNAPSHOT, null)));
        when(responses.findByAttemptItemIds(anyCollection())).thenReturn(List.of(
                new AttemptResponse(UUID.randomUUID(), attemptItemId, payload, 1, 1, Instant.now(), Instant.now())));
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AssessmentAttempt attempt() {
        Instant now = Instant.now();
        return new AssessmentAttempt(attemptId, userId, UUID.randomUUID(), AttemptType.MOCK, AttemptMode.STANDARD,
                AttemptChannel.WEB, AttemptStatus.SUBMITTED, now, now, null, 1, now, now);
    }

    private AutoGradeAttemptService service() {
        return new AutoGradeAttemptService(attemptItems, responses, results, itemResults, errors, knowledgeSnapshot,
                mock(AssessmentResultCompleter.class), new ObjectMapper(), mock(SkillScoreRepository.class));
    }
}
