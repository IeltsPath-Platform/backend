package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.usecase.RequestHumanGradingUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/attempts")
@PreAuthorize("hasRole('CUSTOMER')")
public class HumanGradingRequestController {
    private final CurrentUserProvider currentUser;
    private final RequestHumanGradingUseCase request;

    public HumanGradingRequestController(CurrentUserProvider currentUser, RequestHumanGradingUseCase request) {
        this.currentUser = currentUser;
        this.request = request;
    }

    @PostMapping("/{attemptId}/items/{itemId}/human-grading")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RequestHumanGradingUseCase.RequestResult request(@PathVariable("attemptId") UUID attemptId,
                                                             @PathVariable("itemId") UUID itemId) {
        return request.execute(currentUser.requireUserId(), attemptId, itemId);
    }
}
