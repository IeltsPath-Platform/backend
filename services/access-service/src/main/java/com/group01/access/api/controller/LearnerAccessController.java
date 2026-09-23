package com.group01.access.api.controller;

import com.group01.access.api.dto.request.ActivateKeyRequest;
import com.group01.access.api.dto.response.ActivationResponse;
import com.group01.access.api.dto.response.PointLedgerResponse;
import com.group01.access.api.dto.response.PointWalletResponse;
import com.group01.access.api.dto.response.SubscriptionResponse;
import com.group01.access.application.command.ActivateKeyCommand;
import com.group01.access.application.usecase.ActivateKeyUseCase;
import com.group01.access.application.usecase.GetPointHistoryUseCase;
import com.group01.access.application.usecase.GetUserPointWalletUseCase;
import com.group01.access.application.usecase.GetUserSubscriptionUseCase;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/access/me")
@RequiredArgsConstructor
public class LearnerAccessController {

    private final CurrentUserProvider currentUserProvider;
    private final GetUserSubscriptionUseCase getUserSubscriptionUseCase;
    private final GetUserPointWalletUseCase getUserPointWalletUseCase;
    private final GetPointHistoryUseCase getPointHistoryUseCase;
    private final ActivateKeyUseCase activateKeyUseCase;

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription() {
        UUID userId = currentUserProvider.requireUserId();
        return getUserSubscriptionUseCase.execute(userId)
                .map(SubscriptionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/points")
    public PointWalletResponse getMyPoints() {
        UUID userId = currentUserProvider.requireUserId();
        return PointWalletResponse.from(getUserPointWalletUseCase.execute(userId));
    }

    @GetMapping("/points/history")
    public List<PointLedgerResponse> getMyPointHistory(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        UUID userId = currentUserProvider.requireUserId();
        return getPointHistoryUseCase.execute(userId, offset, limit).stream()
                .map(PointLedgerResponse::from)
                .toList();
    }

    @PostMapping("/keys/activate")
    public ActivationResponse activateKey(@Valid @RequestBody ActivateKeyRequest request) {
        UUID userId = currentUserProvider.requireUserId();
        return ActivationResponse.from(activateKeyUseCase.execute(
                new ActivateKeyCommand(userId, request.rawKey(), request.idempotencyKey())
        ));
    }
}
