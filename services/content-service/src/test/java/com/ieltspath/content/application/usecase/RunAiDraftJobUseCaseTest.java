package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiDraftGenerator;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.AiDraftClaim;
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
    private final AiDraftJobStateUseCases state = mock(AiDraftJobStateUseCases.class);
    private final AiDraftGenerator questions = mock(AiDraftGenerator.class);

    @Test
    void successfulGenerationStoresTheResultAndKeepsTheUse() {
        AiDraft draft = claimed(DraftKind.QUESTIONS);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);
        when(questions.generate(any())).thenReturn(new GenerationOutcome("{\"items\":[1]}", "m", "p1"));
        when(state.complete(any(), any(), any(), any(), any())).thenReturn(true);

        useCase().runBatch(2, Duration.ofMinutes(10));

        verify(state).complete(any(), org.mockito.ArgumentMatchers.eq("{\"items\":[1]}"),
                org.mockito.ArgumentMatchers.eq("m"), org.mockito.ArgumentMatchers.eq("p1"), org.mockito.ArgumentMatchers.eq(now));
    }

    @Test
    void notConfiguredFailsTheDraftAndGivesTheUseBack() {
        AiDraft draft = claimed(DraftKind.QUESTIONS);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);
        when(questions.generate(any())).thenThrow(
                new ContentGenerationException(ContentGenerationException.NOT_CONFIGURED, "no model"));
        when(state.fail(any(), any(), org.mockito.ArgumentMatchers.eq("NOT_CONFIGURED"), any())).thenReturn(true);

        useCase().runBatch(2, Duration.ofMinutes(10));

        verify(state).fail(any(), org.mockito.ArgumentMatchers.eq(draft),
                org.mockito.ArgumentMatchers.eq("NOT_CONFIGURED"), org.mockito.ArgumentMatchers.eq(now));
    }

    @Test
    void kindWithoutAGeneratorFailsInsteadOfHanging() {
        AiDraft draft = claimed(DraftKind.COURSE);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);
        when(state.fail(any(), any(), org.mockito.ArgumentMatchers.eq("UNSUPPORTED_KIND"), any())).thenReturn(true);

        useCase().runBatch(2, Duration.ofMinutes(10));

        verify(state).fail(any(), org.mockito.ArgumentMatchers.eq(draft),
                org.mockito.ArgumentMatchers.eq("UNSUPPORTED_KIND"), org.mockito.ArgumentMatchers.eq(now));
    }

    @Test
    void unexpectedErrorFailsTheDraftWithoutLeakingItsMessage() {
        AiDraft draft = claimed(DraftKind.QUESTIONS);
        when(questions.kind()).thenReturn(DraftKind.QUESTIONS);
        when(questions.generate(any())).thenThrow(new IllegalStateException("prompt text: secret"));
        when(state.fail(any(), any(), org.mockito.ArgumentMatchers.eq("INTERNAL"), any())).thenReturn(true);

        useCase().runBatch(2, Duration.ofMinutes(10));

        verify(state).fail(any(), org.mockito.ArgumentMatchers.eq(draft),
                org.mockito.ArgumentMatchers.eq("INTERNAL"), org.mockito.ArgumentMatchers.eq(now));
    }

    @Test
    void runRequeuesStaleWorkBeforeClaiming() {
        when(state.recoverStale(any(), anyInt(), any())).thenReturn(1);
        when(drafts.claimQueued(anyInt())).thenReturn(List.of());

        int claimed = useCase().runBatch(2, Duration.ofMinutes(10));

        assertEquals(0, claimed);
        verify(state).recoverStale(now.minus(Duration.ofMinutes(10)), 3, now);
    }

    private AiDraft claimed(DraftKind kind) {
        UUID id = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        AiDraft draft = new AiDraft(id, kind, DraftStatus.GENERATING, UUID.randomUUID(), "{}", null, null, null, null,
                now, now, null, null, null, null);
        when(drafts.claimQueued(anyInt())).thenReturn(List.of(new AiDraftClaim(id, token)), List.of());
        when(drafts.findById(id)).thenReturn(Optional.of(draft));
        return draft;
    }

    private RunAiDraftJobUseCase useCase() {
        return new RunAiDraftJobUseCase(drafts, state, List.of(questions), clock);
    }
}
