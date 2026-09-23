package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.AddDeckItemRequest;
import com.group01.learningsupport.api.dto.request.CreateDeckRequest;
import com.group01.learningsupport.api.dto.request.UpdateDeckRequest;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.application.usecase.AddFlashcardToDeckUseCase;
import com.group01.learningsupport.application.usecase.CreateFlashcardDeckUseCase;
import com.group01.learningsupport.application.usecase.DeleteFlashcardDeckUseCase;
import com.group01.learningsupport.application.usecase.GetFlashcardDeckUseCase;
import com.group01.learningsupport.application.usecase.ListDeckItemsUseCase;
import com.group01.learningsupport.application.usecase.ListFlashcardDecksUseCase;
import com.group01.learningsupport.application.usecase.RemoveFlashcardFromDeckUseCase;
import com.group01.learningsupport.application.usecase.UpdateFlashcardDeckUseCase;
import com.group01.learningsupport.domain.aggregate.DeckItem;
import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public FlashcardDeck create(@Valid @RequestBody CreateDeckRequest request) {
        return createFlashcardDeckUseCase.execute(currentUserProvider.requireUserId(), request.name(), request.description());
    }

    @GetMapping
    public PageResult<FlashcardDeck> list(
            @RequestParam(defaultValue = "ACTIVE") LibraryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listFlashcardDecksUseCase.execute(currentUserProvider.requireUserId(), status, new PageQuery(page, size));
    }

    @GetMapping("/{id}")
    public FlashcardDeck get(@PathVariable UUID id) {
        return getFlashcardDeckUseCase.execute(currentUserProvider.requireUserId(), id);
    }

    @PutMapping("/{id}")
    public FlashcardDeck update(@PathVariable UUID id, @Valid @RequestBody UpdateDeckRequest request) {
        return updateFlashcardDeckUseCase.execute(
                currentUserProvider.requireUserId(), id, request.name(), request.description(), request.status()
        );
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
    public PageResult<DeckItem> listItems(
            @PathVariable UUID deckId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listDeckItemsUseCase.execute(currentUserProvider.requireUserId(), deckId, new PageQuery(page, size));
    }

    @DeleteMapping("/{deckId}/items/{flashcardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID deckId, @PathVariable UUID flashcardId) {
        removeFlashcardFromDeckUseCase.execute(currentUserProvider.requireUserId(), deckId, flashcardId);
    }
}
