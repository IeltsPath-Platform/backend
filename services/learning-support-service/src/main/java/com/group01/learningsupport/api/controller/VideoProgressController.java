package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.UpsertVideoProgressRequest;
import com.group01.learningsupport.api.dto.response.PageResponse;
import com.group01.learningsupport.api.dto.response.VideoLearningProgressResponse;
import com.group01.learningsupport.application.command.UpsertVideoProgressCommand;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.usecase.DeleteVideoProgressUseCase;
import com.group01.learningsupport.application.usecase.GetVideoProgressUseCase;
import com.group01.learningsupport.application.usecase.ListVideoProgressUseCase;
import com.group01.learningsupport.application.usecase.UpsertVideoProgressUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning-support/video-progress")
@RequiredArgsConstructor
public class VideoProgressController {
    private final CurrentUserProvider currentUserProvider;
    private final UpsertVideoProgressUseCase upsertVideoProgressUseCase;
    private final GetVideoProgressUseCase getVideoProgressUseCase;
    private final ListVideoProgressUseCase listVideoProgressUseCase;
    private final DeleteVideoProgressUseCase deleteVideoProgressUseCase;

    @PutMapping
    public VideoLearningProgressResponse upsert(@Valid @RequestBody UpsertVideoProgressRequest request) {
        return VideoLearningProgressResponse.from(upsertVideoProgressUseCase.execute(new UpsertVideoProgressCommand(
                currentUserProvider.requireUserId(),
                request.videoId(),
                request.lastPositionMs(),
                request.watchedDurationSeconds(),
                request.progressPercent(),
                request.status(),
                request.startedAt(),
                request.lastWatchedAt(),
                request.completedAt()
        )));
    }

    @GetMapping
    public PageResponse<VideoLearningProgressResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                listVideoProgressUseCase.execute(currentUserProvider.requireUserId(), new PageQuery(page, size)),
                VideoLearningProgressResponse::from
        );
    }

    @GetMapping("/{id}")
    public VideoLearningProgressResponse get(@PathVariable UUID id) {
        return VideoLearningProgressResponse.from(getVideoProgressUseCase.execute(currentUserProvider.requireUserId(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteVideoProgressUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
