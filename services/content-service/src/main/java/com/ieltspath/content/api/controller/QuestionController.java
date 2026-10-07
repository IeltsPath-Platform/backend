package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.request.AddQuestionVersionRequest;
import com.ieltspath.content.api.dto.request.CreateQuestionRequest;
import com.ieltspath.content.api.dto.response.QuestionDetailResponse;
import com.ieltspath.content.api.dto.response.QuestionResponse;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.result.QuestionDetailResult;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/content/questions", "/api/content/admin/questions"})
@RequiredArgsConstructor
public class QuestionController {

    private final ListQuestionsUseCase listQuestionsUseCase;
    private final GetQuestionDetailUseCase getQuestionDetailUseCase;
    private final CreateQuestionUseCase createQuestionUseCase;
    private final AddQuestionVersionUseCase addQuestionVersionUseCase;
    private final ArchiveQuestionUseCase archiveQuestionUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public List<QuestionResponse> listQuestions(
            @RequestParam(value = "skill", required = false) Skill skill,
            @RequestParam(value = "purpose", required = false) QuestionPurpose purpose
    ) {
        return listQuestionsUseCase.execute(skill, purpose).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public QuestionDetailResponse getQuestionDetail(@PathVariable("id") UUID id) {
        QuestionDetailResult result = getQuestionDetailUseCase.execute(id);
        return QuestionDetailResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public QuestionResponse createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        QuestionResult result = createQuestionUseCase.execute(new CreateQuestionCommand(
                request.questionType(),
                request.skill(),
                AccessLevelCompatibility.toRequiredFeatureKey(request.accessLevel(), "PREMIUM_CONTENT"),
                request.purpose()
        ));
        return QuestionResponse.from(result);
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public QuestionResponse addVersion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddQuestionVersionRequest request
    ) {
        QuestionResult result = addQuestionVersionUseCase.execute(new AddQuestionVersionCommand(
                id,
                request.versionNumber(),
                request.stem(),
                request.options(),
                request.answerSpecJson(),
                request.explanation(),
                request.hint(),
                request.difficulty(),
                request.knowledgePoints() == null ? null : request.knowledgePoints().stream()
                        .map(kp -> new AddQuestionVersionCommand.KnowledgePointInput(
                                kp.questionVersionId(), kp.knowledgePointId(), kp.weight()))
                        .toList()
        ));
        return QuestionResponse.from(result);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public QuestionResponse archiveQuestion(@PathVariable("id") UUID id) {
        QuestionResult result = archiveQuestionUseCase.execute(id);
        return QuestionResponse.from(result);
    }
}
