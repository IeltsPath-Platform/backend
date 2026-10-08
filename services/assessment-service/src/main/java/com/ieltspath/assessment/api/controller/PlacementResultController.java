package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.response.PlacementResultResponse;
import com.ieltspath.assessment.application.usecase.GetPlacementResultUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/attempts/{attemptId}/placement-result")
@RequiredArgsConstructor
public class PlacementResultController {

    private final CurrentUserProvider currentUser;
    private final GetPlacementResultUseCase getPlacementResultUseCase;

    @GetMapping
    public PlacementResultResponse get(@PathVariable("attemptId") UUID attemptId) {
        return PlacementResultResponse.from(getPlacementResultUseCase.execute(currentUser.requireUserId(), attemptId));
    }
}
