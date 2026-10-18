package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.LessonResourcesResponse;
import com.ieltspath.learning.application.usecase.GetLessonResourcesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LessonResourceController {
    private final CurrentUserProvider currentUser;
    private final GetLessonResourcesUseCase getLessonResources;

    @GetMapping("/api/learning/lessons/{id}/resources")
    public LessonResourcesResponse resources(@PathVariable("id") UUID id) {
        return LessonResourcesResponse.from(getLessonResources.execute(currentUser.requireUserId(), id));
    }
}
