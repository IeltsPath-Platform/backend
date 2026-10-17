package com.ieltspath.access.api.controller;

import com.ieltspath.access.api.dto.request.HumanGradingCreditRequest;
import com.ieltspath.access.api.dto.response.HumanGradingCreditResponse;
import com.ieltspath.access.application.command.ConsumeHumanGradingCreditCommand;
import com.ieltspath.access.application.command.RefundHumanGradingCreditCommand;
import com.ieltspath.access.application.usecase.ConsumeHumanGradingCreditUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/access/human-grading")
@PreAuthorize("hasRole('CUSTOMER')")
public class InternalHumanGradingController {
    private final CurrentUserProvider currentUser;
    private final ConsumeHumanGradingCreditUseCase credits;

    public InternalHumanGradingController(CurrentUserProvider currentUser, ConsumeHumanGradingCreditUseCase credits) {
        this.currentUser = currentUser;
        this.credits = credits;
    }

    @PostMapping("/consume")
    public HumanGradingCreditResponse consume(@Valid @RequestBody HumanGradingCreditRequest request) {
        int remaining = credits.execute(new ConsumeHumanGradingCreditCommand(
                currentUser.requireUserId(), request.submissionId()));
        return new HumanGradingCreditResponse(request.submissionId(), remaining);
    }

    @PostMapping("/refund")
    public HumanGradingCreditResponse refund(@Valid @RequestBody HumanGradingCreditRequest request) {
        int remaining = credits.refund(new RefundHumanGradingCreditCommand(
                currentUser.requireUserId(), request.submissionId()));
        return new HumanGradingCreditResponse(request.submissionId(), remaining);
    }
}
