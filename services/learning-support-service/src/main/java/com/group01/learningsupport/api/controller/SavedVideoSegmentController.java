package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.CreateSavedVideoSegmentRequest;
import com.group01.learningsupport.application.command.CreateSavedVideoSegmentCommand;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.application.usecase.CreateSavedVideoSegmentUseCase;
import com.group01.learningsupport.application.usecase.DeleteSavedVideoSegmentUseCase;
import com.group01.learningsupport.application.usecase.ListSavedVideoSegmentsUseCase;
import com.group01.learningsupport.domain.aggregate.SavedVideoSegment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public SavedVideoSegment create(@Valid @RequestBody CreateSavedVideoSegmentRequest request) {
        return createSavedVideoSegmentUseCase.execute(new CreateSavedVideoSegmentCommand(
                currentUserProvider.requireUserId(),
                request.videoId(),
                request.segmentId(),
                request.transcriptSnapshot(),
                request.note()
        ));
    }

    @GetMapping
    public PageResult<SavedVideoSegment> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listSavedVideoSegmentsUseCase.execute(currentUserProvider.requireUserId(), new PageQuery(page, size));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteSavedVideoSegmentUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
