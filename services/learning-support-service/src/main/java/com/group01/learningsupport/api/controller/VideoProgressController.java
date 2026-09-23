package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.UpsertVideoProgressRequest;
import com.group01.learningsupport.application.command.UpsertVideoProgressCommand;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.application.usecase.DeleteVideoProgressUseCase;
import com.group01.learningsupport.application.usecase.GetVideoProgressUseCase;
import com.group01.learningsupport.application.usecase.ListVideoProgressUseCase;
import com.group01.learningsupport.application.usecase.UpsertVideoProgressUseCase;
import com.group01.learningsupport.domain.aggregate.VideoLearningProgress;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public VideoLearningProgress upsert(@Valid @RequestBody UpsertVideoProgressRequest request) {
        return upsertVideoProgressUseCase.execute(new UpsertVideoProgressCommand(
                currentUserProvider.requireUserId(),
                request.videoId(),
                request.lastPositionMs(),
                request.watchedDurationSeconds(),
                request.progressPercent(),
                request.status(),
                request.startedAt(),
                request.lastWatchedAt(),
                request.completedAt()
        ));
    }

    @GetMapping
    public PageResult<VideoLearningProgress> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listVideoProgressUseCase.execute(currentUserProvider.requireUserId(), new PageQuery(page, size));
    }

    @GetMapping("/{id}")
    public VideoLearningProgress get(@PathVariable UUID id) {
        return getVideoProgressUseCase.execute(currentUserProvider.requireUserId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteVideoProgressUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
