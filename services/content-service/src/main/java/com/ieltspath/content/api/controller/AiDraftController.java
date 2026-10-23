package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.request.RejectAiDraftRequest;
import com.ieltspath.content.api.dto.response.AiDraftResponse;
import com.ieltspath.content.application.usecase.AiDraftReviewUseCases;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** An author sees only the drafts they requested. Generating and approving are added by the kind-specific routes. */
@RestController
@RequestMapping("/api/content/admin/ai-drafts")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class AiDraftController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AiDraftReviewUseCases drafts;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;

    public AiDraftController(AiDraftReviewUseCases drafts, CurrentUserProvider currentUser, ObjectMapper json) {
        this.drafts = drafts;
        this.currentUser = currentUser;
        this.json = json;
    }

    @GetMapping
    public List<AiDraftResponse> list(@RequestParam(value = "kind", required = false) DraftKind kind,
                                      @RequestParam(value = "status", required = false) DraftStatus status,
                                      @RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return drafts.list(currentUser.requireUserId(), kind, status, safePage, safeSize).stream()
                .map(draft -> AiDraftResponse.from(draft, json))
                .toList();
    }

    @GetMapping("/{id}")
    public AiDraftResponse get(@PathVariable("id") UUID id) {
        return AiDraftResponse.from(drafts.get(currentUser.requireUserId(), id), json);
    }

    @PostMapping("/{id}/reject")
    public AiDraftResponse reject(@PathVariable("id") UUID id, @Valid @RequestBody RejectAiDraftRequest request) {
        return AiDraftResponse.from(drafts.reject(currentUser.requireUserId(), id, request.note()), json);
    }
}
