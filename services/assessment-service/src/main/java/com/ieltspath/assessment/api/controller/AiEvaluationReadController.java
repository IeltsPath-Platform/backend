package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.service.AiEvaluationReadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lists behind the AI evaluation screens. Running and activating evaluations stays in {@link AiEvaluationController}. */
@RestController
@RequestMapping("/api/assessments/admin")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class AiEvaluationReadController {
    private final AiEvaluationReadService reads;

    public AiEvaluationReadController(AiEvaluationReadService reads) {
        this.reads = reads;
    }

    @GetMapping("/ai-gold-essays")
    public AiEvaluationReadService.Page<AiEvaluationReadService.GoldEssayRow> goldEssays(
            @RequestParam(name = "task", required = false) String task,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return reads.goldEssays(task, page, size);
    }

    @GetMapping("/ai-evaluations")
    public AiEvaluationReadService.Page<AiEvaluationReadService.EvaluationRow> evaluations(
            @RequestParam(name = "task", required = false) String task,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return reads.evaluations(task, page, size);
    }

    @GetMapping("/ai-prompt-activations")
    public List<AiEvaluationReadService.ActivationRow> activations() {
        return reads.activations();
    }
}
