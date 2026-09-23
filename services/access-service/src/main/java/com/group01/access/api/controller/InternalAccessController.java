package com.group01.access.api.controller;

import com.group01.access.api.dto.request.DebitPointsRequest;
import com.group01.access.api.dto.request.RefundPointsRequest;
import com.group01.access.api.dto.response.PointLedgerResponse;
import com.group01.access.api.dto.response.UserEntitlementResponse;
import com.group01.access.application.command.ConsumeHumanGradingCreditCommand;
import com.group01.access.application.command.DebitPointsCommand;
import com.group01.access.application.command.RefundPointsCommand;
import com.group01.access.application.usecase.ConsumeHumanGradingCreditUseCase;
import com.group01.access.application.usecase.DebitPointsUseCase;
import com.group01.access.application.usecase.GetUserEntitlementUseCase;
import com.group01.access.application.usecase.RefundPointsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class InternalAccessController {

    private final GetUserEntitlementUseCase getUserEntitlementUseCase;
    private final DebitPointsUseCase debitPointsUseCase;
    private final RefundPointsUseCase refundPointsUseCase;
    private final ConsumeHumanGradingCreditUseCase consumeHumanGradingCreditUseCase;

    @GetMapping("/users/{userId}/entitlement")
    public UserEntitlementResponse getUserEntitlement(@PathVariable("userId") UUID userId) {
        return UserEntitlementResponse.from(getUserEntitlementUseCase.execute(userId));
    }

    @PostMapping("/points/debit")
    public PointLedgerResponse debitPoints(@Valid @RequestBody DebitPointsRequest request) {
        return PointLedgerResponse.from(debitPointsUseCase.execute(new DebitPointsCommand(
                request.userId(),
                request.amount(),
                request.referenceType(),
                request.referenceId(),
                request.idempotencyKey(),
                request.description()
        )));
    }

    @PostMapping("/points/refund")
    public PointLedgerResponse refundPoints(@Valid @RequestBody RefundPointsRequest request) {
        return PointLedgerResponse.from(refundPointsUseCase.execute(new RefundPointsCommand(
                request.userId(),
                request.amount(),
                request.referenceType(),
                request.referenceId(),
                request.idempotencyKey(),
                request.description()
        )));
    }

    @PostMapping("/users/{userId}/consume-human-grading")
    public Map<String, Object> consumeHumanGrading(@PathVariable("userId") UUID userId) {
        int remaining = consumeHumanGradingCreditUseCase.execute(new ConsumeHumanGradingCreditCommand(userId));
        return Map.of(
                "userId", userId,
                "remainingCredits", remaining
        );
    }
}
