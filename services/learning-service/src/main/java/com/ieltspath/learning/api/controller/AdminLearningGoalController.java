package com.ieltspath.learning.api.controller;

import com.ieltspath.learning.api.dto.request.ChangeLearningGoalStatusRequest;
import com.ieltspath.learning.api.dto.response.LearningGoalResponse;
import com.ieltspath.learning.application.command.ChangeLearningGoalStatusCommand;
import com.ieltspath.learning.application.result.LearningGoalResult;
import com.ieltspath.learning.application.usecase.ChangeLearningGoalStatusUseCase;
import com.ieltspath.learning.application.usecase.GetLearningGoalsByUserIdUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Staff view of a learner's goals: the same data the learner manages through {@code /api/learning/goals}. */
@RestController
@RequestMapping("/api/learning/admin/users/{id}/goals")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLearningGoalController {
    private final GetLearningGoalsByUserIdUseCase goals;
    private final ChangeLearningGoalStatusUseCase changeStatus;

    @GetMapping
    public List<LearningGoalResponse> list(@PathVariable("id") UUID id) {
        return goals.execute(id).stream().map(AdminLearningGoalController::toResponse).toList();
    }

    @PutMapping("/{goalId}/status")
    public LearningGoalResponse updateStatus(@PathVariable("id") UUID id, @PathVariable("goalId") UUID goalId,
                                             @Valid @RequestBody ChangeLearningGoalStatusRequest request) {
        // No acting learner: staff may change any goal, so the use case locks the goal's owner instead.
        return toResponse(changeStatus.execute(new ChangeLearningGoalStatusCommand(goalId, null, request.status())));
    }

    private static LearningGoalResponse toResponse(LearningGoalResult result) {
        return new LearningGoalResponse(result.id(), result.userId(), result.targetBand(), result.examDate(),
                result.availableMinutesPerDay(), result.status(), result.startedAt(), result.endedAt(),
                result.createdAt(), result.updatedAt());
    }
}
