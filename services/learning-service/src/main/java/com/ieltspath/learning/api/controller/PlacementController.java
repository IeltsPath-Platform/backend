package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.PlacementAssignmentRequest;
import com.ieltspath.learning.api.dto.response.PlacementAssignmentResponse;
import com.ieltspath.learning.api.dto.response.PlacementTestOptionsResponse;
import com.ieltspath.learning.application.usecase.AssignPlacementTestUseCase;
import com.ieltspath.learning.application.usecase.GetPlacementTestOptionsUseCase;
import com.ieltspath.learning.application.usecase.ReleasePlacementAssignmentUseCase;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.vo.PlacementPool;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning/placement-test")
@RequiredArgsConstructor
public class PlacementController {
    private final CurrentUserProvider currentUser;
    private final GetPlacementTestOptionsUseCase options;
    private final AssignPlacementTestUseCase assignments;
    private final ReleasePlacementAssignmentUseCase release;

    @GetMapping("/options")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PlacementTestOptionsResponse options() {
        return PlacementTestOptionsResponse.from(options.execute(currentUser.requireUserId()));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PlacementAssignmentResponse assign(@Valid @RequestBody PlacementAssignmentRequest request) {
        PlacementPool pool;
        try {
            pool = PlacementPool.valueOf(request.pool());
        } catch (RuntimeException exception) {
            throw new LearningRequestException(400, "INVALID_PLACEMENT_POOL", "Placement pool is invalid");
        }
        return PlacementAssignmentResponse.from(assignments.execute(currentUser.requireUserId(), pool));
    }

    @PostMapping("/assignments/{id}/release")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> release(@PathVariable UUID id) {
        release.execute(currentUser.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
