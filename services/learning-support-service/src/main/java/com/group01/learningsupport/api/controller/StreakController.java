package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learningsupport.api.dto.request.UpsertStreakRequest;
import com.group01.learningsupport.application.command.UpsertStreakCommand;
import com.group01.learningsupport.application.usecase.GetStreakUseCase;
import com.group01.learningsupport.application.usecase.UpsertStreakUseCase;
import com.group01.learningsupport.domain.aggregate.Streak;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning-support/streak")
@RequiredArgsConstructor
public class StreakController {
    private final CurrentUserProvider currentUserProvider;
    private final GetStreakUseCase getStreakUseCase;
    private final UpsertStreakUseCase upsertStreakUseCase;

    @GetMapping
    public Streak get() {
        return getStreakUseCase.execute(currentUserProvider.requireUserId());
    }

    @PutMapping
    public Streak upsert(@Valid @RequestBody UpsertStreakRequest request) {
        return upsertStreakUseCase.execute(new UpsertStreakCommand(
                currentUserProvider.requireUserId(),
                request.currentDays(),
                request.longestDays(),
                request.lastQualifiedDate(),
                request.timezone()
        ));
    }
}
