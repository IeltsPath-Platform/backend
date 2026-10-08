package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementResumeUseCasesTest {
    @Mock AssessmentAttemptRepository attempts;
    @Mock AttemptItemRepository items;
    @Mock AttemptResponseRepository responses;

    private final UUID userId = UUID.randomUUID();

    @Test
    void currentPlacementIsTheNewestPlacementAttemptIgnoringOtherTypes() {
        Instant now = Instant.now();
        AssessmentAttempt older = attempt(AttemptType.PLACEMENT, AttemptStatus.SUBMITTED, now.minusSeconds(600));
        AssessmentAttempt newer = attempt(AttemptType.PLACEMENT, AttemptStatus.IN_PROGRESS, now.minusSeconds(60));
        AssessmentAttempt topicGate = attempt(AttemptType.TOPIC_GATE, AttemptStatus.IN_PROGRESS, now);
        when(attempts.findByUserId(userId)).thenReturn(List.of(older, topicGate, newer));

        var result = new GetCurrentPlacementAttemptUseCase(attempts).execute(userId).orElseThrow();

        assertThat(result.id()).isEqualTo(newer.getId());
        assertThat(result.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    void withoutAPlacementAttemptThereIsNoCurrentOne() {
        when(attempts.findByUserId(userId)).thenReturn(List.of(attempt(AttemptType.TOPIC_GATE, AttemptStatus.IN_PROGRESS, Instant.now())));

        assertThat(new GetCurrentPlacementAttemptUseCase(attempts).execute(userId)).isEmpty();
    }

    @Test
    void responsesOfAnOwnedAttemptComeWithTheirRevisions() {
        AssessmentAttempt attempt = attempt(AttemptType.PLACEMENT, AttemptStatus.IN_PROGRESS, Instant.now());
        AttemptItem item = new AttemptItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, "{}", "{}", null);
        AttemptResponse saved = new AttemptResponse(UUID.randomUUID(), item.id(), "{\"answer\":\"B\"}", 1, 3, Instant.now(), null);
        when(attempts.findByIdAndUserId(attempt.getId(), userId)).thenReturn(Optional.of(attempt));
        when(items.findByAttemptId(attempt.getId())).thenReturn(List.of(item));
        when(responses.findByAttemptItemIds(List.of(item.id()))).thenReturn(List.of(saved));

        var result = new ListAttemptResponsesUseCase(attempts, items, responses).execute(userId, attempt.getId());

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.attemptItemId()).isEqualTo(item.id());
            assertThat(response.payload()).isEqualTo("{\"answer\":\"B\"}");
            assertThat(response.revision()).isEqualTo(3);
        });
    }

    @Test
    void responsesOfSomeoneElsesAttemptAreNotFound() {
        UUID attemptId = UUID.randomUUID();
        when(attempts.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ListAttemptResponsesUseCase(attempts, items, responses).execute(userId, attemptId))
                .isInstanceOf(AssessmentNotFoundException.class);
    }

    private AssessmentAttempt attempt(AttemptType type, AttemptStatus status, Instant createdAt) {
        return new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(), type, AttemptMode.STANDARD,
                AttemptChannel.WEB, status, createdAt, createdAt, null, 1, createdAt, createdAt);
    }
}
