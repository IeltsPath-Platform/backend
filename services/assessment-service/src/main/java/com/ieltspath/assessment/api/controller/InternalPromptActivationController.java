package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.PromptActivationProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Set;

/** Authenticated internal lookup; a null version means this task has never been activated. */
@RestController
public class InternalPromptActivationController {
    private final PromptActivationProvider activations;

    public InternalPromptActivationController(PromptActivationProvider activations) {
        this.activations = activations;
    }

    @GetMapping("/internal/assessments/ai-prompt-activations")
    public ActivationResponse get(@RequestParam("task") String task) {
        if (!Set.of("TASK_1", "TASK_2").contains(task)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_WRITING_TASK");
        }
        return new ActivationResponse(task, activations.activatedVersion(task));
    }

    public record ActivationResponse(String task, String promptVersion) {}
}
