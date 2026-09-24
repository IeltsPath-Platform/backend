package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.CreateFlashcardRequest;
import com.group01.learningsupport.api.dto.request.UpdateFlashcardRequest;
import com.group01.learningsupport.api.dto.response.FlashcardResponse;
import com.group01.learningsupport.api.dto.response.PageResponse;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.usecase.*;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardResponse create(@Valid @RequestBody CreateFlashcardRequest request) {
        return FlashcardResponse.from(createFlashcardUseCase.execute(
                currentUserProvider.requireUserId(),
                request.sourceType(),
                request.vocabularySenseId(),
                request.sourceReferenceId(),
                request.highlightedText(),
                request.front(),
                request.back()
        ));
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
