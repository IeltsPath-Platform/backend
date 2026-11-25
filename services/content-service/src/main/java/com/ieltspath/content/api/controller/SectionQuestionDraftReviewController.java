package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.request.RejectAiDraftRequest;
import com.ieltspath.content.api.dto.request.SectionQuestionReviewRequests;
import com.ieltspath.content.api.dto.response.AiDraftResponse;
import com.ieltspath.content.application.usecase.SectionQuestionDraftReviewUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/admin/ai-drafts/section-questions/{draftId}")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class SectionQuestionDraftReviewController {
    private final SectionQuestionDraftReviewUseCase review;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;
    public SectionQuestionDraftReviewController(SectionQuestionDraftReviewUseCase review,
            CurrentUserProvider currentUser, ObjectMapper json) {
        this.review = review; this.currentUser = currentUser; this.json = json;
    }
    @GetMapping
    public Object get(@PathVariable UUID draftId) {
        var detail = review.detail(currentUser.requireUserId(), draftId);
        com.fasterxml.jackson.databind.node.ObjectNode response = json.valueToTree(AiDraftResponse.from(detail.draft(), json));
        response.put("sourceChanged", detail.sourceChanged());
        response.put("sectionState", detail.sectionState());
        return response;
    }
    @PatchMapping("/items/{itemId}")
    public AiDraftResponse edit(@PathVariable UUID draftId, @PathVariable UUID itemId,
                                @Valid @RequestBody SectionQuestionReviewRequests.Patch body) {
        return AiDraftResponse.from(review.editItem(currentUser.requireUserId(), draftId, itemId, body.revision(), json.valueToTree(body)), json);
    }
    @DeleteMapping("/items/{itemId}")
    public AiDraftResponse remove(@PathVariable UUID draftId, @PathVariable UUID itemId, @RequestParam int revision) {
        return AiDraftResponse.from(review.removeItem(currentUser.requireUserId(), draftId, itemId, revision), json);
    }
    @PostMapping("/approve")
    public AiDraftResponse approve(@PathVariable UUID draftId, @Valid @RequestBody SectionQuestionReviewRequests.Approve body) {
        return AiDraftResponse.from(review.approve(currentUser.requireUserId(), draftId, body.itemIds(), body.revision(), body.acceptSourceChange()), json);
    }
    @PostMapping("/reject")
    public AiDraftResponse reject(@PathVariable UUID draftId, @Valid @RequestBody RejectAiDraftRequest body) {
        return AiDraftResponse.from(review.reject(currentUser.requireUserId(), draftId, body.note()), json);
    }
}
