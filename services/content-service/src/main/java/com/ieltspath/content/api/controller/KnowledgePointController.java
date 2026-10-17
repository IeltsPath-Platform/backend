package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.request.CreateKnowledgePointRequest;
import com.ieltspath.content.api.dto.request.ReplaceKnowledgePointPrerequisitesRequest;
import com.ieltspath.content.api.dto.request.UpdateKnowledgePointRequest;
import com.ieltspath.content.api.dto.response.KnowledgePointResponse;
import com.ieltspath.content.application.command.CreateKnowledgePointCommand;
import com.ieltspath.content.application.command.UpdateKnowledgePointCommand;
import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.application.usecase.ActivateKnowledgePointUseCase;
import com.ieltspath.content.application.usecase.CreateKnowledgePointUseCase;
import com.ieltspath.content.application.usecase.DeactivateKnowledgePointUseCase;
import com.ieltspath.content.application.usecase.GetKnowledgePointPrerequisitesUseCase;
import com.ieltspath.content.application.usecase.GetKnowledgePointsUseCase;
import com.ieltspath.content.application.usecase.ReplaceKnowledgePointPrerequisitesUseCase;
import com.ieltspath.content.application.usecase.UpdateKnowledgePointUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/knowledge-points")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final GetKnowledgePointsUseCase getKnowledgePointsUseCase;
    private final CreateKnowledgePointUseCase createKnowledgePointUseCase;
    private final UpdateKnowledgePointUseCase updateKnowledgePointUseCase;
    private final DeactivateKnowledgePointUseCase deactivateKnowledgePointUseCase;
    private final ActivateKnowledgePointUseCase activateKnowledgePointUseCase;
    private final GetKnowledgePointPrerequisitesUseCase getKnowledgePointPrerequisitesUseCase;
    private final ReplaceKnowledgePointPrerequisitesUseCase replaceKnowledgePointPrerequisitesUseCase;

    @GetMapping
    public List<KnowledgePointResponse> getKnowledgePoints(
            @RequestParam(value = "topicId", required = false) UUID topicId
    ) {
        return getKnowledgePointsUseCase.execute(topicId).stream()
                .map(KnowledgePointResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public KnowledgePointResponse createKnowledgePoint(@Valid @RequestBody CreateKnowledgePointRequest request) {
        KnowledgePointResult result = createKnowledgePointUseCase.execute(new CreateKnowledgePointCommand(
                request.topicId(),
                request.code(),
                request.name(),
                request.kind(),
                request.learningType(),
                request.skill(),
                request.description(),
                request.cefrLevel(),
                request.canDo(),
                request.scopeIn(),
                request.scopeOut()
        ));
        return KnowledgePointResponse.from(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public KnowledgePointResponse updateKnowledgePoint(@PathVariable("id") UUID id,
                                                       @Valid @RequestBody UpdateKnowledgePointRequest request) {
        KnowledgePointResult result = updateKnowledgePointUseCase.execute(id, new UpdateKnowledgePointCommand(
                request.topicId(), request.code(), request.name(), request.kind(), request.learningType(),
                request.skill(), request.description(), request.cefrLevel(), request.canDo(), request.scopeIn(),
                request.scopeOut()));
        return KnowledgePointResponse.from(result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public void deactivateKnowledgePoint(@PathVariable("id") UUID id) {
        deactivateKnowledgePointUseCase.execute(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public KnowledgePointResponse activateKnowledgePoint(@PathVariable("id") UUID id) {
        return KnowledgePointResponse.from(activateKnowledgePointUseCase.execute(id));
    }

    @GetMapping("/{id}/prerequisites")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public List<KnowledgePointResponse> getPrerequisites(@PathVariable("id") UUID id) {
        return getKnowledgePointPrerequisitesUseCase.execute(id).stream().map(KnowledgePointResponse::from).toList();
    }

    @PutMapping("/{id}/prerequisites")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public List<KnowledgePointResponse> replacePrerequisites(
            @PathVariable("id") UUID id, @Valid @RequestBody ReplaceKnowledgePointPrerequisitesRequest request) {
        return replaceKnowledgePointPrerequisitesUseCase.execute(id, request.prerequisiteIds()).stream()
                .map(KnowledgePointResponse::from)
                .toList();
    }
}
