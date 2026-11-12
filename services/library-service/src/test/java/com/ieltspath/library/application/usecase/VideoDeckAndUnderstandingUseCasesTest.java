package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.AddSegmentLexicalEntryCommand;
import com.ieltspath.library.application.command.AddVideoSegmentCommand;
import com.ieltspath.library.application.command.PublishLearningVideoCommand;
import com.ieltspath.library.config.FlashcardSrsProperties;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.aggregate.SavedVideoSegment;
import com.ieltspath.library.domain.aggregate.VideoLearningProgress;
import com.ieltspath.library.domain.entity.VideoSegment;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.repository.FlashcardDeckItemRepository;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.repository.LearningVideoRepository;
import com.ieltspath.library.domain.repository.SavedVideoSegmentRepository;
import com.ieltspath.library.domain.repository.VideoLearningProgressRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.PublicationStatus;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import com.ieltspath.library.domain.vo.VideoProgressStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoDeckAndUnderstandingUseCasesTest {
    private final UUID user = UUID.randomUUID();
    private final FlashcardRepository cards = mock(FlashcardRepository.class);
    private final FlashcardDeckRepository decks = mock(FlashcardDeckRepository.class);
    private final FlashcardDeckItemRepository items = mock(FlashcardDeckItemRepository.class);
    private final LearningVideoRepository videos = mock(LearningVideoRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);

    private SetUnderstandingLevelUseCase understanding() {
        return new SetUnderstandingLevelUseCase(cards, new FlashcardSrsProperties(), clock);
    }

    private Flashcard senseCard() {
        return Flashcard.create(user, FlashcardSourceType.VOCABULARY_SENSE, UUID.randomUUID(), null, null, "word", "meaning");
    }

    private LearningVideo draftVideo() {
        var video = LearningVideo.create("abc123", "https://www.youtube.com/watch?v=abc123", "Lesson", null, null, 120,
                null, null, null, user);
        when(videos.findById(video.getId())).thenReturn(Optional.of(video));
        when(videos.save(video)).thenReturn(video);
        return video;
    }

    // ---- understanding level ------------------------------------------------------------------------------------

    @Test
    void settingTheLevelOfAnActiveWordCardSavesIt() {
        var card = senseCard();
        when(cards.findForReviewByIdAndUserId(card.getId(), user)).thenReturn(Optional.of(card));
        UUID request = UUID.randomUUID();

        understanding().execute(user, card.getId(), UnderstandingLevel.MASTERED, request);

        assertEquals(4, card.getLeitnerBox());
        assertEquals(clock.instant().plusSeconds(8 * 86400L), card.getNextReviewAt());
        verify(cards).save(card);
    }

    @Test
    void repeatingTheSameRequestDoesNotSaveAgain() {
        var card = senseCard();
        when(cards.findForReviewByIdAndUserId(card.getId(), user)).thenReturn(Optional.of(card));
        UUID request = UUID.randomUUID();
        understanding().execute(user, card.getId(), UnderstandingLevel.MASTERED, request);
        understanding().execute(user, card.getId(), UnderstandingLevel.UNKNOWN, request);
        verify(cards, times(1)).save(card);
        assertEquals(4, card.getLeitnerBox());
    }

    @Test
    void onlyActiveWordCardsOfTheUserCanBeRated() {
        var manual = Flashcard.create(user, FlashcardSourceType.MANUAL, null, null, null, "front", "back");
        var archived = senseCard();
        archived.update(FlashcardSourceType.VOCABULARY_SENSE, archived.getVocabularySenseId(), null, null, "word", "meaning", LibraryStatus.ARCHIVED);
        UUID missing = UUID.randomUUID();
        when(cards.findForReviewByIdAndUserId(manual.getId(), user)).thenReturn(Optional.of(manual));
        when(cards.findForReviewByIdAndUserId(archived.getId(), user)).thenReturn(Optional.of(archived));
        when(cards.findForReviewByIdAndUserId(missing, user)).thenReturn(Optional.empty());
        for (UUID id : new UUID[]{manual.getId(), archived.getId(), missing}) {
            assertThrows(ResourceNotFoundException.class, () -> understanding().execute(user, id, UnderstandingLevel.MASTERED, UUID.randomUUID()));
        }
        verify(cards, never()).save(any());
    }

    // ---- remove from deck --------------------------------------------------------------------------------------

    @Test
    void aCardIsRemovedFromTheUsersOwnDeck() {
        var deck = FlashcardDeck.create(user, "Deck", null);
        var card = senseCard();
        when(decks.findByIdAndUserId(deck.getId(), user)).thenReturn(Optional.of(deck));
        when(cards.findByIdAndUserId(card.getId(), user)).thenReturn(Optional.of(card));

        new RemoveFlashcardFromDeckUseCase(decks, cards, items).execute(user, deck.getId(), card.getId());

        verify(items).delete(deck.getId(), card.getId());
    }

    @Test
    void anotherLearnersDeckOrCardCannotBeChanged() {
        UUID foreignDeck = UUID.randomUUID();
        var deck = FlashcardDeck.create(user, "Deck", null);
        UUID foreignCard = UUID.randomUUID();
        when(decks.findByIdAndUserId(foreignDeck, user)).thenReturn(Optional.empty());
        when(decks.findByIdAndUserId(deck.getId(), user)).thenReturn(Optional.of(deck));
        when(cards.findByIdAndUserId(foreignCard, user)).thenReturn(Optional.empty());
        var useCase = new RemoveFlashcardFromDeckUseCase(decks, cards, items);
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(user, foreignDeck, UUID.randomUUID()));
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(user, deck.getId(), foreignCard));
        verify(items, never()).delete(any(), any());
    }

    // ---- video catalog -----------------------------------------------------------------------------------------

    @Test
    void aSegmentIsAddedToAnExistingVideo() {
        var video = draftVideo();
        var result = new AddVideoSegmentUseCase(videos).execute(
                new AddVideoSegmentCommand(video.getId(), 1, 0, 2500, "She goes to school.", "Cô ấy đi học."));
        assertEquals(1, result.sequenceNo());
        assertEquals(1, video.getSegments().size());
        verify(videos).save(video);
    }

    @Test
    void aSegmentForAVideoThatDoesNotExistIsRefused() {
        UUID missing = UUID.randomUUID();
        when(videos.findById(missing)).thenReturn(Optional.empty());
        assertThrows(VideoNotFoundException.class, () -> new AddVideoSegmentUseCase(videos).execute(
                new AddVideoSegmentCommand(missing, 1, 0, 2500, "text", null)));
        verify(videos, never()).save(any());
    }

    @Test
    void aSegmentWithBadTimingIsNotSaved() {
        var video = draftVideo();
        assertThrows(IllegalArgumentException.class, () -> new AddVideoSegmentUseCase(videos).execute(
                new AddVideoSegmentCommand(video.getId(), 1, 3000, 2000, "text", null)));
        verify(videos, never()).save(any());
    }

    @Test
    void aWordMarkIsAddedToTheRightSegment() {
        var video = draftVideo();
        var segment = VideoSegment.create(video.getId(), 1, 0, 2500, "She goes to school.", null);
        video.addSegment(segment);
        when(videos.findBySegmentId(segment.getId())).thenReturn(Optional.of(video));

        new AddSegmentLexicalEntryUseCase(videos).execute(new AddSegmentLexicalEntryCommand(segment.getId(), null, "school", 12, 18, 0));

        assertEquals(1, segment.getLexicalEntries().size());
        assertEquals("school", segment.getLexicalEntries().get(0).getSurfaceText());
        verify(videos).save(video);
    }

    @Test
    void aWordMarkForAnUnknownSegmentIsRefused() {
        UUID missing = UUID.randomUUID();
        when(videos.findBySegmentId(missing)).thenReturn(Optional.empty());
        var error = assertThrows(VideoNotFoundException.class, () -> new AddSegmentLexicalEntryUseCase(videos)
                .execute(new AddSegmentLexicalEntryCommand(missing, null, "word", 0, 4, 0)));
        assertEquals("Learning video not found with youtube id: Video segment not found: " + missing, error.getMessage());
        verify(videos, never()).save(any());
    }

    @Test
    void publishingAVideoMakesItPublished() {
        var video = draftVideo();
        var result = new PublishLearningVideoUseCase(videos).execute(new PublishLearningVideoCommand(video.getId()));
        assertEquals(PublicationStatus.PUBLISHED, result.status());
        verify(videos).save(video);
    }

    @Test
    void publishingAVideoThatDoesNotExistIsRefused() {
        UUID missing = UUID.randomUUID();
        when(videos.findById(missing)).thenReturn(Optional.empty());
        assertThrows(VideoNotFoundException.class, () -> new PublishLearningVideoUseCase(videos).execute(new PublishLearningVideoCommand(missing)));
        verify(videos, never()).save(any());
    }

    // ---- personal video data -----------------------------------------------------------------------------------

    @Test
    void videoProgressAndSavedSegmentsAreOwnerOnly() {
        var progressRepo = mock(VideoLearningProgressRepository.class);
        var segmentRepo = mock(SavedVideoSegmentRepository.class);
        var progress = VideoLearningProgress.create(user, UUID.randomUUID(), 0, 0, BigDecimal.ONE, VideoProgressStatus.IN_PROGRESS, null, null, null);
        var saved = SavedVideoSegment.create(user, UUID.randomUUID(), UUID.randomUUID(), "text", null);
        when(progressRepo.findByIdAndUserId(progress.getId(), user)).thenReturn(Optional.of(progress));
        when(segmentRepo.findByIdAndUserId(saved.getId(), user)).thenReturn(Optional.of(saved));
        UUID other = UUID.randomUUID();

        assertEquals(progress.getId(), new GetVideoProgressUseCase(progressRepo).execute(user, progress.getId()).id());
        new DeleteVideoProgressUseCase(progressRepo).execute(user, progress.getId());
        new DeleteSavedVideoSegmentUseCase(segmentRepo).execute(user, saved.getId());
        verify(progressRepo).delete(progress);
        verify(segmentRepo).delete(saved);

        assertThrows(ResourceNotFoundException.class, () -> new GetVideoProgressUseCase(progressRepo).execute(other, progress.getId()));
        assertThrows(ResourceNotFoundException.class, () -> new DeleteVideoProgressUseCase(progressRepo).execute(other, progress.getId()));
        assertThrows(ResourceNotFoundException.class, () -> new DeleteSavedVideoSegmentUseCase(segmentRepo).execute(other, saved.getId()));
        verify(progressRepo, times(1)).delete(any());
        verify(segmentRepo, times(1)).delete(any());
    }
}
