package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.entity.VideoSegment;
import com.ieltspath.library.domain.entity.VideoSegmentLexicalEntry;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.VideoProgressStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoDeckAndNoteRulesTest {
    private final UUID user = UUID.randomUUID();
    private final UUID video = UUID.randomUUID();

    private VideoLearningProgress progress(BigDecimal percent) {
        return VideoLearningProgress.create(user, video, 0, 0, percent, VideoProgressStatus.IN_PROGRESS, null, null, null);
    }

    @Test
    void progressFromZeroToOneHundredPercentIsAccepted() {
        assertEquals(BigDecimal.ZERO, progress(BigDecimal.ZERO).getProgressPercent());
        assertEquals(new BigDecimal("100"), progress(new BigDecimal("100")).getProgressPercent());
    }

    @Test
    void progressOutsideZeroToOneHundredPercentIsRejected() {
        for (String bad : new String[]{"-0.01", "100.01"}) {
            var error = assertThrows(InvalidDataException.class, () -> progress(new BigDecimal(bad)));
            assertEquals("progressPercent không hợp lệ", error.getMessage());
        }
        assertThrows(InvalidDataException.class, () -> progress(null));
    }

    @Test
    void progressNeedsAVideoAStatusAndNonNegativeCounters() {
        var noVideo = assertThrows(InvalidDataException.class, () -> VideoLearningProgress.create(
                user, null, 0, 0, BigDecimal.TEN, VideoProgressStatus.IN_PROGRESS, null, null, null));
        assertEquals("video progress không hợp lệ", noVideo.getMessage());
        assertThrows(InvalidDataException.class, () -> VideoLearningProgress.create(
                user, video, 0, 0, BigDecimal.TEN, null, null, null, null));
        var negative = assertThrows(InvalidDataException.class, () -> VideoLearningProgress.create(
                user, video, -1, 0, BigDecimal.TEN, VideoProgressStatus.IN_PROGRESS, null, null, null));
        assertEquals("lastPositionMs không hợp lệ", negative.getMessage());
    }

    @Test
    void replacingProgressNeedsAStatusAndKeepsTheOldValuesOtherwise() {
        var saved = progress(BigDecimal.TEN);
        var error = assertThrows(InvalidDataException.class,
                () -> saved.replace(5000, 5, new BigDecimal("50"), null, null, null, null));
        assertEquals("status không hợp lệ", error.getMessage());
        assertEquals(BigDecimal.TEN, saved.getProgressPercent());
        Instant done = Instant.parse("2026-11-01T00:00:00Z");
        saved.replace(60000, 60, new BigDecimal("100"), VideoProgressStatus.COMPLETED, null, done, done);
        assertEquals(VideoProgressStatus.COMPLETED, saved.getStatus());
        assertEquals(60000, saved.getLastPositionMs());
    }

    @Test
    void aSavedSegmentNeedsTheVideoTheSegmentAndATranscript() {
        var saved = SavedVideoSegment.create(user, video, UUID.randomUUID(), "She goes to school.", null);
        assertEquals("She goes to school.", saved.getTranscriptSnapshot());
        assertNull(saved.getNote());
        var missing = assertThrows(InvalidDataException.class, () -> SavedVideoSegment.create(user, video, null, "text", null));
        assertEquals("saved segment không hợp lệ", missing.getMessage());
        var blank = assertThrows(InvalidDataException.class, () -> SavedVideoSegment.create(user, video, UUID.randomUUID(), " ", null));
        assertEquals("transcriptSnapshot không hợp lệ", blank.getMessage());
    }

    @Test
    void aSegmentStartsAtSequenceOneAndEndsAfterItStarts() {
        assertEquals(1, VideoSegment.create(video, 1, 0, 1, "text", null).getSequenceNo());
        var zero = assertThrows(IllegalArgumentException.class, () -> VideoSegment.create(video, 0, 0, 1000, "text", null));
        assertEquals("sequenceNo must be greater than 0", zero.getMessage());
        var negativeStart = assertThrows(IllegalArgumentException.class, () -> VideoSegment.create(video, 1, -1, 1000, "text", null));
        assertEquals("Invalid segment timing: startMs=-1, endMs=1000", negativeStart.getMessage());
        assertThrows(IllegalArgumentException.class, () -> VideoSegment.create(video, 1, 1000, 1000, "text", null));
    }

    @Test
    void aWordMarkInASegmentNeedsValidCharacterBounds() {
        UUID segment = UUID.randomUUID();
        var entry = VideoSegmentLexicalEntry.create(segment, null, "school", 12, 18, 0);
        assertEquals(12, entry.getStartChar());
        for (int[] bounds : new int[][]{{-1, 3}, {5, 5}, {6, 2}}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> VideoSegmentLexicalEntry.create(segment, null, "word", bounds[0], bounds[1], 0));
            assertEquals("Invalid startChar/endChar bounds: " + bounds[0] + ", " + bounds[1], error.getMessage());
        }
    }

    @Test
    void aDeckNeedsANameOfAtMost150Characters() {
        assertEquals("n".repeat(150), FlashcardDeck.create(user, "n".repeat(150), null).getName());
        assertEquals("name không hợp lệ",
                assertThrows(InvalidDataException.class, () -> FlashcardDeck.create(user, "n".repeat(151), null)).getMessage());
        assertThrows(InvalidDataException.class, () -> FlashcardDeck.create(user, " ", null));
    }

    @Test
    void updatingADeckNeedsAStatusAndSignalsWhenItLeavesActive() {
        var deck = FlashcardDeck.create(user, "Deck", null);
        var error = assertThrows(InvalidDataException.class, () -> deck.update("Deck", null, null));
        assertEquals("status không hợp lệ", error.getMessage());
        assertFalse(deck.update("Renamed", null, LibraryStatus.ACTIVE));
        assertTrue(deck.update("Renamed", null, LibraryStatus.ARCHIVED));
        assertEquals("Renamed", deck.getName());
    }

    @Test
    void aNoteTitleHasAtMost255CharactersAndABodyAtMost20000() {
        var note = Note.create(user, "t".repeat(255), "b".repeat(20000));
        assertEquals(255, note.getTitle().length());
        assertEquals("title không hợp lệ",
                assertThrows(InvalidDataException.class, () -> Note.create(user, "t".repeat(256), "body")).getMessage());
        assertEquals("body không hợp lệ",
                assertThrows(InvalidDataException.class, () -> Note.create(user, "title", "b".repeat(20001))).getMessage());
    }
}
