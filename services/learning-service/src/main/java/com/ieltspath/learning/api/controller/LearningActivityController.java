package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.CreateLearningActivityRequest;
import com.ieltspath.learning.api.dto.response.LearningActivityResponse;
import com.ieltspath.learning.api.dto.response.PageResponse;
import com.ieltspath.learning.application.command.CreateLearningActivityCommand;
import com.ieltspath.learning.application.query.PageQuery;
import com.ieltspath.learning.application.usecase.CreateLearningActivityUseCase;
import com.ieltspath.learning.application.usecase.DeleteLearningActivityUseCase;
import com.ieltspath.learning.application.usecase.ListLearningActivitiesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning/activities")
@RequiredArgsConstructor
public class LearningActivityController {
    private final CurrentUserProvider currentUserProvider;
    private final CreateLearningActivityUseCase createLearningActivityUseCase;
    private final ListLearningActivitiesUseCase listLearningActivitiesUseCase;
    private final DeleteLearningActivityUseCase deleteLearningActivityUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningActivityResponse create(@Valid @RequestBody CreateLearningActivityRequest request) {
        return LearningActivityResponse.from(createLearningActivityUseCase.execute(new CreateLearningActivityCommand(
                currentUserProvider.requireUserId(),
                request.activityType(),
                request.sourceType(),
                request.sourceId(),
                request.occurredAt(),
                request.durationSeconds()
        )));
    }

    @GetMapping
    public PageResponse<LearningActivityResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                listLearningActivitiesUseCase.execute(currentUserProvider.requireUserId(), new PageQuery(page, size)),
                LearningActivityResponse::from
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteLearningActivityUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
