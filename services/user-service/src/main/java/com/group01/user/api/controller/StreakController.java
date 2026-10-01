package com.group01.user.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.user.api.dto.request.UpsertStreakRequest;
import com.group01.user.api.dto.response.StreakResponse;
import com.group01.user.application.command.UpsertStreakCommand;
import com.group01.user.application.usecase.GetStreakUseCase;
import com.group01.user.application.usecase.UpsertStreakUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning-support/streak")
@RequiredArgsConstructor
public class StreakController {
    private final CurrentUserProvider currentUserProvider;
    private final GetStreakUseCase getStreakUseCase;
    private final UpsertStreakUseCase upsertStreakUseCase;

    @GetMapping
    public StreakResponse get() {
        return StreakResponse.from(getStreakUseCase.execute(currentUserProvider.requireUserId()));
    }

    @PutMapping
    public StreakResponse upsert(@Valid @RequestBody UpsertStreakRequest request) {
        return StreakResponse.from(upsertStreakUseCase.execute(new UpsertStreakCommand(
                currentUserProvider.requireUserId(),
                request.currentDays(),
                request.longestDays(),
                request.lastQualifiedDate(),
                request.timezone()
        )));
    }
}
