package com.group01.library.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.library.api.dto.request.CreateFlashcardRequest;
import com.group01.library.api.dto.request.UpdateFlashcardRequest;
import com.group01.library.api.dto.response.FlashcardResponse;
import com.group01.library.api.dto.response.PageResponse;
import com.group01.library.application.query.PageQuery;
import com.group01.library.application.result.SavedFlashcard;
import com.group01.library.application.usecase.*;
import com.group01.library.domain.exception.InvalidDataException;
import com.group01.library.domain.vo.FlashcardSourceType;
import com.group01.library.domain.vo.LibraryStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                request.back()
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
                request.status()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteFlashcardUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
