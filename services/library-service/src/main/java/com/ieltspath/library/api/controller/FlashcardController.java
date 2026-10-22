package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonstorage.UploadRejectedException;
import com.ieltspath.library.api.dto.request.CreateFlashcardRequest;
import com.ieltspath.library.api.dto.request.ReviewFlashcardRequest;
import com.ieltspath.library.api.dto.request.UpdateFlashcardRequest;
import com.ieltspath.library.api.dto.response.FlashcardImageResponse;
import com.ieltspath.library.api.dto.response.FlashcardResponse;
import com.ieltspath.library.api.dto.response.PageResponse;
import com.ieltspath.library.application.query.DueFlashcardsQuery;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.SavedFlashcard;
import com.ieltspath.library.application.result.StoredImageResult;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning-support/flashcards")
@RequiredArgsConstructor
public class FlashcardController {
    private final CurrentUserProvider currentUserProvider;
    private final CreateFlashcardUseCase createFlashcardUseCase;
    private final UpdateFlashcardUseCase updateFlashcardUseCase;
    private final GetFlashcardUseCase getFlashcardUseCase;
    private final ListFlashcardsUseCase listFlashcardsUseCase;
    private final DeleteFlashcardUseCase deleteFlashcardUseCase;
    private final SavePracticeQuestionFlashcardUseCase savePracticeQuestionFlashcardUseCase;
    private final GetDueFlashcardsUseCase getDueFlashcardsUseCase;
    private final ReviewFlashcardUseCase reviewFlashcardUseCase;
    private final UploadFlashcardImageUseCase uploadFlashcardImageUseCase;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardImageResponse uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY);
        }
        UUID userId = currentUserProvider.requireUserId();
        StoredImageResult result = uploadFlashcardImageUseCase.execute(userId, file.getContentType(), bytes);
        return FlashcardImageResponse.from(result);
    }

    @PostMapping
    public ResponseEntity<FlashcardResponse> create(@Valid @RequestBody CreateFlashcardRequest request) {
        UUID userId = currentUserProvider.requireUserId();
        if (request.sourceType() == FlashcardSourceType.PRACTICE_QUESTION) {
            // Saving the same practice question again returns the existing card with 200 instead of a duplicate.
            if (request.sourceReferenceId() == null || request.vocabularySenseId() != null
                    || request.highlightedText() != null) {
                throw new InvalidDataException("flashcard practice question không hợp lệ");
            }
            SavedFlashcard saved = savePracticeQuestionFlashcardUseCase.execute(
                    userId, request.sourceReferenceId(), request.front(), request.back());
            return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
                    .body(FlashcardResponse.from(saved.flashcard()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(FlashcardResponse.from(createFlashcardUseCase.execute(
                userId,
                request.sourceType(),
                request.vocabularySenseId(),
                request.sourceReferenceId(),
                request.highlightedText(),
                request.front(),
                request.back(),
                request.imageKey(),
                request.exampleSentence()
        )));
    }

    @GetMapping
    public PageResponse<FlashcardResponse> list(
            @RequestParam(defaultValue = "ACTIVE") LibraryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                listFlashcardsUseCase.execute(currentUserProvider.requireUserId(), status, new PageQuery(page, size)),
                FlashcardResponse::from
        );
    }

    @GetMapping("/due")
    public PageResponse<FlashcardResponse> due(
            @RequestParam(name = "deckId", required = false) UUID deckId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return PageResponse.from(getDueFlashcardsUseCase.execute(currentUserProvider.requireUserId(),
                new DueFlashcardsQuery(deckId, page, limit)), FlashcardResponse::from);
    }

    @PostMapping("/{id}/reviews")
    public FlashcardResponse review(@PathVariable("id") UUID id,
                                    @Valid @RequestBody ReviewFlashcardRequest request) {
        return FlashcardResponse.from(reviewFlashcardUseCase.execute(currentUserProvider.requireUserId(), id,
                request.correct(), request.requestId()));
    }

    @GetMapping("/{id}")
    public FlashcardResponse get(@PathVariable UUID id) {
        return FlashcardResponse.from(getFlashcardUseCase.execute(currentUserProvider.requireUserId(), id));
    }

    @PutMapping("/{id}")
    public FlashcardResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateFlashcardRequest request) {
        return FlashcardResponse.from(updateFlashcardUseCase.execute(
                currentUserProvider.requireUserId(),
                id,
                request.sourceType(),
                request.vocabularySenseId(),
                request.sourceReferenceId(),
                request.highlightedText(),
                request.front(),
                request.back(),
                request.status(),
                request.isImageKeySpecified(),
                request.imageKey(),
                request.exampleSentence()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteFlashcardUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
