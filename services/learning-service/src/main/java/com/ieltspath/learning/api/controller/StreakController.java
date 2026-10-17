package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.StreakUseCase;
import com.ieltspath.learning.domain.vo.StreakView;
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
@RequestMapping("/api/learning/me/streak")
@RequiredArgsConstructor
public class StreakController {
    private final CurrentUserProvider currentUser;
    private final StreakUseCase streak;

    @GetMapping
    public StreakView view() {
        return streak.view(currentUser.requireUserId());
    }

    /** Buys a freeze for points. A repeat of the same {@code requestId} is charged once. */
    @PostMapping("/freeze")
    public StreakView buyFreeze(@Valid @RequestBody FreezeRequest request) {
        return streak.buyFreeze(currentUser.requireUserId(), request.requestId());
    }

    public record FreezeRequest(@NotNull UUID requestId) {}
}
