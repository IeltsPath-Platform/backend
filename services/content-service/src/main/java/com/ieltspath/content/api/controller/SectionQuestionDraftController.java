package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.request.SectionQuestionDraftRequests;
import com.ieltspath.content.api.dto.response.AiDraftResponse;
import com.ieltspath.content.application.command.SectionQuestionDraftRequest;
import com.ieltspath.content.application.usecase.RequestSectionQuestionDraftUseCase;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/admin/ai-drafts/section-questions")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class SectionQuestionDraftController {
    private final RequestSectionQuestionDraftUseCase request;
    private final AiDraftRepository drafts;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;

    public SectionQuestionDraftController(RequestSectionQuestionDraftUseCase request, AiDraftRepository drafts,
            CurrentUserProvider currentUser, ObjectMapper json) {
        this.request = request; this.drafts = drafts; this.currentUser = currentUser; this.json = json;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AiDraftResponse create(@Valid @RequestBody SectionQuestionDraftRequests.Request body) {
        return AiDraftResponse.from(request.execute(currentUser.requireUserId(), new SectionQuestionDraftRequest(
                body.packageId(), body.versionId(), body.sectionId(), body.questionType(), body.count(), body.instructions())), json);
    }

    @GetMapping("/open")
    public List<AiDraftResponse> open(@RequestParam UUID versionId) {
        return drafts.findByRequester(currentUser.requireUserId(), DraftKind.SECTION_QUESTIONS, null, 0, 100).stream()
                .filter(draft -> draft.getStatus() == DraftStatus.QUEUED || draft.getStatus() == DraftStatus.GENERATING
                        || draft.getStatus() == DraftStatus.READY)
                .filter(draft -> versionId.toString().equals(read(draft).path("versionId").asText()))
                .map(draft -> AiDraftResponse.summary(draft, json)).toList();
    }

    private JsonNode read(com.ieltspath.content.domain.aggregate.AiDraft draft) {
        try { return json.readTree(draft.getRequestJson()); } catch (Exception invalid) { return json.createObjectNode(); }
    }
}
