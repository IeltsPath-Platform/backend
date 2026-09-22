package com.group01.content.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
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
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.Skill;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<QuestionResponse> listQuestions(
            @RequestParam(value = "skill", required = false) Skill skill
    ) {
        List<QuestionResult> questions = listQuestionsUseCase.execute(skill);
        if (!isAuthorOrAdmin()) {
            questions = questions.stream()
                    .filter(q -> q.status() == PublicationStatus.PUBLISHED)
                    .toList();
        }
        return questions.stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public QuestionDetailResponse getQuestionDetail(@PathVariable("id") UUID id) {
        QuestionDetailResult result = getQuestionDetailUseCase.execute(id);
        if (result.status() != PublicationStatus.PUBLISHED && !isAuthorOrAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Draft questions are only accessible to authors and admins");
        }
        QuestionDetailResponse response = QuestionDetailResponse.from(result);
        if (!canViewAnswerSpec()) {
            response = response.withMaskedAnswerSpec();
        }
        return response;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
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
                request.difficulty(),
                request.knowledgePoints()
        ));
        return QuestionResponse.from(result);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public QuestionResponse archiveQuestion(@PathVariable("id") UUID id) {
        QuestionResult result = archiveQuestionUseCase.execute(id);
        return QuestionResponse.from(result);
    }

    private boolean canViewAnswerSpec() {
        return currentUserProvider.currentUser()
                .map(user -> user.roles().contains("ADMIN")
                        || user.roles().contains("CONTENT_AUTHOR")
                        || user.roles().contains("EXAMINER"))
                .orElse(false);
    }

    private boolean isAuthorOrAdmin() {
        return currentUserProvider.currentUser()
                .map(user -> user.roles().contains("ADMIN") || user.roles().contains("CONTENT_AUTHOR"))
                .orElse(false);
    }
}

