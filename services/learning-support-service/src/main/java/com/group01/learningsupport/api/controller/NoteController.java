package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.CreateNoteRequest;
import com.group01.learningsupport.api.dto.request.UpdateNoteRequest;
import com.group01.learningsupport.api.dto.response.NoteResponse;
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
    public NoteResponse create(@Valid @RequestBody CreateNoteRequest request) {
        return NoteResponse.from(createNoteUseCase.execute(
                currentUserProvider.requireUserId(), request.title(), request.body()
        ));
    }

    @GetMapping
    public PageResponse<NoteResponse> list(
            @RequestParam(defaultValue = "ACTIVE") LibraryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                listNotesUseCase.execute(currentUserProvider.requireUserId(), status, new PageQuery(page, size)),
                NoteResponse::from
        );
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable UUID id) {
        return NoteResponse.from(getNoteUseCase.execute(currentUserProvider.requireUserId(), id));
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest request) {
        return NoteResponse.from(updateNoteUseCase.execute(
                currentUserProvider.requireUserId(), id, request.title(), request.body(), request.status()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteNoteUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
