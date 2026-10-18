package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.GetProvisionalFeedbackUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assessments/attempts")
public class ProvisionalFeedbackController {
    private final CurrentUserProvider currentUser;
    private final GetProvisionalFeedbackUseCase feedback;
    @GetMapping("/{id}/provisional-feedback")
    public GetProvisionalFeedbackUseCase.Feedback get(@PathVariable("id") UUID id) {
        return feedback.execute(currentUser.requireUserId(),id);
    }
}
