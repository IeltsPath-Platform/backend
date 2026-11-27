package com.ieltspath.access.api.controller;

import com.ieltspath.access.api.dto.request.AdjustPointsRequest;
import com.ieltspath.access.api.dto.response.PointLedgerResponse;
import com.ieltspath.access.application.command.AdjustPointsCommand;
import com.ieltspath.access.application.usecase.AdjustPointsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccessController {

    private final AdjustPointsUseCase adjustPointsUseCase;

    @PostMapping("/points/adjust")
    public PointLedgerResponse adjustPoints(@Valid @RequestBody AdjustPointsRequest request) {
        return PointLedgerResponse.from(adjustPointsUseCase.execute(new AdjustPointsCommand(
                request.userId(),
                request.delta(),
                request.reason(),
                request.idempotencyKey()
        )));
    }
}
