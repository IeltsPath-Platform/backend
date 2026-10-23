package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.response.AiDraftResponse;
import com.ieltspath.content.api.dto.response.CourseDraftAnalysisResponse;
import com.ieltspath.content.application.usecase.ApproveCourseDraftUseCase;
import com.ieltspath.content.application.usecase.CourseDraftUseCases;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Write, check and approve a course draft. Listing and rejecting use the AI draft routes. */
@RestController
@RequestMapping("/api/content/admin/course-drafts")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class CourseDraftController {

    private final CourseDraftUseCases drafts;
    private final ApproveCourseDraftUseCase approve;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;

    public CourseDraftController(CourseDraftUseCases drafts, ApproveCourseDraftUseCase approve,
                                 CurrentUserProvider currentUser, ObjectMapper json) {
        this.drafts = drafts;
        this.approve = approve;
        this.currentUser = currentUser;
        this.json = json;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiDraftResponse create(@RequestBody(required = false) StructureBody body) {
        JsonNode structure = body == null ? null : body.structure();
        return AiDraftResponse.from(drafts.create(currentUser.requireUserId(), structure), json);
    }

    @PutMapping("/{id}")
    public AiDraftResponse save(@PathVariable("id") UUID id, @RequestBody StructureBody body) {
        if (body == null || body.structure() == null) {
            throw new IllegalArgumentException("structure is required");
        }
        return AiDraftResponse.from(drafts.save(currentUser.requireUserId(), id, body.structure()), json);
    }

    @GetMapping("/{id}/analysis")
    public CourseDraftAnalysisResponse analysis(@PathVariable("id") UUID id) {
        return CourseDraftAnalysisResponse.from(drafts.analyze(currentUser.requireUserId(), id));
    }

    @PostMapping("/{id}/approve")
    public ApprovedResponse approve(@PathVariable("id") UUID id) {
        ApproveCourseDraftUseCase.Approved approved = approve.execute(currentUser.requireUserId(), id);
        return new ApprovedResponse(approved.draftId(), approved.courseId(), approved.topicIds(),
                approved.lessonIds(), approved.finalTestPackageIds());
    }

    /** {@code structure} is the plan as JSON; it is checked by the use case, not by bean validation. */
    public record StructureBody(@NotNull JsonNode structure) {
    }

    public record ApprovedResponse(UUID draftId, UUID courseId, List<UUID> topicIds, List<UUID> lessonIds,
                                   List<UUID> finalTestPackageIds) {
    }
}
