package com.ieltspath.access.api.controller;

import com.ieltspath.access.api.dto.request.DebitPointsRequest;
import com.ieltspath.access.api.dto.response.PointLedgerResponse;
import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.usecase.DebitPointsUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service point operations; the Gateway denies {@code /internal/**} to clients. */
@RestController
@RequestMapping("/internal/access")
@RequiredArgsConstructor
public class InternalPointsController {

    private final CurrentUserProvider currentUserProvider;
    private final DebitPointsUseCase debitPointsUseCase;

    @PostMapping("/points/debit")
    public PointLedgerResponse debitPoints(@Valid @RequestBody DebitPointsRequest request) {
        return PointLedgerResponse.from(debitPointsUseCase.execute(new DebitPointsCommand(
                currentUserProvider.requireUserId(),
                request.userId(),
                request.amount(),
                request.referenceType(),
                request.referenceId(),
                request.idempotencyKey(),
                request.description()
        )));
    }
}
