package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.exception.InvalidDataException;

import com.ieltspath.library.domain.DomainChecks;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Flashcard {
    private final UUID id;
    private final UUID userId;
    private FlashcardSourceType sourceType;
    private UUID vocabularySenseId;
    private UUID sourceReferenceId;
    private String highlightedText;
    private String front;
    private String back;
    private LibraryStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    @Builder.Default
    private int leitnerBox = 1;
    private Instant nextReviewAt;
    private Instant lastReviewedAt;
    @Builder.Default
    private int reviewCount = 0;
    private UUID lastReviewRequestId;
    private String imageKey;
    private String exampleSentence;

    public static Flashcard create(
            UUID userId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            String imageKey,
            String exampleSentence
    ) {
        Flashcard card = new Flashcard(
                UUID.randomUUID(),
                DomainChecks.userId(userId),
                null,
                null,
                null,
                null,
                null,
                null,
                LibraryStatus.ACTIVE,
                null,
                null,
                1,
                null,
                null,
                0,
                null,
                DomainChecks.optionalTrimmed(imageKey, 300, "imageKey"),
                DomainChecks.optionalTrimmed(exampleSentence, 1000, "exampleSentence")
        );
        card.applySource(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back);
        return card;
    }

    public static Flashcard create(
            UUID userId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back
    ) {
        return create(userId, sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, null, null);
    }

    public boolean update(
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            LibraryStatus status,
            String imageKey,
            String exampleSentence
    ) {
        if (status == null) {
            throw new InvalidDataException("status không hợp lệ");
        }
        applySource(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back);
        this.imageKey = DomainChecks.optionalTrimmed(imageKey, 300, "imageKey");
        this.exampleSentence = DomainChecks.optionalTrimmed(exampleSentence, 1000, "exampleSentence");
        boolean dropLinks = this.status == LibraryStatus.ACTIVE && status != LibraryStatus.ACTIVE;
        this.status = status;
        return dropLinks;
    }

    public boolean update(
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            LibraryStatus status
    ) {
        return update(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, status, this.imageKey, this.exampleSentence);
    }

    /** Applies one Leitner review; replaying the most recent request ID leaves the card unchanged. */
    public boolean review(boolean correct, UUID requestId, Instant now, List<Integer> intervalsDays) {
        if (requestId == null || now == null) {
            throw new InvalidDataException("review request is incomplete");
        }
        if (requestId.equals(lastReviewRequestId)) {
            return false;
        }
        if (leitnerBox < 1 || leitnerBox > 5 || intervalsDays == null || intervalsDays.size() != 5
                || intervalsDays.stream().anyMatch(days -> days == null || days < 1)) {
            throw new InvalidDataException("Leitner schedule is invalid");
        }

        leitnerBox = correct ? Math.min(leitnerBox + 1, 5) : 1;
        Instant reviewedAt = now.truncatedTo(ChronoUnit.MICROS);
        lastReviewedAt = reviewedAt;
        nextReviewAt = reviewedAt.plus(Duration.ofDays(intervalsDays.get(leitnerBox - 1)));
        reviewCount = Math.addExact(reviewCount, 1);
        lastReviewRequestId = requestId;
        return true;
    }

    public boolean setUnderstanding(UnderstandingLevel level, Instant now, List<Integer> intervalsDays, UUID requestId) {
        if (level == null || requestId == null || now == null) {
            throw new InvalidDataException("understanding request is incomplete");
        }
        if (requestId.equals(lastReviewRequestId)) return false;
        if (intervalsDays == null || intervalsDays.size() != 5
                || intervalsDays.stream().anyMatch(days -> days == null || days < 1)) {
            throw new InvalidDataException("Leitner schedule is invalid");
        }
        leitnerBox = level.targetBox();
        Instant changedAt = now.truncatedTo(ChronoUnit.MICROS);
        nextReviewAt = level == UnderstandingLevel.UNKNOWN
                ? changedAt : changedAt.plus(Duration.ofDays(intervalsDays.get(leitnerBox - 1)));
        lastReviewRequestId = requestId;
        return true;
    }

    private void applySource(
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back
    ) {
        if (sourceType == null) {
            throw new InvalidDataException("sourceType không hợp lệ");
        }
        String nextFront = DomainChecks.required(front, DomainChecks.FRONT_MAX, "front");
        String nextBack = DomainChecks.required(back, DomainChecks.FRONT_MAX, "back");
        switch (sourceType) {
            case MANUAL -> {
                if (vocabularySenseId != null || sourceReferenceId != null || highlightedText != null) {
                    throw new InvalidDataException("flashcard manual không hợp lệ");
                }
                this.highlightedText = null;
                this.vocabularySenseId = null;
                this.sourceReferenceId = null;
            }
            case VOCABULARY_SENSE -> {
                if (vocabularySenseId == null) {
                    throw new InvalidDataException("vocabularySenseId không hợp lệ");
                }
                this.vocabularySenseId = vocabularySenseId;
                this.sourceReferenceId = sourceReferenceId;
                this.highlightedText = DomainChecks.optional(highlightedText, DomainChecks.HIGHLIGHT_MAX, "highlightedText");
            }
            case HIGHLIGHT -> {
                this.highlightedText = DomainChecks.required(highlightedText, DomainChecks.HIGHLIGHT_MAX, "highlightedText");
                this.vocabularySenseId = vocabularySenseId;
                this.sourceReferenceId = sourceReferenceId;
            }
            case PRACTICE_QUESTION -> {
                // Points back at the AI Learning practice question the learner answered.
                if (sourceReferenceId == null || vocabularySenseId != null || highlightedText != null) {
                    throw new InvalidDataException("flashcard practice question không hợp lệ");
                }
                this.highlightedText = null;
                this.vocabularySenseId = null;
                this.sourceReferenceId = sourceReferenceId;
            }
        }
        this.sourceType = sourceType;
        this.front = nextFront;
        this.back = nextBack;
    }
}
