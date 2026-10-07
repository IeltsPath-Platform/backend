package com.ieltspath.access.api.controller;

import com.ieltspath.access.api.dto.request.RefundPointsRequest;
import com.ieltspath.access.api.dto.response.PointLedgerResponse;
import com.ieltspath.access.api.dto.response.UserEntitlementResponse;
import com.ieltspath.access.application.command.ConsumeHumanGradingCreditCommand;
import com.ieltspath.access.application.command.RefundPointsCommand;
import com.ieltspath.access.application.usecase.ConsumeHumanGradingCreditUseCase;
import com.ieltspath.access.application.usecase.GetUserEntitlementUseCase;
import com.ieltspath.access.application.usecase.RefundPointsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Operations on another user's access. They take a {@code userId} from the request, so only ADMIN may call them;
 * no service calls them yet. Point debit for a learner lives in {@link InternalPointsController}.
 */
@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InternalAccessController {

    private final GetUserEntitlementUseCase getUserEntitlementUseCase;
    private final RefundPointsUseCase refundPointsUseCase;
    private final ConsumeHumanGradingCreditUseCase consumeHumanGradingCreditUseCase;

    @GetMapping("/users/{userId}/entitlement")
    public UserEntitlementResponse getUserEntitlement(@PathVariable("userId") UUID userId) {
        return UserEntitlementResponse.from(getUserEntitlementUseCase.execute(userId));
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
