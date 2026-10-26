package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiDraftTest {

    private final Instant now = Instant.parse("2026-10-23T00:00:00Z");

    @Test
    void generatedDraftBecomesReadyAndKeepsItsVersions() {
        AiDraft draft = generating();

        draft.complete("{\"items\":[]}", "model-x", "prompt-1", now);

        assertEquals(DraftStatus.READY, draft.getStatus());
        assertEquals("prompt-1", draft.getPromptVersion());
        assertEquals(now, draft.getFinishedAt());
    }

    @Test
    void generatingIsRequiredBeforeCompletingOrFailing() {
        AiDraft queued = AiDraft.request(DraftKind.QUESTIONS, UUID.randomUUID(), "{}", now);

        assertThrows(InvalidContentStateException.class, () -> queued.complete("{}", "m", "p", now));
        assertThrows(InvalidContentStateException.class, () -> queued.fail("FAILED", now));
    }

    @Test
    void readyDraftCanBeRejectedOrApprovedOnce() {
        AiDraft draft = generating();
        draft.complete("{}", "m", "p", now);
        draft.reject(UUID.randomUUID(), "wrong answer", now);

        assertEquals(DraftStatus.REJECTED, draft.getStatus());
        assertThrows(InvalidContentStateException.class, () -> draft.approve(UUID.randomUUID(), now));
    }

    @Test
    void failedDraftCannotBeApproved() {
        AiDraft draft = generating();
        draft.fail("NOT_CONFIGURED", now);

        assertThrows(InvalidContentStateException.class, () -> draft.approve(UUID.randomUUID(), now));
    }

    @Test
    void requeueReturnsAGeneratingDraftToTheQueue() {
        AiDraft draft = generating();

        draft.requeue();

        assertEquals(DraftStatus.QUEUED, draft.getStatus());
    }

    private AiDraft generating() {
        AiDraft draft = AiDraft.request(DraftKind.QUESTIONS, UUID.randomUUID(), "{}", now);
        return new AiDraft(draft.getId(), draft.getKind(), DraftStatus.GENERATING, draft.getRequestedBy(),
                draft.getRequestJson(), null, null, null, null, now, now, null, null, null, null);
    }
}
