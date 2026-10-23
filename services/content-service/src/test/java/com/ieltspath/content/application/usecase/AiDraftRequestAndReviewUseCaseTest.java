package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.exception.AiQuotaExceededException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiDraftRequestAndReviewUseCaseTest {

    private final Instant now = Instant.parse("2026-10-23T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final AiUsageQuota quota = mock(AiUsageQuota.class);

    @Test
    void requestTakesOneUseAndQueuesTheDraft() {
        UUID author = UUID.randomUUID();
        when(quota.tryConsume(eq(author), any(), eq(AiUsageKind.AI), eq(20))).thenReturn(true);
        when(drafts.save(any(AiDraft.class))).thenAnswer(call -> call.getArgument(0));

        AiDraft queued = new StartAiDraftUseCase(drafts, quota, clock)
                .execute(author, DraftKind.QUESTIONS, "{\"count\":5}", 20);

        assertEquals(DraftStatus.QUEUED, queued.getStatus());
        assertEquals(author, queued.getRequestedBy());
    }

    @Test
    void exhaustedAllowanceRefusesBeforeAnyDraftIsStored() {
        when(quota.tryConsume(any(), any(), any(), anyInt())).thenReturn(false);

        assertThrows(AiQuotaExceededException.class, () -> new StartAiDraftUseCase(drafts, quota, clock)
                .execute(UUID.randomUUID(), DraftKind.QUESTIONS, "{}", 20));
        verify(drafts, never()).save(any());
    }

    @Test
    void anotherAuthorsDraftLooksLikeItDoesNotExist() {
        AiDraft mine = draft(UUID.randomUUID(), DraftStatus.READY);
        when(drafts.findById(mine.getId())).thenReturn(Optional.of(mine));
        UUID stranger = UUID.randomUUID();

        assertThrows(AiDraftNotFoundException.class,
                () -> new AiDraftReviewUseCases(drafts, clock).get(stranger, mine.getId()));
    }

    @Test
    void authorCanRejectOwnReadyDraft() {
        UUID author = UUID.randomUUID();
        AiDraft ready = draft(author, DraftStatus.READY);
        when(drafts.findById(ready.getId())).thenReturn(Optional.of(ready));
        when(drafts.save(any(AiDraft.class))).thenAnswer(call -> call.getArgument(0));

        AiDraft rejected = new AiDraftReviewUseCases(drafts, clock).reject(author, ready.getId(), "off topic");

        assertEquals(DraftStatus.REJECTED, rejected.getStatus());
        assertEquals("off topic", rejected.getReviewNote());
    }

    @Test
    void queuedDraftCannotBeRejected() {
        UUID author = UUID.randomUUID();
        AiDraft queued = draft(author, DraftStatus.QUEUED);
        when(drafts.findById(queued.getId())).thenReturn(Optional.of(queued));

        assertThrows(InvalidContentStateException.class,
                () -> new AiDraftReviewUseCases(drafts, clock).reject(author, queued.getId(), null));
    }

    private AiDraft draft(UUID author, DraftStatus status) {
        return new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, status, author, "{}", "{}", null, null, null,
                now, now, now, null, null, null);
    }
}
