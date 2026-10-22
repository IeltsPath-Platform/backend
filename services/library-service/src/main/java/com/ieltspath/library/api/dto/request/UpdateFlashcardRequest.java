package com.ieltspath.library.api.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UpdateFlashcardRequest {
    @NotNull
    private FlashcardSourceType sourceType;
    private UUID vocabularySenseId;
    private UUID sourceReferenceId;
    @Size(max = 2000)
    private String highlightedText;
    @NotBlank
    @Size(max = 4000)
    private String front;
    @NotBlank
    @Size(max = 4000)
    private String back;
    @NotNull
    private LibraryStatus status;
    @Size(max = 300)
    private String imageKey;
    @Size(max = 1000)
    private String exampleSentence;

    private boolean imageKeySpecified = false;

    public UpdateFlashcardRequest(
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
        this.sourceType = sourceType;
        this.vocabularySenseId = vocabularySenseId;
        this.sourceReferenceId = sourceReferenceId;
        this.highlightedText = highlightedText;
        this.front = front;
        this.back = back;
        this.status = status;
        this.imageKey = imageKey;
        this.exampleSentence = exampleSentence;
        this.imageKeySpecified = true;
    }

    public UpdateFlashcardRequest(
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back,
            LibraryStatus status
    ) {
        this(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, status, null, null);
        this.imageKeySpecified = false;
    }

    @JsonSetter(value = "imageKey", nulls = Nulls.SET)
    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
        this.imageKeySpecified = true;
    }

    public boolean isImageKeySpecified() {
        return imageKeySpecified;
    }

    public FlashcardSourceType sourceType() { return sourceType; }
    public UUID vocabularySenseId() { return vocabularySenseId; }
    public UUID sourceReferenceId() { return sourceReferenceId; }
    public String highlightedText() { return highlightedText; }
    public String front() { return front; }
    public String back() { return back; }
    public LibraryStatus status() { return status; }
    public String imageKey() { return imageKey; }
    public String exampleSentence() { return exampleSentence; }
}
