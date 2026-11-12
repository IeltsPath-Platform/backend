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
        PromptActivationProvider.Activation activation = activations.activation(task);
        if (activation == null) return new ActivationResponse(task, null, null, null);
        return new ActivationResponse(task, activation.promptVersion(), activation.model(), activation.reasoningEffort());
    }

    /** {@code model} and {@code reasoningEffort} are what the activated run was graded with; Learning compares them with its own. */
    public record ActivationResponse(String task, String promptVersion, String model, String reasoningEffort) {}
}
