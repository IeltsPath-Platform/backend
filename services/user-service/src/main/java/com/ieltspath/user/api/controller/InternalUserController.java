package com.ieltspath.user.api.controller;

import com.ieltspath.user.api.dto.request.UserDisplayNamesRequest;
import com.ieltspath.user.api.dto.response.UserDisplayNamesResponse;
import com.ieltspath.user.application.result.InternalLearningGoalResult;
import com.ieltspath.user.application.usecase.GetInternalLearningGoalUseCase;
import com.ieltspath.user.application.usecase.GetUserDisplayNamesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Internal user lookups used by other backend services; requests require a verified internal JWT. */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final GetInternalLearningGoalUseCase learningGoals;
    private final GetUserDisplayNamesUseCase displayNames;

    @GetMapping("/{userId}/learning-goal")
    public InternalLearningGoalResult learningGoal(@PathVariable("userId") UUID userId) {
        return learningGoals.execute(userId);
    }

    @PostMapping("/display-names")
    public UserDisplayNamesResponse displayNames(@Valid @RequestBody UserDisplayNamesRequest request) {
        return UserDisplayNamesResponse.from(displayNames.execute(request.ids()));
    }
}
