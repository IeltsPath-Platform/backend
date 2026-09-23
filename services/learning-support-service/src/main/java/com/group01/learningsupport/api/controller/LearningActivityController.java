package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.CreateLearningActivityRequest;
import com.group01.learningsupport.application.command.CreateLearningActivityCommand;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.application.usecase.CreateLearningActivityUseCase;
import com.group01.learningsupport.application.usecase.DeleteLearningActivityUseCase;
import com.group01.learningsupport.application.usecase.ListLearningActivitiesUseCase;
import com.group01.learningsupport.domain.aggregate.LearningActivity;
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
@RequestMapping("/api/learning-support/activities")
@RequiredArgsConstructor
public class LearningActivityController {
    private final CurrentUserProvider currentUserProvider;
    private final CreateLearningActivityUseCase createLearningActivityUseCase;
    private final ListLearningActivitiesUseCase listLearningActivitiesUseCase;
    private final DeleteLearningActivityUseCase deleteLearningActivityUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningActivity create(@Valid @RequestBody CreateLearningActivityRequest request) {
        return createLearningActivityUseCase.execute(new CreateLearningActivityCommand(
                currentUserProvider.requireUserId(),
                request.activityType(),
                request.sourceType(),
                request.sourceId(),
                request.occurredAt(),
                request.durationSeconds()
        ));
    }

    @GetMapping
    public PageResult<LearningActivity> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listLearningActivitiesUseCase.execute(currentUserProvider.requireUserId(), new PageQuery(page, size));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteLearningActivityUseCase.execute(currentUserProvider.requireUserId(), id);
    }
}
