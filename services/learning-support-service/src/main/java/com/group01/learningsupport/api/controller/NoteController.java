package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.CreateNoteRequest;
import com.group01.learningsupport.api.dto.request.UpdateNoteRequest;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.application.usecase.CreateNoteUseCase;
import com.group01.learningsupport.application.usecase.DeleteNoteUseCase;
import com.group01.learningsupport.application.usecase.GetNoteUseCase;
import com.group01.learningsupport.application.usecase.ListNotesUseCase;
import com.group01.learningsupport.application.usecase.UpdateNoteUseCase;
import com.group01.learningsupport.domain.aggregate.Note;
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
@RequestMapping("/api/learning-support/notes")
@RequiredArgsConstructor
public class NoteController {
    private final CurrentUserProvider currentUserProvider;
    private final CreateNoteUseCase createNoteUseCase;
    private final UpdateNoteUseCase updateNoteUseCase;
    private final GetNoteUseCase getNoteUseCase;
    private final ListNotesUseCase listNotesUseCase;
    private final DeleteNoteUseCase deleteNoteUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Note create(@Valid @RequestBody CreateNoteRequest request) {
        return createNoteUseCase.execute(currentUserProvider.requireUserId(), request.title(), request.body());
    }

    @GetMapping
    public PageResult<Note> list(
            @RequestParam(defaultValue = "ACTIVE") LibraryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listNotesUseCase.execute(currentUserProvider.requireUserId(), status, new PageQuery(page, size));
    }

    @GetMapping("/{id}")
    public Note get(@PathVariable UUID id) {
        return getNoteUseCase.execute(currentUserProvider.requireUserId(), id);
    }

    @PutMapping("/{id}")
    public Note update(@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest request) {
        return updateNoteUseCase.execute(
                currentUserProvider.requireUserId(), id, request.title(), request.body(), request.status()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteNoteUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
