package com.ieltspath.access.api.controller;

import com.ieltspath.access.application.usecase.IsUserPremiumUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/access/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
public class InternalEntitlementController {
    private final IsUserPremiumUseCase isUserPremiumUseCase;

    @GetMapping("/{userId}/premium")
    public Map<String, Boolean> premium(@PathVariable("userId") UUID userId) {
        return Map.of("premium", isUserPremiumUseCase.execute(userId));
    }
}
