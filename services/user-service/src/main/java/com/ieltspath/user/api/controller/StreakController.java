package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.UpsertStreakRequest;
import com.ieltspath.user.api.dto.response.StreakResponse;
import com.ieltspath.user.application.command.UpsertStreakCommand;
import com.ieltspath.user.application.usecase.GetStreakUseCase;
import com.ieltspath.user.application.usecase.UpsertStreakUseCase;
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
