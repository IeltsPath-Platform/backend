package com.group01.learningsupport.domain.aggregate;

import com.group01.learningsupport.domain.exception.InvalidDataException;

import com.group01.learningsupport.domain.DomainChecks;
import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
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

    public static Flashcard create(
            UUID userId,
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back
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
                null
        );
        card.applySource(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back);
        return card;
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
        if (this.status == LibraryStatus.DELETED) {
            throw new InvalidDataException("Flashcard has been deleted");
        }
        if (status == null) {
            throw new InvalidDataException("status không hợp lệ");
        }
        if (status == LibraryStatus.DELETED) {
            throw new InvalidDataException("Use the delete operation to delete a flashcard");
        }
        applySource(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back);
        boolean dropLinks = this.status == LibraryStatus.ACTIVE && status != LibraryStatus.ACTIVE;
        this.status = status;
        return dropLinks;
    }

    public void delete() {
        if (status == LibraryStatus.DELETED) {
            throw new InvalidDataException("Flashcard has been deleted");
        }
        status = LibraryStatus.DELETED;
    }

    public void restore() {
        if (status != LibraryStatus.DELETED) {
            throw new InvalidDataException("Only deleted flashcards can be restored");
        }
        status = LibraryStatus.ACTIVE;
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
