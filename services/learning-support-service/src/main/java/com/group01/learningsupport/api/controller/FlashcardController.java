package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.CreateFlashcardRequest;
import com.group01.learningsupport.api.dto.request.UpdateFlashcardRequest;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.application.usecase.CreateFlashcardUseCase;
import com.group01.learningsupport.application.usecase.DeleteFlashcardUseCase;
import com.group01.learningsupport.application.usecase.GetFlashcardUseCase;
import com.group01.learningsupport.application.usecase.ListFlashcardsUseCase;
import com.group01.learningsupport.application.usecase.UpdateFlashcardUseCase;
import com.group01.learningsupport.domain.aggregate.Flashcard;
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
    public Flashcard create(@Valid @RequestBody CreateFlashcardRequest request) {
        return createFlashcardUseCase.execute(
                currentUserProvider.requireUserId(),
                request.sourceType(),
                request.vocabularySenseId(),
                request.sourceReferenceId(),
                request.highlightedText(),
                request.front(),
                request.back()
        );
    }

    @GetMapping
    public PageResult<Flashcard> list(
            @RequestParam(defaultValue = "ACTIVE") LibraryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listFlashcardsUseCase.execute(currentUserProvider.requireUserId(), status, new PageQuery(page, size));
    }

    @GetMapping("/{id}")
    public Flashcard get(@PathVariable UUID id) {
        return getFlashcardUseCase.execute(currentUserProvider.requireUserId(), id);
    }

    @PutMapping("/{id}")
    public Flashcard update(@PathVariable UUID id, @Valid @RequestBody UpdateFlashcardRequest request) {
        return updateFlashcardUseCase.execute(
                currentUserProvider.requireUserId(),
                id,
                request.sourceType(),
                request.vocabularySenseId(),
                request.sourceReferenceId(),
                request.highlightedText(),
                request.front(),
                request.back(),
                request.status()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteFlashcardUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
