package com.ieltspath.content.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.response.AiDraftResponse;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.usecase.RequestCourseDraftUseCase;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Asks the model to plan a course. The plan is then edited and approved through the course draft routes. */
@RestController
@RequestMapping("/api/content/admin/ai-drafts")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class AiCourseDraftController {

    private final RequestCourseDraftUseCase request;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;

    public AiCourseDraftController(RequestCourseDraftUseCase request, CurrentUserProvider currentUser,
                                   ObjectMapper json) {
        this.request = request;
        this.currentUser = currentUser;
        this.json = json;
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AiDraftResponse requestCourse(@Valid @RequestBody Body body) {
        CourseDraftRequest command = new CourseDraftRequest(body.bandLevel(), body.skills(), body.topics(),
                body.knowledgePointIds(), body.lessonCount(), body.instructions());
        return AiDraftResponse.from(request.execute(currentUser.requireUserId(), command), json);
    }

    public record Body(@NotNull BigDecimal bandLevel, List<Skill> skills, List<String> topics,
                       List<UUID> knowledgePointIds, Integer lessonCount, String instructions) {
    }
}
