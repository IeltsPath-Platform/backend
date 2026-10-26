package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiDraftGenerator;
import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunAiDraftJobUseCaseTest {

    private final Instant now = Instant.parse("2026-10-23T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final AiUsageQuota quota = mock(AiUsageQuota.class);
    private final AiDraftGenerator questions = mock(AiDraftGenerator.class);

    @Test
    void successfulGenerationStoresTheResultAndKeepsTheUse() {
        AiDraft draft = claimed(DraftKind.QUESTIONS);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);
        when(questions.generate(any())).thenReturn(new GenerationOutcome("{\"items\":[1]}", "m", "p1"));

        useCase().runBatch(2, Duration.ofMinutes(10));

        assertEquals(DraftStatus.READY, draft.getStatus());
        assertEquals("{\"items\":[1]}", draft.getResultJson());
        verify(quota, org.mockito.Mockito.never()).refund(any(), any(), any());
    }

    @Test
    void notConfiguredFailsTheDraftAndGivesTheUseBack() {
        AiDraft draft = claimed(DraftKind.QUESTIONS);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);
        when(questions.generate(any())).thenThrow(
                new ContentGenerationException(ContentGenerationException.NOT_CONFIGURED, "no model"));

        useCase().runBatch(2, Duration.ofMinutes(10));

        assertEquals(DraftStatus.FAILED, draft.getStatus());
        assertEquals("NOT_CONFIGURED", draft.getErrorCode());
        verify(quota).refund(draft.getRequestedBy(), now.atZone(ZoneOffset.UTC).toLocalDate(), AiUsageKind.AI);
    }

    @Test
    void kindWithoutAGeneratorFailsInsteadOfHanging() {
        AiDraft draft = claimed(DraftKind.COURSE);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);

        useCase().runBatch(2, Duration.ofMinutes(10));

        assertEquals(DraftStatus.FAILED, draft.getStatus());
        assertEquals("UNSUPPORTED_KIND", draft.getErrorCode());
    }

    @Test
    void unexpectedErrorFailsTheDraftWithoutLeakingItsMessage() {
        AiDraft draft = claimed(DraftKind.QUESTIONS);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);
        when(questions.generate(any())).thenThrow(new IllegalStateException("prompt text: secret"));

        useCase().runBatch(2, Duration.ofMinutes(10));

        assertEquals(DraftStatus.FAILED, draft.getStatus());
        assertEquals("INTERNAL", draft.getErrorCode());
    }

    @Test
    void runRequeuesStaleWorkBeforeClaiming() {
        when(drafts.requeueStaleGenerating(any())).thenReturn(1);
        when(drafts.claimQueued(anyInt())).thenReturn(List.of());

        int claimed = useCase().runBatch(2, Duration.ofMinutes(10));

        assertEquals(0, claimed);
        verify(drafts).requeueStaleGenerating(now.minus(Duration.ofMinutes(10)));
    }

    private AiDraft claimed(DraftKind kind) {
        UUID id = UUID.randomUUID();
        AiDraft draft = new AiDraft(id, kind, DraftStatus.GENERATING, UUID.randomUUID(), "{}", null, null, null, null,
                now, now, null, null, null, null);
        when(drafts.claimQueued(anyInt())).thenReturn(List.of(id));
        when(drafts.findById(id)).thenReturn(Optional.of(draft));
        when(drafts.save(any(AiDraft.class))).thenAnswer(call -> call.getArgument(0));
        return draft;
    }

    private RunAiDraftJobUseCase useCase() {
        return new RunAiDraftJobUseCase(drafts, quota, List.of(questions), clock);
    }
}
