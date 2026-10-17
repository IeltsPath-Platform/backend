package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CreateAiEvaluationRequest;
import com.ieltspath.assessment.api.dto.request.CreateAiGoldEssayRequest;
import com.ieltspath.assessment.application.service.AiEvaluationService;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AiEvaluationController {
    private final AiEvaluationService service;
    private final CurrentUserProvider currentUser;
    public AiEvaluationController(AiEvaluationService service, CurrentUserProvider currentUser) {
        this.service = service; this.currentUser = currentUser;
    }

    @PostMapping("/ai-gold-essays")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,UUID> addGold(@Valid @RequestBody CreateAiGoldEssayRequest request) {
        UUID id = service.addManual(request.task(), request.prompt(), request.chartFacts(), request.essay(),
                request.examinerBand(), request.examinerCriteria(), request.examinerErrorCodes(), currentUser.requireUserId());
        return Map.of("id", id);
    }

    @PostMapping("/ai-gold-essays/import-comparisons")
    public Map<String,Integer> importComparisons() {
        return Map.of("imported", service.importComparisons(currentUser.requireUserId()));
    }

    @PostMapping("/ai-evaluations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String,UUID> start(@Valid @RequestBody(required = false) CreateAiEvaluationRequest request) {
        return Map.of("id", service.start(request == null ? null : request.task()));
    }

    @GetMapping("/ai-evaluations/{id}")
    public Map<String,Object> get(@PathVariable("id") UUID id) { return service.getRun(id); }

    @PostMapping("/ai-prompt-activations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@RequestBody ActivationRequest request) {
        service.activate(request.runId(), currentUser.requireUserId());
    }

    public record ActivationRequest(UUID runId) {}
}
