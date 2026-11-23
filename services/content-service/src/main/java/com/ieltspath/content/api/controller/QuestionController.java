package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.request.AddQuestionVersionRequest;
import com.ieltspath.content.api.dto.request.CreateQuestionRequest;
import com.ieltspath.content.api.dto.response.QuestionDetailResponse;
import com.ieltspath.content.api.dto.response.QuestionPageResponse;
import com.ieltspath.content.api.dto.response.QuestionPassageResponse;
import com.ieltspath.content.api.dto.response.QuestionResponse;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.result.QuestionDetailResult;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping({"/api/content/questions", "/api/content/admin/questions"})
@RequiredArgsConstructor
public class QuestionController {

    private final ListQuestionsUseCase listQuestionsUseCase;
    private final ListQuestionsWithIncludesUseCase listQuestionsWithIncludesUseCase;
    private final GetQuestionDetailUseCase getQuestionDetailUseCase;
    private final GetQuestionPassagesUseCase getQuestionPassagesUseCase;
    private final CreateQuestionUseCase createQuestionUseCase;
    private final AddQuestionVersionUseCase addQuestionVersionUseCase;
    private final ArchiveQuestionUseCase archiveQuestionUseCase;

    @GetMapping
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public QuestionPageResponse listQuestions(
            @RequestParam(value = "skill", required = false) Skill skill,
            @RequestParam(value = "purpose", required = false) QuestionPurpose purpose,
            @RequestParam(value = "status", required = false) PublicationStatus status,
            @RequestParam(value = "importRef", required = false) String importRef,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "include", required = false) String include,
            @RequestParam(value = "exceptBlockId", required = false) UUID exceptBlockId,
            @RequestParam(value = "onlyFree", defaultValue = "false") boolean onlyFree
    ) {
        if (importRef != null) {
            return QuestionPageResponse.from(listQuestionsUseCase.findByImportRef(importRef));
        }
        Set<String> includes = parseIncludes(include);
        if (!includes.isEmpty() || onlyFree) {
            return QuestionPageResponse.from(listQuestionsWithIncludesUseCase.execute(
                    skill, purpose, status, code, page, size, includes, onlyFree, exceptBlockId));
        }
        return QuestionPageResponse.from(listQuestionsUseCase.execute(skill, purpose, status, code, page, size));
    }

    private static Set<String> parseIncludes(String include) {
        if (include == null || include.isBlank()) return Set.of();
        Set<String> values = Arrays.stream(include.split(",", -1)).map(String::trim).collect(Collectors.toSet());
        if (values.stream().anyMatch(value -> !value.equals("ownership") && !value.equals("preview"))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ bổ sung trạng thái sử dụng hoặc bản xem trước câu hỏi.");
        }
        return Set.copyOf(values);
    }

    /** The reading passages that go with one version of a question; empty when it has none. */
    @GetMapping("/versions/{versionId}/passages")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public List<QuestionPassageResponse> getVersionPassages(@PathVariable("versionId") UUID versionId) {
        return getQuestionPassagesUseCase.execute(versionId).stream().map(QuestionPassageResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public QuestionDetailResponse getQuestionDetail(@PathVariable("id") UUID id) {
        QuestionDetailResult result = getQuestionDetailUseCase.execute(id);
        return QuestionDetailResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public QuestionResponse createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        QuestionResult result = createQuestionUseCase.execute(new CreateQuestionCommand(
                request.questionType(),
                request.skill(),
                AccessLevelCompatibility.toRequiredFeatureKey(request.accessLevel(), "PREMIUM_CONTENT"),
                request.purpose(),
                request.importRef()
        ));
        return QuestionResponse.from(result);
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
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
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public QuestionResponse archiveQuestion(@PathVariable("id") UUID id) {
        QuestionResult result = archiveQuestionUseCase.execute(id);
        return QuestionResponse.from(result);
    }
}
