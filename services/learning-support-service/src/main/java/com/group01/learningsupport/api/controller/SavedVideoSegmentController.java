package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.CreateSavedVideoSegmentRequest;
import com.group01.learningsupport.api.dto.response.PageResponse;
import com.group01.learningsupport.api.dto.response.SavedVideoSegmentResponse;
import com.group01.learningsupport.application.command.CreateSavedVideoSegmentCommand;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.usecase.CreateSavedVideoSegmentUseCase;
import com.group01.learningsupport.application.usecase.DeleteSavedVideoSegmentUseCase;
import com.group01.learningsupport.application.usecase.ListSavedVideoSegmentsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning-support/saved-segments")
@RequiredArgsConstructor
public class SavedVideoSegmentController {
    private final CurrentUserProvider currentUserProvider;
    private final CreateSavedVideoSegmentUseCase createSavedVideoSegmentUseCase;
    private final ListSavedVideoSegmentsUseCase listSavedVideoSegmentsUseCase;
    private final DeleteSavedVideoSegmentUseCase deleteSavedVideoSegmentUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedVideoSegmentResponse create(@Valid @RequestBody CreateSavedVideoSegmentRequest request) {
        return SavedVideoSegmentResponse.from(createSavedVideoSegmentUseCase.execute(new CreateSavedVideoSegmentCommand(
                currentUserProvider.requireUserId(),
                request.videoId(),
                request.segmentId(),
                request.transcriptSnapshot(),
                request.note()
        )));
    }

    @GetMapping
    public PageResponse<SavedVideoSegmentResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                listSavedVideoSegmentsUseCase.execute(currentUserProvider.requireUserId(), new PageQuery(page, size)),
                SavedVideoSegmentResponse::from
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteSavedVideoSegmentUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
