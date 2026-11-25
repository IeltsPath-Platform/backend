package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.request.QuestionDraftRequests;
import com.ieltspath.content.api.dto.response.AiDraftResponse;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.usecase.ApproveQuestionDraftUseCase;
import com.ieltspath.content.application.usecase.EditDraftItemUseCase;
import com.ieltspath.content.application.usecase.RequestQuestionDraftUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/** Asks for question drafts, edits or drops single items while the draft is ready, and approves chosen items. */
@RestController
@RequestMapping("/api/content/admin/ai-drafts")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class QuestionDraftController {

    private final RequestQuestionDraftUseCase request;
    private final ApproveQuestionDraftUseCase approve;
    private final EditDraftItemUseCase edit;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;

    public QuestionDraftController(RequestQuestionDraftUseCase request, ApproveQuestionDraftUseCase approve,
                                   EditDraftItemUseCase edit, CurrentUserProvider currentUser, ObjectMapper json) {
        this.request = request;
        this.approve = approve;
        this.edit = edit;
        this.currentUser = currentUser;
        this.json = json;
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AiDraftResponse requestQuestions(@Valid @RequestBody QuestionDraftRequests.Request body) {
        QuestionDraftRequest command = new QuestionDraftRequest(body.knowledgePointId(), body.questionType(),
                body.count(), body.difficulty(), body.instructions(), body.lessonId(), body.blockId());
        return AiDraftResponse.from(request.execute(currentUser.requireUserId(), command), json);
    }

    @GetMapping("/questions/open")
    public AiDraftResponse openForLesson(@RequestParam("lessonId") UUID lessonId) {
        return AiDraftResponse.from(request.findOpen(currentUser.requireUserId(), lessonId), json);
    }

    @PatchMapping("/{id}/items/{itemId}")
    public AiDraftResponse editItem(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId,
                                    @RequestParam("revision") int revision,
                                    @Valid @RequestBody QuestionDraftRequests.ItemPatch body) {
        return AiDraftResponse.from(edit.edit(currentUser.requireUserId(), id, itemId, revision, body.toCommand()), json);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public AiDraftResponse removeItem(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId,
                                      @RequestParam("revision") int revision) {
        return AiDraftResponse.from(edit.remove(currentUser.requireUserId(), id, itemId, revision), json);
    }

    @PostMapping("/{id}/approve")
    public AiDraftResponse approveItems(@PathVariable("id") UUID id,
                                        @Valid @RequestBody QuestionDraftRequests.Approve body) {
        return AiDraftResponse.from(approve.execute(currentUser.requireUserId(), id, body.itemIds(), body.revision(), body.createNewBlock()), json);
    }
}
