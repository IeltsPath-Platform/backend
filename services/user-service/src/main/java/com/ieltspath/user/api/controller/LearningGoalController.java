package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.ChangeLearningGoalStatusRequest;
import com.ieltspath.user.api.dto.request.CreateLearningGoalRequest;
import com.ieltspath.user.api.dto.response.LearningGoalResponse;
import com.ieltspath.user.application.command.ChangeLearningGoalStatusCommand;
import com.ieltspath.user.application.command.CreateLearningGoalCommand;
import com.ieltspath.user.application.result.LearningGoalResult;
import com.ieltspath.user.application.usecase.ChangeLearningGoalStatusUseCase;
import com.ieltspath.user.application.usecase.CreateLearningGoalUseCase;
import com.ieltspath.user.application.usecase.GetActiveLearningGoalUseCase;
import com.ieltspath.user.application.usecase.GetLearningGoalsByUserIdUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class LearningGoalController {

    private final GetActiveLearningGoalUseCase getActiveLearningGoalUseCase;
    private final GetLearningGoalsByUserIdUseCase getLearningGoalsByUserIdUseCase;
    private final CreateLearningGoalUseCase createLearningGoalUseCase;
    private final ChangeLearningGoalStatusUseCase changeLearningGoalStatusUseCase;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me/learning-goals/active")
    public LearningGoalResponse getMyActiveGoal() {
        UUID userId = currentUserProvider.requireUserId();
        return toResponse(getActiveLearningGoalUseCase.execute(userId));
    }

    @GetMapping("/me/learning-goals")
    public List<LearningGoalResponse> getMyLearningGoals() {
        UUID userId = currentUserProvider.requireUserId();
        return getLearningGoalsByUserIdUseCase.execute(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/me/learning-goals")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningGoalResponse createMyLearningGoal(@Valid @RequestBody CreateLearningGoalRequest request) {
        UUID userId = currentUserProvider.requireUserId();
        return toResponse(createLearningGoalUseCase.execute(new CreateLearningGoalCommand(
                userId,
                request.targetBand(),
                request.examDate(),
                request.availableMinutesPerDay()
        )));
    }

    @PutMapping("/me/learning-goals/{goalId}/status")
    public LearningGoalResponse updateMyGoalStatus(
            @PathVariable("goalId") UUID goalId,
            @Valid @RequestBody ChangeLearningGoalStatusRequest request
    ) {
        UUID userId = currentUserProvider.requireUserId();
        return toResponse(changeLearningGoalStatusUseCase.execute(new ChangeLearningGoalStatusCommand(
                goalId,
                userId,
                request.status()
        )));
    }

    private LearningGoalResponse toResponse(LearningGoalResult result) {
        return new LearningGoalResponse(
                result.id(),
                result.userId(),
                result.targetBand(),
                result.examDate(),
                result.availableMinutesPerDay(),
                result.status(),
                result.startedAt(),
                result.endedAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
