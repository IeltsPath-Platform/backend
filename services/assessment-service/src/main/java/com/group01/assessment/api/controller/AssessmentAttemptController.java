package com.group01.assessment.api.controller;

import com.group01.assessment.api.dto.request.SaveAttemptResponseRequest;
import com.group01.assessment.api.dto.request.StartAssessmentAttemptRequest;
import com.group01.assessment.api.dto.response.AssessmentAttemptResponse;
import com.group01.assessment.api.dto.response.AttemptResponseResponse;
import com.group01.assessment.api.dto.response.AttemptStructureResponse;
import com.group01.assessment.application.command.SaveAttemptResponseCommand;
import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.group01.assessment.application.usecase.ExpireAssessmentAttemptUseCase;
import com.group01.assessment.application.usecase.GetAssessmentAttemptUseCase;
import com.group01.assessment.application.usecase.GetAttemptStructureUseCase;
import com.group01.assessment.application.usecase.SaveAttemptResponseUseCase;
import com.group01.assessment.application.usecase.StartAssessmentAttemptUseCase;
import com.group01.assessment.application.usecase.SubmitAssessmentAttemptUseCase;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/attempts")
@RequiredArgsConstructor
public class AssessmentAttemptController {

    private final CurrentUserProvider currentUser;
    private final StartAssessmentAttemptUseCase startAssessmentAttemptUseCase;
    private final GetAssessmentAttemptUseCase getAssessmentAttemptUseCase;
    private final GetAttemptStructureUseCase getAttemptStructureUseCase;
    private final SubmitAssessmentAttemptUseCase submitAssessmentAttemptUseCase;
    private final ExpireAssessmentAttemptUseCase expireAssessmentAttemptUseCase;
    private final SaveAttemptResponseUseCase saveAttemptResponseUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentAttemptResponse start(
            @Valid @RequestBody StartAssessmentAttemptRequest request
    ) {
        var sections = request.sections().stream()
                .map(section -> new StartAssessmentAttemptCommand.SectionInput(
                        section.contentSectionId(),
                        section.sortOrder(),
                        section.snapshot(),
                        section.items().stream()
                                .map(item -> new StartAssessmentAttemptCommand.ItemInput(
                                        item.questionVersionId(),
                                        item.sortOrder(),
                                        item.questionSnapshot(),
                                        item.answerSnapshot(),
                                        item.knowledgeSnapshot()
                                ))
                                .toList()
                ))
                .toList();

        var result = startAssessmentAttemptUseCase.execute(new StartAssessmentAttemptCommand(
                currentUser.requireUserId(),
                request.packageVersionId(),
                request.attemptType(),
                request.mode(),
                request.channel(),
                request.expiresAt(),
                sections
        ));
        return AssessmentAttemptResponse.from(result);
    }

    @GetMapping("/{id}")
    public AssessmentAttemptResponse get(@PathVariable("id") UUID id) {
        return AssessmentAttemptResponse.from(getAssessmentAttemptUseCase.execute(currentUser.requireUserId(), id));
    }

    @GetMapping("/{id}/structure")
    public AttemptStructureResponse structure(@PathVariable("id") UUID id) {
        return AttemptStructureResponse.from(getAttemptStructureUseCase.execute(currentUser.requireUserId(), id));
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
