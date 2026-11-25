package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.AiDraftClaim;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiDraftJobStateUseCasesTest {
    private final Instant now = Instant.parse("2026-10-23T10:00:00Z");
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final AiUsageQuota quota = mock(AiUsageQuota.class);
    private final AiDraftJobStateUseCases state = new AiDraftJobStateUseCases(drafts, quota,
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void aReplacedWorkerCannotCompleteOrRefundTheSameClaim() {
        AiDraft draft = draft();
        AiDraftClaim claim = new AiDraftClaim(draft.getId(), UUID.randomUUID());
        when(drafts.failIfClaimed(draft.getId(), claim.claimToken(), "INTERNAL", now)).thenReturn(0);

        assertFalse(state.fail(claim, draft, "INTERNAL", now));

        verify(quota, never()).refund(any(), any(), any());
    }

    @Test
    void failureRefundsTheAllowanceOnlyWhenTheClaimWasChanged() {
        AiDraft draft = draft();
        AiDraftClaim claim = new AiDraftClaim(draft.getId(), UUID.randomUUID());
        when(drafts.failIfClaimed(draft.getId(), claim.claimToken(), "INTERNAL", now)).thenReturn(1);

        assertTrue(state.fail(claim, draft, "INTERNAL", now));

        verify(quota).refund(draft.getRequestedBy(), LocalDate.of(2026, 10, 23), AiUsageKind.AI);
    }

    @Test
    void staleDraftsRefundOnlyAfterTheLastConfiguredAttempt() {
        AiDraft exhausted = draft();
        when(drafts.requeueStaleGenerating(now.minusSeconds(600), 3, now)).thenReturn(List.of(exhausted));

        assertEquals(1, state.recoverStale(now.minusSeconds(600), 3, now));

        verify(quota).refund(exhausted.getRequestedBy(), LocalDate.of(2026, 10, 23), AiUsageKind.AI);
    }

    private AiDraft draft() {
        return new AiDraft(UUID.randomUUID(), DraftKind.QUESTIONS, DraftStatus.GENERATING, UUID.randomUUID(), "{}",
                null, null, null, null, now, now, null, null, null, null);
    }
}
