package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.service.HeartsService;
import com.ieltspath.learning.application.usecase.HeartsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning/me/hearts")
@RequiredArgsConstructor
public class HeartsController {
    private final CurrentUserProvider currentUser;
    private final HeartsUseCase hearts;

    @GetMapping
    public HeartsService.Snapshot view() {
        return hearts.view(currentUser.requireUserId());
    }

    /** Refills the hearts for points. A repeat of the same {@code requestId} is charged once. */
    @PostMapping("/refill")
    public HeartsService.Snapshot refill(@Valid @RequestBody RefillRequest request) {
        return hearts.refill(currentUser.requireUserId(), request.requestId());
    }

    public record RefillRequest(@NotNull UUID requestId) {}
}
