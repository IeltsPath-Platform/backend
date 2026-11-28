package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.GetPlacementHistoryUseCase;
import com.ieltspath.commonsecurity.authorization.LearnerOnly;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessments/attempts/placement/history")
@LearnerOnly
public class PlacementHistoryController {
    private final CurrentUserProvider currentUser;
    private final GetPlacementHistoryUseCase history;

    public PlacementHistoryController(CurrentUserProvider currentUser, GetPlacementHistoryUseCase history) {
        this.currentUser = currentUser;
        this.history = history;
    }

    @GetMapping
    public GetPlacementHistoryUseCase.Page history(@RequestParam(name = "page", defaultValue = "0") int page,
                                                   @RequestParam(name = "size", defaultValue = "10") int size) {
        return history.execute(currentUser.requireUserId(), page, size);
    }
}
