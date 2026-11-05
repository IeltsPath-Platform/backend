package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.response.AiDraftResponse;
import com.ieltspath.content.application.command.PracticeSetDraftRequest;
import com.ieltspath.content.application.usecase.RequestPracticeSetDraftUseCase;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Asks the model to write one practice set for a lesson that has none. The result is a draft to read, then to put into
 * the course draft; nothing is created until that course draft is approved.
 */
@RestController
@RequestMapping("/api/content/admin/ai-drafts")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class AiPracticeSetDraftController {

    private final RequestPracticeSetDraftUseCase request;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;

    public AiPracticeSetDraftController(RequestPracticeSetDraftUseCase request, CurrentUserProvider currentUser,
                                        ObjectMapper json) {
        this.request = request;
        this.currentUser = currentUser;
        this.json = json;
    }

    @PostMapping("/practice-sets")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AiDraftResponse requestPracticeSet(@Valid @RequestBody Body body) {
        PracticeSetDraftRequest command = new PracticeSetDraftRequest(body.knowledgePointIds(), body.lessonTitle(),
                body.vocabulary(), body.questionType(), body.count(), body.difficulty(), body.instructions());
        return AiDraftResponse.from(request.execute(currentUser.requireUserId(), command), json);
    }

    public record Body(@NotEmpty List<UUID> knowledgePointIds, String lessonTitle, List<String> vocabulary,
                       @NotNull QuestionType questionType, @NotNull Integer count, @NotNull QuestionDifficulty difficulty,
                       String instructions) {
    }
}
