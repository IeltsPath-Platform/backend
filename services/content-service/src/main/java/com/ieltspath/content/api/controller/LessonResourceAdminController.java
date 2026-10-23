package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.request.CreateLessonResourceRequest;
import com.ieltspath.content.api.dto.request.ReorderLessonResourcesRequest;
import com.ieltspath.content.api.dto.request.UpdateLessonResourceRequest;
import com.ieltspath.content.api.dto.request.UpdateLessonSummaryRequest;
import com.ieltspath.content.api.dto.response.LessonResourceResponse;
import com.ieltspath.content.api.dto.response.LessonResourcesResponse;
import com.ieltspath.content.application.command.CreateLessonResourceCommand;
import com.ieltspath.content.application.command.UpdateLessonResourceCommand;
import com.ieltspath.content.application.usecase.CreateLessonResourceUseCase;
import com.ieltspath.content.application.usecase.DeleteLessonResourceUseCase;
import com.ieltspath.content.application.usecase.ListLessonResourcesUseCase;
import com.ieltspath.content.application.usecase.ReorderLessonResourcesUseCase;
import com.ieltspath.content.application.usecase.UpdateLessonResourceUseCase;
import com.ieltspath.content.application.usecase.UpdateLessonSummaryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Authoring of a lesson's Markdown summary and its slide and document links. */
@RestController
@RequestMapping("/api/content/admin/lessons/{lessonId}")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
@RequiredArgsConstructor
public class LessonResourceAdminController {
    private final ListLessonResourcesUseCase listResources;
    private final CreateLessonResourceUseCase createResource;
    private final UpdateLessonResourceUseCase updateResource;
    private final DeleteLessonResourceUseCase deleteResource;
    private final UpdateLessonSummaryUseCase updateSummary;
    private final ReorderLessonResourcesUseCase reorderResources;

    @GetMapping("/resources")
    public LessonResourcesResponse list(@PathVariable("lessonId") UUID lessonId) {
        return LessonResourcesResponse.from(listResources.execute(lessonId));
    }

    @PutMapping("/summary")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSummary(@PathVariable("lessonId") UUID lessonId,
                              @Valid @RequestBody UpdateLessonSummaryRequest request) {
        updateSummary.execute(lessonId, request.markdown());
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResourceResponse create(@PathVariable("lessonId") UUID lessonId,
                                         @Valid @RequestBody CreateLessonResourceRequest request) {
        return LessonResourceResponse.from(createResource.execute(new CreateLessonResourceCommand(lessonId,
                request.type(), request.title(), request.url(), request.sortOrder())));
    }

    @PutMapping("/resources/{resourceId}")
    public LessonResourceResponse update(@PathVariable("lessonId") UUID lessonId,
                                         @PathVariable("resourceId") UUID resourceId,
                                         @Valid @RequestBody UpdateLessonResourceRequest request) {
        return LessonResourceResponse.from(updateResource.execute(new UpdateLessonResourceCommand(lessonId,
                resourceId, request.title(), request.url(), request.sortOrder())));
    }

    /** Reorders one type's resources in a single transaction, so no swap is left half done. */
    @PutMapping("/resources/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable("lessonId") UUID lessonId,
                        @Valid @RequestBody ReorderLessonResourcesRequest request) {
        reorderResources.execute(lessonId, request.type(), request.resourceIds());
    }

    @DeleteMapping("/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("lessonId") UUID lessonId, @PathVariable("resourceId") UUID resourceId) {
        deleteResource.execute(lessonId, resourceId);
    }
}
