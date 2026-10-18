package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.vo.FlashcardSourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashcardLeitnerReviewTest {
    private static final List<Integer> INTERVALS = List.of(1, 2, 4, 8, 16);

    @Test
    void newCardIsInBoxOneAndDueImmediately() {
        Flashcard card = newCard();

        assertEquals(1, card.getLeitnerBox());
        assertNull(card.getNextReviewAt());
        assertNull(card.getLastReviewedAt());
        assertEquals(0, card.getReviewCount());
    }

    @Test
    void fourCorrectReviewsAdvanceToBoxFiveAndScheduleSixteenDays() {
        Flashcard card = newCard();
        Instant now = Instant.parse("2026-10-17T00:00:00Z");

        for (int review = 0; review < 4; review++) {
            card.review(true, UUID.randomUUID(), now.plusSeconds(review), INTERVALS);
        }

        assertEquals(5, card.getLeitnerBox());
        assertEquals(now.plusSeconds(3).plusSeconds(16L * 24 * 60 * 60), card.getNextReviewAt());
        assertEquals(4, card.getReviewCount());
    }

    @Test
    void incorrectReviewResetsToBoxOneAndRetryingTheSameRequestIsANoOp() {
        Flashcard card = newCard();
        Instant now = Instant.parse("2026-10-17T00:00:00Z");
        card.review(true, UUID.randomUUID(), now, INTERVALS);
        UUID requestId = UUID.randomUUID();

        assertTrue(card.review(false, requestId, now.plusSeconds(1), INTERVALS));
        Instant scheduled = card.getNextReviewAt();
        assertFalse(card.review(true, requestId, now.plusSeconds(10), INTERVALS));

        assertEquals(1, card.getLeitnerBox());
        assertEquals(now.plusSeconds(1).plusSeconds(24L * 60 * 60), scheduled);
        assertEquals(scheduled, card.getNextReviewAt());
        assertEquals(2, card.getReviewCount());
    }

    private static Flashcard newCard() {
        return Flashcard.create(UUID.randomUUID(), FlashcardSourceType.MANUAL, null, null, null, "Front", "Back");
    }
}
