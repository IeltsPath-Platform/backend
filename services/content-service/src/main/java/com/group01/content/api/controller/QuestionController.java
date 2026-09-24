package com.group01.content.api.controller;

import com.group01.content.api.AccessLevelCompatibility;
import com.group01.content.api.dto.request.AddQuestionVersionRequest;
import com.group01.content.api.dto.request.CreateQuestionRequest;
import com.group01.content.api.dto.response.QuestionDetailResponse;
import com.group01.content.api.dto.response.QuestionResponse;
import com.group01.content.application.command.AddQuestionVersionCommand;
import com.group01.content.application.command.CreateQuestionCommand;
import com.group01.content.application.result.QuestionDetailResult;
import com.group01.content.application.result.QuestionResult;
import com.group01.content.application.usecase.*;
import com.group01.content.domain.vo.Skill;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public List<QuestionResponse> listQuestions(
            @RequestParam(value = "skill", required = false) Skill skill
    ) {
        return listQuestionsUseCase.execute(skill).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public QuestionDetailResponse getQuestionDetail(@PathVariable("id") UUID id) {
        QuestionDetailResult result = getQuestionDetailUseCase.execute(id);
        return QuestionDetailResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        QuestionResult result = createQuestionUseCase.execute(new CreateQuestionCommand(
                request.questionType(),
                request.skill(),
                AccessLevelCompatibility.toRequiredFeatureKey(request.accessLevel(), "PREMIUM_CONTENT")
        ));
        return QuestionResponse.from(result);
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
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
                request.difficulty(),
                request.knowledgePoints() == null ? null : request.knowledgePoints().stream()
                        .map(kp -> new AddQuestionVersionCommand.KnowledgePointInput(
                                kp.questionVersionId(), kp.knowledgePointId(), kp.weight()))
                        .toList()
        ));
        return QuestionResponse.from(result);
    }

    @PostMapping("/{id}/archive")
    public QuestionResponse archiveQuestion(@PathVariable("id") UUID id) {
        QuestionResult result = archiveQuestionUseCase.execute(id);
        return QuestionResponse.from(result);
    }
}
