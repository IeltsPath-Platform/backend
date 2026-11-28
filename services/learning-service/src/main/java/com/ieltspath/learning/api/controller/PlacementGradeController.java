package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.PlacementGradesResponse;
import com.ieltspath.learning.application.usecase.GetPlacementGradesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The detail behind the Writing and Speaking bands of the learner's placement report. */
@RestController
@RequestMapping("/api/learning/placement-grades")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class PlacementGradeController {
    private final CurrentUserProvider currentUser;
    private final GetPlacementGradesUseCase grades;

    @GetMapping("/{attemptId}")
    public PlacementGradesResponse get(@PathVariable UUID attemptId) {
        return PlacementGradesResponse.from(grades.execute(currentUser.requireUserId(), attemptId));
    }
}
