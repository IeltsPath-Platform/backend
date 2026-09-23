package com.group01.access.api.controller;

import com.group01.access.api.dto.request.*;
import com.group01.access.api.dto.response.*;
import com.group01.access.application.command.*;
import com.group01.access.application.usecase.*;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/access/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccessController {

    private final CurrentUserProvider currentUserProvider;
    private final GenerateActivationKeysUseCase generateActivationKeysUseCase;
    private final RevokeActivationKeyUseCase revokeActivationKeyUseCase;
    private final CreatePlanUseCase createPlanUseCase;
    private final CreateKeyProductUseCase createKeyProductUseCase;
    private final AdjustPointsUseCase adjustPointsUseCase;
    private final GrantSubscriptionUseCase grantSubscriptionUseCase;

    @PostMapping("/keys/generate")
    public List<GeneratedKeyResponse> generateKeys(@Valid @RequestBody GenerateKeysRequest request) {
        UUID adminId = currentUserProvider.currentUser()
                .map(com.group01.commonsecurity.currentuser.CurrentUser::id)
                .orElse(null);
        return generateActivationKeysUseCase.execute(new GenerateActivationKeysCommand(
                request.productId(),
                request.count(),
                request.expiresAt(),
                adminId
        )).stream().map(GeneratedKeyResponse::from).toList();
    }

    @PostMapping("/keys/{id}/revoke")
    public ResponseEntity<Void> revokeKey(@PathVariable("id") UUID id) {
        revokeActivationKeyUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse createPlan(@Valid @RequestBody CreatePlanRequest request) {
        return PlanResponse.from(createPlanUseCase.execute(new CreatePlanCommand(request.code(), request.name())));
    }

    @PostMapping("/key-products")
    @ResponseStatus(HttpStatus.CREATED)
    public KeyProductResponse createKeyProduct(@Valid @RequestBody CreateKeyProductRequest request) {
        return KeyProductResponse.from(createKeyProductUseCase.execute(new CreateKeyProductCommand(
                request.code(),
                request.name(),
                request.keyType(),
                request.pointsAmount(),
                request.planId(),
                request.premiumDays(),
                request.humanGradingCredits()
        )));
    }

    @PostMapping("/points/adjust")
    public PointLedgerResponse adjustPoints(@Valid @RequestBody AdjustPointsRequest request) {
        return PointLedgerResponse.from(adjustPointsUseCase.execute(new AdjustPointsCommand(
                request.userId(),
                request.delta(),
                request.reason(),
                request.idempotencyKey()
        )));
    }

    @PostMapping("/subscriptions/grant")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse grantSubscription(@Valid @RequestBody GrantSubscriptionRequest request) {
        return SubscriptionResponse.from(grantSubscriptionUseCase.execute(new GrantSubscriptionCommand(
                request.userId(),
                request.planId(),
                request.durationDays(),
                request.humanGradingCredits()
        )));
    }
}
