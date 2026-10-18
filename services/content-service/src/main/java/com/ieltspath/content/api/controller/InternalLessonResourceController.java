package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.response.LessonResourcesResponse;
import com.ieltspath.content.application.usecase.ListLessonResourcesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-facing read of a published lesson's summary and resources; the learning service applies the lesson gate
 * before passing it on. The gateway denies {@code /internal/**} to clients.
 */
@RestController
@RequiredArgsConstructor
public class InternalLessonResourceController {
    private final ListLessonResourcesUseCase listResources;

    @GetMapping("/internal/learning-content/lessons/{id}/resources")
    public LessonResourcesResponse resources(@PathVariable("id") UUID lessonId) {
        return LessonResourcesResponse.from(listResources.executePublished(lessonId));
    }
}
