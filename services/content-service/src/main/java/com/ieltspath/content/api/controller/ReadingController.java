package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.response.ReadingPassageResponse;
import com.ieltspath.content.application.usecase.GetReadingPassageUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/reading")
public class ReadingController {

    private final GetReadingPassageUseCase getReadingPassageUseCase;
    private final CurrentUserProvider currentUser;

    public ReadingController(GetReadingPassageUseCase getReadingPassageUseCase, CurrentUserProvider currentUser) {
        this.getReadingPassageUseCase = getReadingPassageUseCase;
        this.currentUser = currentUser;
    }

    @GetMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public ReadingPassageResponse getReadingPassage(@PathVariable("sectionId") UUID sectionId) {
        return ReadingPassageResponse.from(getReadingPassageUseCase.execute(sectionId, isLearner()));
    }

    private boolean isLearner() {
        var roles = currentUser.requireRoles();
        return roles.contains("CUSTOMER") && !roles.contains("CONTENT_AUTHOR") && !roles.contains("EXAMINER");
    }
}
