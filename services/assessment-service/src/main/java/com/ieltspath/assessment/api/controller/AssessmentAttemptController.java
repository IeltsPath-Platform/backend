package com.ieltspath.assessment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.api.dto.request.SaveAttemptResponseRequest;
import com.ieltspath.assessment.api.dto.request.StartAssessmentAttemptRequest;
import com.ieltspath.assessment.api.dto.response.AssessmentAttemptResponse;
import com.ieltspath.assessment.api.dto.response.AttemptResponseResponse;
import com.ieltspath.assessment.api.dto.response.AttemptStructureResponse;
import com.ieltspath.assessment.application.command.SaveAttemptResponseCommand;
import com.ieltspath.assessment.application.command.StartAssessmentAttemptCommand;
import com.ieltspath.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.ieltspath.assessment.application.usecase.CompleteAttemptSectionUseCase;
import com.ieltspath.assessment.application.usecase.StartAttemptSectionUseCase;
import com.ieltspath.assessment.application.usecase.ExpireAssessmentAttemptUseCase;
import com.ieltspath.assessment.application.usecase.GetAssessmentAttemptUseCase;
import com.ieltspath.assessment.application.usecase.GetAttemptStructureUseCase;
import com.ieltspath.assessment.application.usecase.SaveAttemptResponseUseCase;
import com.ieltspath.assessment.application.usecase.ListAttemptResponsesUseCase;
import com.ieltspath.assessment.application.usecase.GetCurrentPlacementAttemptUseCase;
import com.ieltspath.assessment.application.usecase.StartAssessmentAttemptUseCase;
import com.ieltspath.assessment.application.usecase.SubmitAssessmentAttemptUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/assessments/attempts")
@RequiredArgsConstructor
public class AssessmentAttemptController {

    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;
    private final StartAssessmentAttemptUseCase startAssessmentAttemptUseCase;
    private final GetAssessmentAttemptUseCase getAssessmentAttemptUseCase;
    private final GetAttemptStructureUseCase getAttemptStructureUseCase;
    private final SubmitAssessmentAttemptUseCase submitAssessmentAttemptUseCase;
    private final ExpireAssessmentAttemptUseCase expireAssessmentAttemptUseCase;
    private final SaveAttemptResponseUseCase saveAttemptResponseUseCase;
    private final GetCurrentPlacementAttemptUseCase getCurrentPlacementAttemptUseCase;
    private final ListAttemptResponsesUseCase listAttemptResponsesUseCase;
    private final StartAttemptSectionUseCase startAttemptSectionUseCase;
    private final CompleteAttemptSectionUseCase completeAttemptSectionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentAttemptResponse start(
            @Valid @RequestBody StartAssessmentAttemptRequest request
    ) {
        var result = startAssessmentAttemptUseCase.execute(new StartAssessmentAttemptCommand(
                currentUser.requireUserId(),
                request.packageVersionId(),
                request.mode(),
                request.channel()
        ));
        return AssessmentAttemptResponse.from(result);
    }

    /** Latest placement attempt of the caller in any status; 204 when they never started one. */
    @GetMapping("/placement/current")
    public ResponseEntity<AssessmentAttemptResponse> currentPlacement() {
        return getCurrentPlacementAttemptUseCase.execute(currentUser.requireUserId())
                .map(result -> ResponseEntity.ok(AssessmentAttemptResponse.from(result)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public AssessmentAttemptResponse get(@PathVariable("id") UUID id) {
        return AssessmentAttemptResponse.from(getAssessmentAttemptUseCase.execute(currentUser.requireUserId(), id));
    }

    @GetMapping("/{id}/structure")
    public AttemptStructureResponse structure(@PathVariable("id") UUID id) {
        return AttemptStructureResponse.from(getAttemptStructureUseCase.execute(currentUser.requireUserId(), id), json);
    }

    @GetMapping("/{id}/responses")
    public List<AttemptResponseResponse> responses(@PathVariable("id") UUID id) {
        return listAttemptResponsesUseCase.execute(currentUser.requireUserId(), id).stream()
                .map(AttemptResponseResponse::from).toList();
    }

    /** Records when the learner first opens a section; repeating it is harmless. */
    @PostMapping("/{id}/sections/{sectionId}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startSection(@PathVariable("id") UUID id, @PathVariable("sectionId") UUID sectionId) {
        startAttemptSectionUseCase.execute(currentUser.requireUserId(), id, sectionId);
    }

    /** Finishes one section; its items take no more responses afterwards. Repeating it is harmless. */
    @PostMapping("/{id}/sections/{sectionId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeSection(@PathVariable("id") UUID id, @PathVariable("sectionId") UUID sectionId) {
        completeAttemptSectionUseCase.execute(currentUser.requireUserId(), id, sectionId);
    }

    @PostMapping("/{id}/submit")
    public AssessmentAttemptResponse submit(@PathVariable("id") UUID id) {
        return AssessmentAttemptResponse.from(
                submitAssessmentAttemptUseCase.execute(new SubmitAssessmentAttemptCommand(currentUser.requireUserId(), id))
        );
    }

    @PostMapping("/{id}/expire")
    public AssessmentAttemptResponse expire(@PathVariable("id") UUID id) {
        return AssessmentAttemptResponse.from(expireAssessmentAttemptUseCase.execute(currentUser.requireUserId(), id));
    }

    @PutMapping("/{id}/items/{itemId}/response")
    public AttemptResponseResponse saveResponse(
            @PathVariable("id") UUID id,
            @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody SaveAttemptResponseRequest request
    ) {
        var result = saveAttemptResponseUseCase.execute(new SaveAttemptResponseCommand(
                currentUser.requireUserId(),
                id,
                itemId,
                request.payload(),
                request.schemaVersion(),
                request.expectedRevision()
        ));
        return AttemptResponseResponse.from(result);
    }

}
