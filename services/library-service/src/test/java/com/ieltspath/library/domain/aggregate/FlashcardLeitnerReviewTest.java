package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void rejectsMissingRequestValuesWithoutChangingTheCard() {
        Flashcard card = newCard();

        InvalidDataException missingRequestId = assertThrows(InvalidDataException.class,
                () -> card.review(true, null, Instant.parse("2026-10-17T00:00:00Z"), INTERVALS));
        assertEquals("review request is incomplete", missingRequestId.getMessage());
        InvalidDataException missingTime = assertThrows(InvalidDataException.class,
                () -> card.review(true, UUID.randomUUID(), null, INTERVALS));
        assertEquals("review request is incomplete", missingTime.getMessage());

        assertEquals(1, card.getLeitnerBox());
        assertEquals(0, card.getReviewCount());
        assertNull(card.getLastReviewedAt());
        assertNull(card.getNextReviewAt());
    }

    @Test
    void rejectsInvalidSchedulesWithoutChangingTheCard() {
        Flashcard card = newCard();

        InvalidDataException wrongSize = assertThrows(InvalidDataException.class,
                () -> card.review(true, UUID.randomUUID(), Instant.parse("2026-10-17T00:00:00Z"), List.of(1, 2, 4, 8)));
        assertEquals("Leitner schedule is invalid", wrongSize.getMessage());
        InvalidDataException nonPositiveInterval = assertThrows(InvalidDataException.class,
                () -> card.review(true, UUID.randomUUID(), Instant.parse("2026-10-17T00:00:00Z"), List.of(1, 2, 0, 8, 16)));
        assertEquals("Leitner schedule is invalid", nonPositiveInterval.getMessage());

        assertEquals(1, card.getLeitnerBox());
        assertEquals(0, card.getReviewCount());
        assertNull(card.getLastReviewedAt());
        assertNull(card.getNextReviewAt());
    }

    private static Flashcard newCard() {
        return Flashcard.create(UUID.randomUUID(), FlashcardSourceType.MANUAL, null, null, null, "Front", "Back");
    }

    @Test
    void oneCorrectReviewMovesToBoxTwoForTwoDays() {
        Flashcard card = newCard();
        Instant now = Instant.parse("2026-10-17T00:00:00Z");
        card.review(true, UUID.randomUUID(), now, INTERVALS);
        assertEquals(2, card.getLeitnerBox());
        assertEquals(now.plusSeconds(2L * 24 * 60 * 60), card.getNextReviewAt());
        assertEquals(1, card.getReviewCount());
    }

    @Test
    void aCorrectReviewInBoxFiveStaysInBoxFive() {
        Flashcard card = newCard();
        Instant now = Instant.parse("2026-10-17T00:00:00Z");
        for (int review = 0; review < 4; review++) {
            card.review(true, UUID.randomUUID(), now.plusSeconds(review), INTERVALS);
        }
        Instant fifth = now.plusSeconds(100);
        card.review(true, UUID.randomUUID(), fifth, INTERVALS);
        assertEquals(5, card.getLeitnerBox());
        assertEquals(fifth.plusSeconds(16L * 24 * 60 * 60), card.getNextReviewAt());
        assertEquals(5, card.getReviewCount());
    }

    @Test
    void aSenseCardNeedsASenseId() {
        assertEquals("vocabularySenseId không hợp lệ", assertThrows(InvalidDataException.class, () -> Flashcard.create(
                UUID.randomUUID(), FlashcardSourceType.VOCABULARY_SENSE, null, null, null, "word", "meaning")).getMessage());
    }

    @Test
    void aHighlightCardNeedsHighlightedText() {
        assertEquals("highlightedText không hợp lệ", assertThrows(InvalidDataException.class, () -> Flashcard.create(
                UUID.randomUUID(), FlashcardSourceType.HIGHLIGHT, null, null, null, "word", "meaning")).getMessage());
    }

    @Test
    void aBlankFrontIsRefused() {
        assertEquals("front không hợp lệ", assertThrows(InvalidDataException.class, () -> Flashcard.create(
                UUID.randomUUID(), FlashcardSourceType.MANUAL, null, null, null, " ", "Back")).getMessage());
    }

    @Test
    void aFrontHasAtMostFourThousandCharacters() {
        assertEquals(4000, Flashcard.create(UUID.randomUUID(), FlashcardSourceType.MANUAL, null, null, null,
                "f".repeat(4000), "Back").getFront().length());
        assertEquals("front không hợp lệ", assertThrows(InvalidDataException.class, () -> Flashcard.create(
                UUID.randomUUID(), FlashcardSourceType.MANUAL, null, null, null, "f".repeat(4001), "Back")).getMessage());
    }
}
