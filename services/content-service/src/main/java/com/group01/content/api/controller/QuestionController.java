package com.group01.content.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.group01.content.api.dto.request.AddQuestionVersionRequest;
import com.group01.content.api.dto.request.CreateQuestionRequest;
import com.group01.content.api.dto.response.QuestionDetailResponse;
import com.group01.content.api.dto.response.QuestionResponse;
import com.group01.content.application.command.AddQuestionVersionCommand;
import com.group01.content.application.command.CreateQuestionCommand;
import com.group01.content.application.result.QuestionDetailResult;
import com.group01.content.application.result.QuestionResult;
import com.group01.content.application.usecase.AddQuestionVersionUseCase;
import com.group01.content.application.usecase.ArchiveQuestionUseCase;
import com.group01.content.application.usecase.CreateQuestionUseCase;
import com.group01.content.application.usecase.GetQuestionDetailUseCase;
import com.group01.content.application.usecase.ListQuestionsUseCase;
import com.group01.content.domain.vo.Skill;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
                request.accessLevel()
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
                request.knowledgePoints()
        ));
        return QuestionResponse.from(result);
    }

    @PostMapping("/{id}/archive")
    public QuestionResponse archiveQuestion(@PathVariable("id") UUID id) {
        QuestionResult result = archiveQuestionUseCase.execute(id);
        return QuestionResponse.from(result);
    }
}
