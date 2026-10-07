package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.api.dto.request.AddDeckItemRequest;
import com.ieltspath.library.api.dto.request.CreateDeckRequest;
import com.ieltspath.library.api.dto.request.UpdateDeckRequest;
import com.ieltspath.library.api.dto.response.DeckItemResponse;
import com.ieltspath.library.api.dto.response.FlashcardDeckResponse;
import com.ieltspath.library.api.dto.response.PageResponse;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.domain.vo.LibraryStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning-support/decks")
@RequiredArgsConstructor
public class FlashcardDeckController {
    private final CurrentUserProvider currentUserProvider;
    private final CreateFlashcardDeckUseCase createFlashcardDeckUseCase;
    private final UpdateFlashcardDeckUseCase updateFlashcardDeckUseCase;
    private final GetFlashcardDeckUseCase getFlashcardDeckUseCase;
    private final ListFlashcardDecksUseCase listFlashcardDecksUseCase;
    private final DeleteFlashcardDeckUseCase deleteFlashcardDeckUseCase;
    private final AddFlashcardToDeckUseCase addFlashcardToDeckUseCase;
    private final ListDeckItemsUseCase listDeckItemsUseCase;
    private final RemoveFlashcardFromDeckUseCase removeFlashcardFromDeckUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardDeckResponse create(@Valid @RequestBody CreateDeckRequest request) {
        return FlashcardDeckResponse.from(createFlashcardDeckUseCase.execute(
                currentUserProvider.requireUserId(), request.name(), request.description()
        ));
    }

    @GetMapping
    public PageResponse<FlashcardDeckResponse> list(
            @RequestParam(defaultValue = "ACTIVE") LibraryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                listFlashcardDecksUseCase.execute(currentUserProvider.requireUserId(), status, new PageQuery(page, size)),
                FlashcardDeckResponse::from
        );
    }

    @GetMapping("/{id}")
    public FlashcardDeckResponse get(@PathVariable UUID id) {
        return FlashcardDeckResponse.from(getFlashcardDeckUseCase.execute(currentUserProvider.requireUserId(), id));
    }

    @PutMapping("/{id}")
    public FlashcardDeckResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDeckRequest request) {
        return FlashcardDeckResponse.from(updateFlashcardDeckUseCase.execute(
                currentUserProvider.requireUserId(), id, request.name(), request.description(), request.status()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteFlashcardDeckUseCase.execute(currentUserProvider.requireUserId(), id);
    }

    @PostMapping("/{deckId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public void addItem(@PathVariable UUID deckId, @Valid @RequestBody AddDeckItemRequest request) {
        addFlashcardToDeckUseCase.execute(
                currentUserProvider.requireUserId(), deckId, request.flashcardId(), request.sortOrder()
        );
    }

    @GetMapping("/{deckId}/items")
    public PageResponse<DeckItemResponse> listItems(
            @PathVariable UUID deckId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                listDeckItemsUseCase.execute(currentUserProvider.requireUserId(), deckId, new PageQuery(page, size)),
                DeckItemResponse::from
        );
    }

    @DeleteMapping("/{deckId}/items/{flashcardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID deckId, @PathVariable UUID flashcardId) {
        removeFlashcardFromDeckUseCase.execute(currentUserProvider.requireUserId(), deckId, flashcardId);
    }
}
