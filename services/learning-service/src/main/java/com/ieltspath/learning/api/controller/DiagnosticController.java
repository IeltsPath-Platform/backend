package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.SubmitExerciseRequest;
import com.ieltspath.learning.api.dto.response.DiagnosticProbeResponse;
import com.ieltspath.learning.application.result.DiagnosticResult;
import com.ieltspath.learning.application.usecase.GetDiagnosticProbeUseCase;
import com.ieltspath.learning.application.usecase.GetDiagnosticUseCase;
import com.ieltspath.learning.application.usecase.SubmitDiagnosticProbeUseCase;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning/diagnostics")
@PreAuthorize("hasRole('CUSTOMER')")
public class DiagnosticController {
    private final CurrentUserProvider currentUser;
    private final GetDiagnosticUseCase diagnosis;
    private final GetDiagnosticProbeUseCase probe;
    private final SubmitDiagnosticProbeUseCase submit;

    public DiagnosticController(CurrentUserProvider currentUser, GetDiagnosticUseCase diagnosis,
                                GetDiagnosticProbeUseCase probe, SubmitDiagnosticProbeUseCase submit) {
        this.currentUser = currentUser;
        this.diagnosis = diagnosis;
        this.probe = probe;
        this.submit = submit;
    }

    @GetMapping("/{sessionId}")
    public DiagnosticResult diagnosis(@PathVariable("sessionId") UUID sessionId) {
        return diagnosis.execute(currentUser.requireUserId(), sessionId);
    }

    @GetMapping("/{sessionId}/probes/{probeId}")
    public DiagnosticProbeResponse probe(@PathVariable("sessionId") UUID sessionId,
                                         @PathVariable("probeId") UUID probeId) {
        return DiagnosticProbeResponse.from(probe.execute(currentUser.requireUserId(), sessionId, probeId));
    }

    @PostMapping("/{sessionId}/probes/{probeId}/submissions")
    public DiagnosticProbeResponse.Submission submit(@PathVariable("sessionId") UUID sessionId,
                                                     @PathVariable("probeId") UUID probeId,
                                                     @Valid @RequestBody SubmitExerciseRequest request) {
        return DiagnosticProbeResponse.Submission.from(submit.execute(currentUser.requireUserId(), sessionId,
                probeId, request.toCommand()));
    }
}
