package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.PlacementTestResponse;
import com.ieltspath.learning.application.usecase.GetPlacementTestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PlacementController.PATH)
@RequiredArgsConstructor
public class PlacementController {
    public static final String PATH = "/api/learning/placement-test";

    private final CurrentUserProvider currentUser;
    private final GetPlacementTestUseCase getPlacementTest;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PlacementTestResponse placementTest() {
        return PlacementTestResponse.from(getPlacementTest.execute(currentUser.requireUserId()));
    }
}
