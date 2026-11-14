package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardUnderstandingLevelTest {
    @Test void mapsBoxesToLevelsIncludingBothMasteredBoxes() {
        assertEquals(UnderstandingLevel.UNKNOWN, UnderstandingLevel.fromBox(1));
        assertEquals(UnderstandingLevel.FAMILIAR, UnderstandingLevel.fromBox(2));
        assertEquals(UnderstandingLevel.LEARNING, UnderstandingLevel.fromBox(3));
        assertEquals(UnderstandingLevel.MASTERED, UnderstandingLevel.fromBox(4));
        assertEquals(UnderstandingLevel.MASTERED, UnderstandingLevel.fromBox(5));
        assertThrows(InvalidDataException.class, () -> UnderstandingLevel.fromBox(0));
    }

    @Test void settingLevelSetsScheduleAndReplaysOnlyTheLatestRequestId() {
        Flashcard card = Flashcard.create(UUID.randomUUID(), FlashcardSourceType.VOCABULARY_SENSE,
                UUID.randomUUID(), null, null, "word", "meaning");
        Instant now = Instant.parse("2026-10-26T10:00:00Z");
        UUID request = UUID.randomUUID();
        assertTrue(card.setUnderstanding(UnderstandingLevel.MASTERED, now, List.of(1, 2, 4, 8, 16), request));
        assertEquals(4, card.getLeitnerBox());
        assertEquals(now.plusSeconds(8 * 86400L), card.getNextReviewAt());
        assertFalse(card.setUnderstanding(UnderstandingLevel.UNKNOWN, now.plusSeconds(5), List.of(1, 2, 4, 8, 16), request));
        assertEquals(4, card.getLeitnerBox());
        assertTrue(card.setUnderstanding(UnderstandingLevel.UNKNOWN, now.plusSeconds(5), List.of(1, 2, 4, 8, 16), UUID.randomUUID()));
        assertEquals(1, card.getLeitnerBox());
        assertEquals(now.plusSeconds(5), card.getNextReviewAt());
    }

    @Test void rejectsMissingUnderstandingValuesWithoutChangingTheCard() {
        Flashcard card = Flashcard.create(UUID.randomUUID(), FlashcardSourceType.VOCABULARY_SENSE,
                UUID.randomUUID(), null, null, "word", "meaning");
        Instant now = Instant.parse("2026-10-26T10:00:00Z");
        List<Integer> intervals = List.of(1, 2, 4, 8, 16);

        InvalidDataException missingLevel = assertThrows(InvalidDataException.class,
                () -> card.setUnderstanding(null, now, intervals, UUID.randomUUID()));
        assertEquals("understanding request is incomplete", missingLevel.getMessage());
        InvalidDataException missingTime = assertThrows(InvalidDataException.class,
                () -> card.setUnderstanding(UnderstandingLevel.MASTERED, null, intervals, UUID.randomUUID()));
        assertEquals("understanding request is incomplete", missingTime.getMessage());
        InvalidDataException missingRequestId = assertThrows(InvalidDataException.class,
                () -> card.setUnderstanding(UnderstandingLevel.MASTERED, now, intervals, null));
        assertEquals("understanding request is incomplete", missingRequestId.getMessage());

        assertEquals(1, card.getLeitnerBox());
        assertNull(card.getNextReviewAt());
        assertNull(card.getLastReviewRequestId());
    }

    @Test void boxSixIsNotALevel() {
        InvalidDataException error = assertThrows(InvalidDataException.class, () -> UnderstandingLevel.fromBox(6));
        assertEquals("Leitner box is invalid", error.getMessage());
    }
}
